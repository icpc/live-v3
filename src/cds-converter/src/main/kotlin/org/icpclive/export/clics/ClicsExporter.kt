import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import org.icpclive.cds.*
import org.icpclive.cds.adapters.*
import org.icpclive.cds.api.*
import org.icpclive.clics.*
import org.icpclive.clics.v202207.*
import org.icpclive.clics.v202207.Award
import org.icpclive.clics.v202207.Scoreboard
import org.icpclive.clics.v202207.ScoreboardRow
import org.icpclive.cds.scoreboard.ScoreboardAndContestInfo
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.util.*
import java.nio.ByteBuffer
import kotlin.time.Duration.Companion.minutes

typealias EventProducer = (String) -> Event

private fun ProblemInfo.toClicsProblem() = Problem(
    id = id.value,
    ordinal = ordinal,
    label = displayName,
    name = fullName,
    rgb = color,
    test_data_count = 1,
)

private fun GroupInfo.toClicsGroup() = Group(
    id = id.value,
    name = displayName,
)

private fun OrganizationInfo.toClicsOrg() = Organization(
    id = id.value,
    name = displayName,
    formal_name = fullName,
    logo = listOfNotNull(logo?.toClicsMedia())
)

private fun MediaType.toClicsMedia() = when (this) {
    is MediaType.Object -> null
    is MediaType.Image -> Media("image", url)
    is MediaType.TaskStatus -> null
    is MediaType.Video -> Media("video", url)
    is MediaType.M2tsVideo -> Media("video/m2ts", url)
    is MediaType.HLSVideo -> Media("application/vnd.apple.mpegurl", url)
    is MediaType.WebRTCGrabberConnection -> null
    is MediaType.WebRTCProxyConnection -> null
}

