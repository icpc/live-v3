package org.icpclive.export.clics

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import org.icpclive.cds.*
import org.icpclive.cds.adapters.*
import org.icpclive.cds.api.*
import org.icpclive.clics.*
import org.icpclive.clics.v202306.objects.*
import org.icpclive.clics.v202306.events.*
import org.icpclive.clics.events.Event
import org.icpclive.clics.events.GlobalEvent
import org.icpclive.clics.events.IdEvent
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.cds.util.loopFlow
import org.icpclive.clics.v202306.objects.ScoreboardRowProblem
import org.icpclive.clics.v202306.objects.Award
import org.icpclive.clics.v202306.objects.ScoreboardRow
import java.nio.ByteBuffer
import kotlin.time.Duration.Companion.minutes

typealias EventProducer = (String) -> Event

private fun ProblemInfo.toClicsProblem() = Problem(
    id = id.value,
    ordinal = ordinal,
    label = displayName,
    name = fullName,
    rgb = color?.value,
    testDataCount = 1,
)

private fun GroupInfo.toClicsGroup() = Group(
    id = id.value,
    name = displayName,
)

private fun OrganizationInfo.toClicsOrg() = Organization(
    id = id.value,
    name = displayName,
    formalName = fullName,
    logo = listOfNotNull(logo?.toClicsMedia())
)

private fun MediaType.toClicsMedia() = when (this) {
    is MediaType.Object -> null
    is MediaType.Image -> File("image", Url(url))
    is MediaType.TaskStatus -> null
    is MediaType.TimeLine -> null
    is MediaType.Video -> File("video", Url(url))
    is MediaType.M2tsVideo -> File("video/m2ts", Url(url))
    is MediaType.HLSVideo -> File("application/vnd.apple.mpegurl", Url(url))
    is MediaType.WebRTCGrabberConnection -> null
    is MediaType.WebRTCProxyConnection -> null
}

private fun TeamInfo.toClicsTeam() = Team(
    id = id.value,
    name = fullName,
    hidden = isHidden,
    groupIds = groups.map { it.value },
    organizationId = organizationId?.value,
    photo = listOfNotNull(medias[TeamMediaType.PHOTO]?.toClicsMedia()),
    video = listOfNotNull(medias[TeamMediaType.RECORD]?.toClicsMedia()),
    desktop = listOfNotNull(medias[TeamMediaType.SCREEN]?.toClicsMedia()),
    webcam = listOfNotNull(medias[TeamMediaType.CAMERA]?.toClicsMedia()),
)


object ClicsExporter  {

    private fun Verdict.toJudgmentType() = when (this) {
        Verdict.Accepted -> JudgementType("AC", "correct", solved = true, penalty = false)

        Verdict.Fail -> JudgementType("JE", "judging error", solved = false, penalty = false)
        Verdict.Ignored -> JudgementType("IG", "ignored", solved = false, penalty = false)
        Verdict.CompilationError -> JudgementType("CE", "compiler error", solved = false, penalty = false)

        Verdict.Challenged -> JudgementType("CH", "challenged", solved = false, penalty = true)

        Verdict.CompilationErrorWithPenalty -> JudgementType("CEP", "compiler error penalised", solved = false, penalty = true)
        Verdict.IdlenessLimitExceeded -> JudgementType( "IL", "idleness limit", solved = false, penalty = true)
        Verdict.MemoryLimitExceeded -> JudgementType("MLE", "memory limit", solved = false, penalty = true)
        Verdict.OutputLimitExceeded -> JudgementType("OLE", "output limit", solved = false, penalty = true)
        Verdict.PresentationError -> JudgementType("PE", "presentation error", solved = false, penalty = true)
        Verdict.Rejected -> JudgementType("RE", "rejected", solved = false, penalty = true)
        Verdict.RuntimeError -> JudgementType("RTE", "run-time error", solved = false, penalty = true)
        Verdict.SecurityViolation -> JudgementType( "SV", "security violation", solved = false, penalty = true)
        Verdict.TimeLimitExceeded -> JudgementType("TLE", "time limit", solved = false, penalty = true)
        Verdict.WrongAnswer -> JudgementType("WA", "wrong answer", solved = false, penalty = true)
    }

