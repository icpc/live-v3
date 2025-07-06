package org.icpclive.converter.export.clics

import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.html.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import org.icpclive.cds.*
import org.icpclive.cds.adapters.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.onIdle
import org.icpclive.clics.*
import org.icpclive.clics.events.*
import org.icpclive.clics.v202306.events.*
import org.icpclive.clics.v202306.events.AwardEvent
import org.icpclive.clics.v202306.events.CommentaryEvent
import org.icpclive.clics.v202306.events.ContestEvent
import org.icpclive.clics.v202306.events.GroupEvent
import org.icpclive.clics.v202306.events.JudgementEvent
import org.icpclive.clics.v202306.events.JudgementTypeEvent
import org.icpclive.clics.v202306.events.LanguageEvent
import org.icpclive.clics.v202306.events.OrganizationEvent
import org.icpclive.clics.v202306.events.ProblemEvent
import org.icpclive.clics.v202306.events.StateEvent
import org.icpclive.clics.v202306.events.SubmissionEvent
import org.icpclive.clics.v202306.events.TeamEvent
import org.icpclive.clics.v202306.objects.*
import org.icpclive.clics.v202306.objects.Award
import org.icpclive.clics.v202306.objects.ScoreboardRow
import org.icpclive.converter.export.Exporter
import org.icpclive.converter.export.Router
import java.nio.ByteBuffer
import kotlin.time.Duration.Companion.minutes

typealias EventProducer = (EventToken) -> Event

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

private fun LanguageInfo.toClicsLang() = Language(
    id = id.value,
    name = name,
    extensions = extensions
)


private fun MediaType.toClicsMedia() = when (this) {
    is MediaType.Object -> null
    is MediaType.Image -> File("image", Url(url))
    is MediaType.Video -> File("video", Url(url))
    is MediaType.Audio -> File("audio", Url(url))
    is MediaType.M2tsVideo -> File("video/m2ts", Url(url))
    is MediaType.HLSVideo -> File("application/vnd.apple.mpegurl", Url(url))
    is MediaType.WebRTCGrabberConnection -> null
    is MediaType.WebRTCProxyConnection -> null
}

private fun TeamInfo.toClicsTeam() = Team(
    id = id.value,
    name = fullName,
    displayName = displayName,
    hidden = isHidden,
    groupIds = groups.map { it.value },
    organizationId = organizationId?.value,
    photo = listOfNotNull(medias[TeamMediaType.PHOTO]?.toClicsMedia()),
    video = listOfNotNull(medias[TeamMediaType.RECORD]?.toClicsMedia()),
    desktop = listOfNotNull(medias[TeamMediaType.SCREEN]?.toClicsMedia()),
    webcam = listOfNotNull(medias[TeamMediaType.CAMERA]?.toClicsMedia()),
)


object ClicsExporter : Exporter {

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

    private val unknownLanguage = LanguageInfo(
        id = "unknown".toLanguageId(),
        name = "unknown",
        extensions = emptyList()
    )

    private suspend fun <ID, T> FlowCollector<EventProducer>.updateEvent(id: ID, data: T, block : (ID, EventToken, T?) -> Event) = emit {
        block(id, it, data)
    }

    private suspend fun <T> FlowCollector<EventProducer>.updateEvent(data: T, block : (EventToken, T?) -> Event) = emit { block(it, data) }

    private fun getContest(info: ContestInfo) = Contest(
        id = "contest",
        name = info.name,
        formalName = info.name,
        startTime = info.startTime,
        countdownPauseTime = (info.status as? ContestStatus.BEFORE)?.holdTime,
        duration = info.contestLength,
        scoreboardFreezeDuration = info.freezeTime?.let { info.contestLength - it },
        scoreboardType = when (info.resultType) {
            ContestResultType.ICPC -> "pass-fail"
            ContestResultType.IOI -> "score"
        },
        penaltyTime = info.penaltyPerWrongAttempt
    )