private fun TeamInfo.toClicsTeam() = Team(
    id = id.value,
    name = fullName,
    hidden = isHidden,
    group_ids = groups.map { it.value },
    organization_id = organizationId?.value,
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
        "unknown",
        false,
        emptyList()
    )

    private val languages = listOf(unknownLanguage)

    private suspend fun <ID, T> FlowCollector<EventProducer>.updateEvent(id: ID, data: T, block : (ID, String, T?) -> Event) = emit {
        block(id, it, data)
    }

    private suspend fun <T> FlowCollector<EventProducer>.updateEvent(data: T, block : (String, T?) -> Event) = emit { block(it, data) }

    private fun getContest(info: ContestInfo) = Contest(
        id = "contest",
        name = info.name,
        formal_name = info.name,
        start_time = info.startTime.takeIf { it != Instant.fromEpochSeconds(0) },
        countdown_pause_time = info.holdBeforeStartTime,
        duration = info.contestLength,
        scoreboard_freeze_duration = info.contestLength - info.freezeTime,
        scoreboard_type = "pass-fail",
        penalty_time = info.penaltyPerWrongAttempt,
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
            unfrozen = null,
            finalized = null,
            thawed = null,
            end_of_updates = null
        )

        ContestStatus.RUNNING, ContestStatus.FAKE_RUNNING -> State(
            ended = null,
            frozen = if (info.currentContestTime >= info.freezeTime) info.startTime + info.freezeTime else null,
            started = info.startTime,
            unfrozen = null,
            finalized = null,
            thawed = null,
            end_of_updates = null
        )

        ContestStatus.OVER -> State(
            ended = info.startTime + info.contestLength,
            frozen = info.startTime + info.freezeTime,
            started = info.startTime,
            unfrozen = null,
            finalized = null,
            thawed = null,
            end_of_updates = null
        )
        ContestStatus.FINALIZED -> State(
            ended = info.startTime + info.contestLength,
            frozen = null,
            started = info.startTime,
            unfrozen = null,
            finalized = info.startTime + info.contestLength,
            thawed = null,
            end_of_updates = info.startTime + info.contestLength
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
                    language_id = unknownLanguage.id,
                    problem_id = run.problemId.value,
                    team_id = run.teamId.value,
                    time = info.startTime + run.time,
                    contest_time = run.time,
                ),
                Event::SubmissionEvent
            )
        }
        val result = run.result as? RunResult.ICPC ?: return
        updateEvent(
            run.id.toString(),
            Judgement(
                id = run.id.toString(),
                submission_id = run.id.toString(),
                judgement_type_id = judgmentTypes[result.verdict]?.id,
                start_time = info.startTime + run.time,
                start_contest_time = run.time,
                end_time = info.startTime + run.time,
                end_contest_time = run.time
            ),
            Event::JudgementEvent
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
        diff(oldInfo, newInfo, ::getContest, Event::ContestEvent)
        diff(oldInfo, newInfo, ::getState, Event::StateEvent)
        if (oldInfo == null) {
            for (type in judgmentTypes.values) {
                updateEvent(type.id, type, Event::JudgementTypeEvent)
            }
            for (language in languages) {
                updateEvent(language.id, language, Event::LanguageEvent)
            }
        }
        diff(problemsMap, newInfo.problemList, { id }, { toClicsProblem() }) { id, token, data -> Event.ProblemEvent(id.value, token, data) }
        diffChange(groupsMap, newInfo.groupList, { id }, { toClicsGroup() }) { id, token, data -> Event.GroupsEvent(id.value, token, data) }
        diffChange(orgsMap, newInfo.organizationList, { id }, { toClicsOrg() }) { id, token, data -> Event.OrganizationEvent(id.value, token, data) }

        diff(teamsMap, newInfo.teamList, { id }, TeamInfo::toClicsTeam) { id, token, data -> Event.TeamEvent(id.value, token, data) }

        diffRemove(groupsMap, newInfo.groupList, { id }) { id, token, data -> Event.GroupsEvent(id.value, token, data) }
        diffRemove(orgsMap, newInfo.organizationList, { id }) { id, token, data -> Event.OrganizationEvent(id.value, token, data) }
    }

    private suspend fun FlowCollector<EventProducer>.processAnalytics(message: AnalyticsMessage) {
        val event = message as? AnalyticsCommentaryEvent ?: return
        updateEvent(
            event.id,
            Commentary(
                event.id,
                event.time,
                event.relativeTime,
                event.message,
                event.tags,
                event.teamIds.map { it.value },
                emptyList(),
                event.runIds.map { it.toString() }
            ),
            Event::CommentaryEvent
        )
    }


    private fun generateEventFeed(updates: Flow<ContestUpdate>) : Flow<Event> {
        var eventCounter = 1
        return updates.contestState().transform {state ->
            when (val event = state.event) {
                is InfoUpdate -> calculateDiff(state.infoBeforeEvent, event.newInfo)
                is RunUpdate -> processRun(state.infoBeforeEvent!!, event.newInfo)
                is AnalyticsUpdate -> processAnalytics(event.message)
            }
        }.map { it("live-cds-${eventCounter++}") }
    }

    private inline fun <X, reified T: GlobalEvent<X>> Flow<Event>.filterGlobalEvent(scope: CoroutineScope) = filterIsInstance<T>().map {
        it.data
    }.stateIn(scope, SharingStarted.Eagerly, null)
        .filterNotNull()
    private inline fun <X, reified T: IdEvent<X>> Flow<Event>.filterIdEvent(scope: CoroutineScope) = filterIsInstance<T>()
        .runningFold(persistentMapOf<String, X>()) { accumulator, value ->
            if (value.data == null) {
                accumulator.remove(value.id)
            } else {
                accumulator.put(value.id, value.data!!)
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
    var awardEventId = 0

    private fun generateAwardEvents(awards: Flow<List<Award>>) = awards.transform {
        diff(
            awardsMap,
            it,
            Award::id,
            { this },
            Event::AwardsEvent
        )
    }.map { it("live-cds-award-${awardEventId}") }


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
        val contestFlow = eventFeed.filterGlobalEvent<Contest, Event.ContestEvent>(scope)
        val stateFlow = eventFeed.filterGlobalEvent<State, Event.StateEvent>(scope)

        val judgementTypesFlow = eventFeed.filterIdEvent<JudgementType, Event.JudgementTypeEvent>(scope)
        val languagesFlow = eventFeed.filterIdEvent<Language, Event.LanguageEvent>(scope)
        val problemsFlow = eventFeed.filterIdEvent<Problem, Event.ProblemEvent>(scope)
        val groupsFlow = eventFeed.filterIdEvent<Group, Event.GroupsEvent>(scope)
        val organizationsFlow = eventFeed.filterIdEvent<Organization, Event.OrganizationEvent>(scope)
        val teamsFlow = eventFeed.filterIdEvent<Team, Event.TeamEvent>(scope)
        val submissionsFlow = eventFeed.filterIdEvent<Submission, Event.SubmissionEvent>(scope)
        val judgementsFlow = eventFeed.filterIdEvent<Judgement, Event.JudgementEvent>(scope)
        //val runsFlow = eventFeed.filterIdEvent<Run, Event.RunsEvent>(scope)
        val commentaryFlow = eventFeed.filterIdEvent<Commentary, Event.CommentaryEvent>(scope)
        //val personsFlow = eventFeed.filterIdEvent<Person, Event.PersonEvent>(scope)
        //val accountsFlow = eventFeed.filterIdEvent<Account, Event.AccountEvent>(scope)
        //val clarificationsFlow = eventFeed.filterIdEvent<Clarification, Event.ClarificationEvent>(scope)
        val awardsFlow = eventFeed.filterIdEvent<Award, Event.AwardsEvent>(scope)

        val json = defaultJsonSettings()
        route("/api") {
            get {
                call.respond(
                    ApiInfo(
                        version = "2022_07",
                        versionUrl = "https://ccs-specs.icpc.io/2022-07/contest_api",
                        name = "icpc live"
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

    private fun ScoreboardAndContestInfo.toClicsScoreboard() = Scoreboard(
        time = info.startTime + lastSubmissionTime,
        contest_time = lastSubmissionTime,
        state = getState(info),
        rows = scoreboardSnapshot.order.zip(scoreboardSnapshot.ranks).map { (teamId, rank) ->
            val row = scoreboardSnapshot.rows[teamId]!!
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

    private fun ScoreboardAndContestInfo.toClicsAwards() = buildList {
        for (award in scoreboardSnapshot.awards) {
            add(Award(award.id, award.citation, award.teams.map { it.value }))
        }
        for ((index, problem) in info.scoreboardProblems.withIndex()) {
            add(Award(
                "first-to-solve-${problem.id}",
                "First to solve problem ${problem.displayName}",
                scoreboardSnapshot.rows.entries
                    .filter { (it.value.problemResults[index] as? ICPCProblemResult)?.isFirstToSolve == true }
                    .map { it.key.value }
            ))
        }
    }
}