    private val judgmentTypes = Verdict.all.associateWith {
        it.toJudgmentType()
    }

    private val unknownLanguage = Language(
        "unknown",
    )

    private val languages = listOf(unknownLanguage)

    private suspend fun <ID, T> FlowCollector<EventProducer>.updateEvent(id: ID, data: T, block : (ID, String, T?) -> Event) = emit {
        block(id, it, data)
    }

    private suspend fun <T> FlowCollector<EventProducer>.updateEvent(data: T, block : (String, T?) -> Event) = emit { block(it, data) }

    private fun getContest(info: ContestInfo) = Contest(
        id = "contest",
        name = info.name,
        formalName = info.name,
        startTime = info.startTime.takeIf { it != Instant.fromEpochSeconds(0) },
        countdownPauseTime = info.holdBeforeStartTime,
        duration = info.contestLength,
        scoreboardFreezeDuration = info.contestLength - info.freezeTime,
        scoreboardType = "pass-fail",
        penaltyTime = info.penaltyPerWrongAttempt.inWholeMinutes,
    )

    private suspend fun <ID, T, CT> FlowCollector<EventProducer>.diffChange(
        old: MutableMap<ID, T>,
        new: List<T>,
        id: T.() -> ID,
        convert: T.() -> CT,
        toFinalEvent: (ID, String, CT?) -> Event
    ) {
        for (n in new) {
            if (old[n.id()] != n) {
                updateEvent(n.id(), n.convert(), toFinalEvent)
                old[n.id()] = n
            }
        }
    }

    private suspend fun <ID, T> FlowCollector<EventProducer>.diffRemove(
        old: MutableMap<ID, T>,
        new: List<T>,
        id: T.() -> ID,
        toFinalEvent: Function3<ID, String, Nothing?, Event>
    ) {
        val values = new.map { it.id() }.toSet()
        for (k in old.keys) {
            if (k !in values) {
                updateEvent(k, null, toFinalEvent)
            }
        }
    }

    private suspend fun <ID, T, CT> FlowCollector<EventProducer>.diff(
        old: MutableMap<ID, T>,
        new: List<T>,
        id: T.() -> ID,
        convert: T.() -> CT,
        toFinalEvent: (ID, String, CT?) -> Event
    ) {
        diffChange(old, new, id, convert, toFinalEvent)
        diffRemove(old, new, id, toFinalEvent)
    }

    private fun getState(info: ContestInfo) = when (info.status) {
        ContestStatus.BEFORE -> State(
            ended = null,
            frozen = null,
            started = null,
            finalized = null,
            thawed = null,
            endOfUpdates = null
        )

        ContestStatus.RUNNING, ContestStatus.FAKE_RUNNING -> State(
            ended = null,
            frozen = if (info.currentContestTime >= info.freezeTime) info.startTime + info.freezeTime else null,
            started = info.startTime,
            finalized = null,
            thawed = null,
            endOfUpdates = null
        )

        ContestStatus.OVER -> State(
            ended = info.startTime + info.contestLength,
            frozen = info.startTime + info.freezeTime,
            started = info.startTime,
            finalized = null,
            thawed = null,
            endOfUpdates = null
        )
        ContestStatus.FINALIZED -> State(
            ended = info.startTime + info.contestLength,
            frozen = info.startTime + info.freezeTime,
            started = info.startTime,
            finalized = info.startTime + info.contestLength,
            thawed = info.startTime + info.contestLength,
            endOfUpdates = info.startTime + info.contestLength
        )
    }

    private val submissionsCreated = mutableSetOf<RunId>()