    private suspend fun <ID, T, CT> FlowCollector<EventProducer>.diffChange(
        old: MutableMap<ID, T>,
        new: List<T>,
        id: T.() -> ID,
        convert: T.() -> CT,
        toFinalEvent: (ID, EventToken, CT?) -> Event
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
        toFinalEvent: (ID, EventToken, Nothing?) -> Event
    ) {
        val values = new.map { it.id() }.toSet()
        val toRemove = buildList {
            for (k in old.keys) {
                if (k !in values) {
                    updateEvent(k, null, toFinalEvent)
                    add(k)
                }
            }
        }
        for (i in toRemove) {
            old.remove(i)
        }
    }

    private suspend fun <ID, T, CT> FlowCollector<EventProducer>.diff(
        old: MutableMap<ID, T>,
        new: List<T>,
        id: T.() -> ID,
        convert: T.() -> CT,
        toFinalEvent: (ID, EventToken, CT?) -> Event
    ) {
        diffChange(old, new, id, convert, toFinalEvent)
        diffRemove(old, new, id, toFinalEvent)
    }

    private fun getState(info: ContestInfo) = when (val status = info.status) {
        is ContestStatus.BEFORE -> State(
            ended = null,
            frozen = null,
            started = null,
            finalized = null,
            thawed = null,
            endOfUpdates = null
        )

        is ContestStatus.RUNNING -> State(
            ended = null,
            frozen = status.frozenAt,
            started = status.startedAt,
            finalized = null,
            thawed = null,
            endOfUpdates = null
        )

        is ContestStatus.OVER -> State(
            ended = status.finishedAt,
            frozen = status.frozenAt,
            started = status.startedAt,
            finalized = null,
            thawed = null,
            endOfUpdates = null
        )
        is ContestStatus.FINALIZED -> State(
            ended = status.finishedAt,
            frozen = status.frozenAt,
            started = status.startedAt,
            finalized = status.finalizedAt,
            thawed = null,
            endOfUpdates = status.finalizedAt
        )
    }

    private val submissions = mutableMapOf<RunId, Pair<Submission, Judgement>>()

    private suspend fun FlowCollector<EventProducer>.processRun(info: ContestInfo, run: RunInfo) {
        val submission = Submission(
            id = run.id.toString(),
            languageId = (run.languageId ?: unknownLanguage.id).value,
            problemId = run.problemId.value,
            teamId = run.teamId.value,
            time = info.startTimeOrZero + run.time,
            contestTime = run.time,
        )
        val judgement = when (val result = run.result) {
            is RunResult.ICPC -> Judgement(
                id = run.id.toString(),
                submissionId = run.id.toString(),
                judgementTypeId = judgmentTypes[result.verdict]?.id,
                startTime = info.startTimeOrZero + run.time,
                startContestTime = run.time,
                endTime = info.startTimeOrZero + run.time,
                endContestTime = run.time
            )

            is RunResult.IOI -> Judgement(
                id = run.id.toString(),
                submissionId = run.id.toString(),
                judgementTypeId = judgmentTypes[result.wrongVerdict ?: Verdict.Accepted]?.id,
                score = when (info.problems[run.problemId]?.scoreMergeMode) {
                    ScoreMergeMode.MAX_PER_GROUP, ScoreMergeMode.SUM, null -> result.scoreAfter
                    ScoreMergeMode.MAX_TOTAL, ScoreMergeMode.LAST, ScoreMergeMode.LAST_OK -> result.score.sum()
                },
                startTime = info.startTimeOrZero + run.time,
                startContestTime = run.time,
                endTime = info.startTimeOrZero + run.time,
                endContestTime = run.time
            )
            is RunResult.InProgress -> Judgement(
                id = run.id.toString(),
                submissionId = run.id.toString(),
                startTime = info.startTimeOrZero + run.time,
                startContestTime = run.time,
            )
        }
        val (curSubmission, curJudgment) = submissions[run.id] ?: (null to null)
        if (submission != curSubmission) {
            updateEvent(
                run.id.toString(),
                submission,
                ::SubmissionEvent
            )
        }
        if (judgement != curJudgment) {
            updateEvent(
                run.id.toString(),
                judgement,
                ::JudgementEvent
            )
        }
    }