    private suspend fun FlowCollector<EventProducer>.processRun(info: ContestInfo, run: RunInfo) {
        if (run.id !in submissionsCreated) {
            submissionsCreated.add(run.id)
            updateEvent(
                run.id.toString(),
                Submission(
                    id = run.id.toString(),
                    languageId = unknownLanguage.id,
                    problemId = run.problemId.value,
                    teamId = run.teamId.value,
                    time = info.startTime + run.time,
                    contestTime = run.time,
                ),
                ::SubmissionEvent
            )
        }
        val result = run.result as? RunResult.ICPC ?: return
        updateEvent(
            run.id.toString(),
            Judgement(
                id = run.id.toString(),
                submissionId = run.id.toString(),
                judgementTypeId = judgmentTypes[result.verdict]?.id,
                startTime = info.startTime + run.time,
                startContestTime = run.time,
                endTime = info.startTime + run.time,
                endContestTime = run.time
            ),
            ::JudgementEvent
        )
    }

    private suspend fun <T> FlowCollector<EventProducer>.diff(oldInfo: ContestInfo?, newInfo: ContestInfo, getter: ContestInfo.() -> T, event : (String, T?) -> Event) {
        val old = oldInfo?.getter()
        val new = newInfo.getter()
        if (old != new) {
            updateEvent(new, event)
        }
    }

    private val groupsMap = mutableMapOf<GroupId, GroupInfo>()
    private val orgsMap = mutableMapOf<OrganizationId, OrganizationInfo>()
    private val problemsMap = mutableMapOf<ProblemId, ProblemInfo>()
    private val teamsMap = mutableMapOf<TeamId, TeamInfo>()

    @OptIn(InefficientContestInfoApi::class)
    private suspend fun FlowCollector<EventProducer>.calculateDiff(oldInfo: ContestInfo?, newInfo: ContestInfo) {
        diff(oldInfo, newInfo, ClicsExporter::getContest, ::ContestEvent)
        diff(oldInfo, newInfo, ClicsExporter::getState, ::StateEvent)
        if (oldInfo == null) {
            for (type in judgmentTypes.values) {
                updateEvent(type.id, type, ::JudgementTypeEvent)
            }
            for (language in languages) {
                updateEvent(language.id, language, ::LanguageEvent)
            }
        }
        diff(problemsMap, newInfo.problemList, { id }, { toClicsProblem() }) { id, token, data -> ProblemEvent(id.value, token, data) }
        diffChange(groupsMap, newInfo.groupList, { id }, { toClicsGroup() }) { id, token, data -> GroupEvent(id.value, token, data) }
        diffChange(orgsMap, newInfo.organizationList, { id }, { toClicsOrg() }) { id, token, data -> OrganizationEvent(id.value, token, data) }

        diff(teamsMap, newInfo.teamList, { id }, TeamInfo::toClicsTeam) { id, token, data -> TeamEvent(id.value, token, data) }

        diffRemove(groupsMap, newInfo.groupList, { id }) { id, token, data -> GroupEvent(id.value, token, data) }
        diffRemove(orgsMap, newInfo.organizationList, { id }) { id, token, data -> OrganizationEvent(id.value, token, data) }
    }

    private suspend fun FlowCollector<EventProducer>.processAnalytics(message: AnalyticsMessage) {
        val event = message as? AnalyticsCommentaryEvent ?: return
        updateEvent(
            event.id,
            Commentary(
                id = event.id,
                time = event.time,
                contestTime = event.relativeTime,
                message = event.message,
                tags = event.tags,
                teamIds = event.teamIds.map { it.value },
                problemIds = event.runIds.map { it.toString() },
                submissionIds = emptyList(),
            ),
            ::CommentaryEvent
        )
    }


    private fun generateEventFeed(updates: Flow<ContestUpdate>) : Flow<Event> {
        var eventCounter = 1
        return updates.contestState().transform {state ->
            when (val event = state.lastEvent) {
                is InfoUpdate -> calculateDiff(state.infoBeforeEvent, event.newInfo)
                is RunUpdate -> processRun(state.infoBeforeEvent!!, event.newInfo)
                is AnalyticsUpdate -> processAnalytics(event.message)
            }
        }.map { it("live-cds-${eventCounter++}") }
    }

    private inline fun <reified X, Y, reified T: GlobalEvent<Y>> Flow<Event>.filterGlobalEvent(scope: CoroutineScope) = filterIsInstance<T>().map {
        it.data as X
    }.stateIn(scope, SharingStarted.Eagerly, null)
        .filterNotNull()

    private inline fun <reified X : Y, Y, reified T: IdEvent<Y>> Flow<Event>.filterIdEvent(scope: CoroutineScope) = filterIsInstance<T>()
        .runningFold(persistentMapOf<String, X>()) { accumulator, value ->
            if (value.data == null) {
                accumulator.remove(value.id)
            } else {
                accumulator.put(value.id, value.data as X)
            }
    }.stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    @Serializable
    data class Error(val code: Int, val message: String)

    private val endpoint = mutableMapOf<String, SerialDescriptor>()

    private inline fun <reified T> Route.getId(prefix: String, flow: Flow<Map<String, T>>) {
        endpoint[prefix] = serializer<T>().descriptor
        route("/$prefix") {
            get { call.respond(flow.first().entries.sortedBy { it.key }.map { it.value }) }
            get("/{id}") {
                val id = call.parameters["id"]
                val f = flow.first()
                if (id in f) {
                    call.respond(f[id]!!)
                } else {
                    call.respond(
                        Error(404,"Object with ID '$id' not found")
                    )
                }
            }
        }
    }

    private inline fun <reified T: Any> Route.getGlobal(prefix: String, flow: Flow<T>) {
        endpoint[prefix] = serializer<T>().descriptor
        route("/$prefix") {
            get { call.respond(flow.first()) }
        }
    }

    private val awardsMap = mutableMapOf<String, Award>()
    private var awardEventId = 0

    private fun generateAwardEvents(awards: Flow<List<Award>>) = awards.transform {
        diff(
            awardsMap,
            it,
            Award::id,
            { this },
            ::AwardEvent
        )
    }.map { it("live-cds-award-${awardEventId++}") }