    private suspend fun <T> FlowCollector<EventProducer>.diff(oldInfo: ContestInfo?, newInfo: ContestInfo, getter: ContestInfo.() -> T, event : (EventToken, T?) -> Event) {
        val old = oldInfo?.getter()
        val new = newInfo.getter()
        if (old != new) {
            updateEvent(new, event)
        }
    }

    private val groupsMap = mutableMapOf<GroupId, GroupInfo>()
    private val orgsMap = mutableMapOf<OrganizationId, OrganizationInfo>()
    private val languagesMap = mutableMapOf<LanguageId, LanguageInfo>()
    private val problemsMap = mutableMapOf<ProblemId, ProblemInfo>()
    private val teamsMap = mutableMapOf<TeamId, TeamInfo>()
    private val awardsMap = mutableMapOf<String, Award>()

    @OptIn(InefficientContestInfoApi::class)
    private suspend fun FlowCollector<EventProducer>.calculateDiff(oldInfo: ContestInfo?, newInfo: ContestInfo) {
        diff(oldInfo, newInfo, ClicsExporter::getContest, ::ContestEvent)
        diff(oldInfo, newInfo, ClicsExporter::getState, ::StateEvent)
        if (oldInfo == null) {
            for (type in judgmentTypes.values) {
                updateEvent(type.id, type, ::JudgementTypeEvent)
            }
        }
        diff(problemsMap, newInfo.problemList, { id }, { toClicsProblem() }) { id, token, data -> ProblemEvent(id.value, token, data) }
        diffChange(groupsMap, newInfo.groupList, { id }, { toClicsGroup() }) { id, token, data -> GroupEvent(id.value, token, data) }
        diffChange(orgsMap, newInfo.organizationList, { id }, { toClicsOrg() }) { id, token, data -> OrganizationEvent(id.value, token, data) }
        diffChange(languagesMap, newInfo.languagesList + unknownLanguage, { id }, { toClicsLang() }) { id, token, data -> LanguageEvent(id.value, token, data) }

        diff(teamsMap, newInfo.teamList, { id }, TeamInfo::toClicsTeam) { id, token, data -> TeamEvent(id.value, token, data) }

        diffRemove(groupsMap, newInfo.groupList, { id }) { id, token, data -> GroupEvent(id.value, token, data) }
        diffRemove(orgsMap, newInfo.organizationList, { id }) { id, token, data -> OrganizationEvent(id.value, token, data) }
        diffRemove(languagesMap, newInfo.languagesList + unknownLanguage, { id }) { id, token, data -> LanguageEvent(id.value, token, data) }
    }

    private suspend fun FlowCollector<EventProducer>.processCommentaryMessage(event: CommentaryMessage) {
        updateEvent(
            event.id.toString(),
            Commentary(
                id = event.id.toString(),
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

    private val currentState = MutableStateFlow<ContestStateWithScoreboard?>(null)

    private fun generateEventFeed(updates: Flow<ContestStateWithScoreboard>) : Flow<Event> {
        var eventCounter = 1
        return updates.transform { state ->
            currentState.value = state
            when (val event = state.state.lastEvent) {
                is InfoUpdate -> calculateDiff(state.state.infoBeforeEvent, event.newInfo)
                is RunUpdate -> processRun(state.state.infoBeforeEvent!!, event.newInfo)
                is CommentaryMessagesUpdate -> processCommentaryMessage(event.message)
            }
            diff(
                awardsMap,
                state.toClicsAwards(),
                Award::id,
                { this },
                ::AwardEvent
            )
        }.map { it(EventToken("live-cds-${eventCounter++}")) }
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


    override fun CoroutineScope.runOn(contestUpdates: Flow<ContestStateWithScoreboard>): Router {
        val eventFeed = generateEventFeed(contestUpdates)
            .shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
            .transformWhile {
                emit(it)
                it !is StateEvent || it.data?.endOfUpdates == null
            }
        val contestFlow = eventFeed.filterGlobalEvent<Contest, _, ContestEvent>(this)
        val stateFlow = eventFeed.filterGlobalEvent<State, _, StateEvent>(this)

        val judgementTypesFlow = eventFeed.filterIdEvent<JudgementType, _, JudgementTypeEvent>(this)
        val languagesFlow = eventFeed.filterIdEvent<Language, _, LanguageEvent>(this)
        val problemsFlow = eventFeed.filterIdEvent<Problem, _, ProblemEvent>(this)
        val groupsFlow = eventFeed.filterIdEvent<Group, _, GroupEvent>(this)
        val organizationsFlow = eventFeed.filterIdEvent<Organization, _, OrganizationEvent>(this)
        val teamsFlow = eventFeed.filterIdEvent<Team, _, TeamEvent>(this)
        val submissionsFlow = eventFeed.filterIdEvent<Submission, _, SubmissionEvent>(this)
        val judgementsFlow = eventFeed.filterIdEvent<Judgement, _, JudgementEvent>(this)
        //val runsFlow = eventFeed.filterIdEvent<Run, Event.RunsEvent>(scope)
        val commentaryFlow = eventFeed.filterIdEvent<Commentary, _, CommentaryEvent>(this)
        //val personsFlow = eventFeed.filterIdEvent<Person, Event.PersonEvent>(scope)
        //val accountsFlow = eventFeed.filterIdEvent<Account, Event.AccountEvent>(scope)
        //val clarificationsFlow = eventFeed.filterIdEvent<Clarification, Event.ClarificationEvent>(scope)
        val awardsFlow = eventFeed.filterIdEvent<Award, _, AwardEvent>(this)

        val json = Json {
            encodeDefaults = true
            prettyPrint = false
            explicitNulls = false
            serializersModule = clicsEventsSerializersModule(FeedVersion.`2023_06`, tokenPrefix = "")
        }
        return object : Router {
            override fun HtmlBlockTag.mainPage() {
                a("/clics/api") { +"Clics api root" }
                br
                a("/clics/api/contests/contest") { +"Clics contest api root" }
                br
                a("/clics/api/contests/contest/event-feed") { +"Clics event feed" }
            }
            override fun Route.setUpRoutes() {
                route("/clics/api") {
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
                            get("/scoreboard") { call.respond(currentState.filterNotNull().first().toClicsScoreboard()) }
                            get("/event-feed") {
                                call.respondBytesWriter(contentType = ContentType("application", "x-ndjson")) {
                                    eventFeed
                                        .map { json.encodeToString(it) }
                                        .onIdle(1.minutes) { channel.send("") }
                                        .collect {
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
        }
    }

    private fun ContestStateWithScoreboard.toClicsScoreboard(): Scoreboard {
        val info = state.infoAfterEvent!!
        return Scoreboard(
            time = info.startTimeOrZero + lastSubmissionTime,
            contestTime = lastSubmissionTime,
            state = getState(info),
            rows = rankingAfter.order.zip(rankingAfter.ranks).map { (teamId, rank) ->
                val row = scoreboardRowAfter(teamId)
                ScoreboardRow(
                    rank,
                    teamId.value,
                    ScoreboardRowScore(row.totalScore.toInt(), row.penalty),
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
                    .filter {
                        when (val result = it.second.problemResults[index]) {
                            is ICPCProblemResult -> result.isFirstToSolve
                            is IOIProblemResult -> result.isFirstBest
                        }
                    }
                    .map { it.first.value }
            ))
        }
    }
}