    fun Route.setUp(scope: CoroutineScope, updates: Flow<ContestUpdate>) {
        val scoreboardFlow = updates
            .addFirstToSolves()
            .calculateScoreboard(OptimismLevel.NORMAL)
            .stateIn(scope, SharingStarted.Eagerly, null)
            .filterNotNull()

        val eventFeed =
            merge(
                generateEventFeed(updates),
                generateAwardEvents(scoreboardFlow.map { it.toClicsAwards() }.distinctUntilChanged())
            ).shareIn(scope, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
        val contestFlow = eventFeed.filterGlobalEvent<Contest, _, ContestEvent>(scope)
        val stateFlow = eventFeed.filterGlobalEvent<State, _, StateEvent>(scope)

        val judgementTypesFlow = eventFeed.filterIdEvent<JudgementType, _, JudgementTypeEvent>(scope)
        val languagesFlow = eventFeed.filterIdEvent<Language, _, LanguageEvent>(scope)
        val problemsFlow = eventFeed.filterIdEvent<Problem, _, ProblemEvent>(scope)
        val groupsFlow = eventFeed.filterIdEvent<Group, _, GroupEvent>(scope)
        val organizationsFlow = eventFeed.filterIdEvent<Organization, _, OrganizationEvent>(scope)
        val teamsFlow = eventFeed.filterIdEvent<Team, _, TeamEvent>(scope)
        val submissionsFlow = eventFeed.filterIdEvent<Submission, _, SubmissionEvent>(scope)
        val judgementsFlow = eventFeed.filterIdEvent<Judgement, _, JudgementEvent>(scope)
        //val runsFlow = eventFeed.filterIdEvent<Run, Event.RunsEvent>(scope)
        val commentaryFlow = eventFeed.filterIdEvent<Commentary, _, CommentaryEvent>(scope)
        //val personsFlow = eventFeed.filterIdEvent<Person, Event.PersonEvent>(scope)
        //val accountsFlow = eventFeed.filterIdEvent<Account, Event.AccountEvent>(scope)
        //val clarificationsFlow = eventFeed.filterIdEvent<Clarification, Event.ClarificationEvent>(scope)
        val awardsFlow = eventFeed.filterIdEvent<Award, _, AwardEvent>(scope)

        val json = Json {
            encodeDefaults = true
            prettyPrint = false
            explicitNulls = false
            serializersModule = clicsEventsSerializersModule(FeedVersion.`2023_06`)
            classDiscriminatorMode = ClassDiscriminatorMode.NONE
        }
        route("/api") {
            install(ContentNegotiation) { json(json) }
            get {
                call.respond(
                    ApiInformation(
                        version = FeedVersion.`2023_06`.name,
                        versionUrl = FeedVersion.`2023_06`.url,
                        provider = ApiInformationProvider(
                            name = "icpc live"
                        )
                    )
                )
            }
            route("/contests") {
                get { call.respond(listOf(contestFlow.first())) }
                getGlobal("contest", contestFlow)
                route("contest") {
                    getGlobal("state", stateFlow)
                    getId("judgement-types", judgementTypesFlow)
                    getId("languages", languagesFlow)
                    getId("problems", problemsFlow)
                    getId("groups", groupsFlow)
                    getId("organizations", organizationsFlow)
                    getId("teams", teamsFlow)
                    getId("submissions", submissionsFlow)
                    getId("judgements", judgementsFlow)
                    //getId("runs", runsFlow)
                    getId("commentary", commentaryFlow)
                    //getId("persons", personsFlow)
                    //getId("accounts", accountsFlow)
                    //getId("clarifications", clarificationsFlow)
                    getId("awards", awardsFlow)
                    get("/scoreboard") { call.respond(scoreboardFlow.first().toClicsScoreboard()) }
                    get("/event-feed") {
                        call.respondBytesWriter {
                            merge(
                                eventFeed.map { json.encodeToString(it) },
                                loopFlow(2.minutes, onError = {}) { "" }
                            ).collect {
                                writeFully(ByteBuffer.wrap("$it\n".toByteArray()))
                                flush()
                            }
                        }
                    }
                    get("/access") {
                        call.respond(
                            Access(
                                emptyList(),
                                endpoint.entries.map { (k, v) ->
                                    Endpoint(
                                        k,
                                        v.elementNames.toList()
                                    )
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun ContestStateWithScoreboard.toClicsScoreboard(): Scoreboard {
        val info = state.infoAfterEvent!!
        return Scoreboard(
            time = info.startTime + lastSubmissionTime,
            contestTime = lastSubmissionTime,
            state = getState(info),
            rows = rankingAfter.order.zip(rankingAfter.ranks).map { (teamId, rank) ->
                val row = scoreboardRowAfter(teamId)
                ScoreboardRow(
                    rank,
                    teamId.value,
                    ScoreboardRowScore(row.totalScore.toInt(), row.penalty.inWholeMinutes),
                    row.problemResults.mapIndexed { index, v ->
                        val iv = v as ICPCProblemResult
                        ScoreboardRowProblem(
                            info.scoreboardProblems[index].id.value,
                            iv.wrongAttempts + (if (iv.isSolved) 1 else 0),
                            iv.pendingAttempts,
                            iv.isSolved,
                            iv.lastSubmitTime?.inWholeMinutes.takeIf { iv.isSolved }
                        )
                    }
                )
            }
        )
    }

    private fun ContestStateWithScoreboard.toClicsAwards() = buildList {
        val info = state.infoAfterEvent!!
        for (award in rankingAfter.awards) {
            add(Award(award.id, award.citation, award.teams.map { it.value }))
        }
        for ((index, problem) in info.scoreboardProblems.withIndex()) {
            add(Award(
                "first-to-solve-${problem.id}",
                "First to solve problem ${problem.displayName}",
                rankingAfter.order.map { it to scoreboardRowAfter(it) }
                    .filter { (it.second.problemResults[index] as? ICPCProblemResult)?.isFirstToSolve == true }
                    .map { it.first.value }
            ))
        }
    }
}
