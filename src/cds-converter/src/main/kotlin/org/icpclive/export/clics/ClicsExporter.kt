import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import org.icpclive.api.*
import org.icpclive.cds.*
import org.icpclive.cds.adapters.stateGroupedByTeam
import org.icpclive.cds.adapters.withContestInfoBefore
import org.icpclive.cds.clics.FeedVersion

import org.icpclive.cds.clics.api.*
import org.icpclive.cds.clics.api.Scoreboard
import org.icpclive.cds.clics.api.ScoreboardRow
import org.icpclive.scoreboard.calculateScoreboardWithInfo
import org.icpclive.util.defaultJsonSettings
import org.icpclive.util.intervalFlow
import java.nio.ByteBuffer
import kotlin.time.Duration.Companion.minutes

typealias EventProducer = (String) -> Event

private fun ProblemInfo.toClicsProblem() = Problem(
    id = contestSystemId,
    ordinal = ordinal,
    label = letter,
    name = name,
    rgb = color,
    test_data_count = 1,
)

private fun GroupInfo.toClicsGroup() = Group(
    id = name,
    name = name,
)

private fun OrganizationInfo.toClicsOrg() = Organization(
    id = cdsId,
    name = shortname,
    formal_name = name,
)

private fun TeamInfo.toClicsTeam() = Team(
    id = contestSystemId,
    name = name,
    hidden = isHidden,
    group_ids = groups,
    organization_id = organizationId
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

    private suspend fun <T> FlowCollector<EventProducer>.updateEvent(id: String, data: T, block : (String, String, T?) -> Event) = emit {
        block(id, it, data)
    }

    private suspend fun <T> FlowCollector<EventProducer>.updateEvent(data: T, block : (String, T?) -> Event) = emit { block(it, data) }

    private fun getContest(info: ContestInfo) = Contest(
            id = "contest",
            start_time = info.startTime.takeIf { it != Instant.fromEpochSeconds(0) },
            name = info.name,
            formal_name = info.name,
            duration = info.contestLength,
            scoreboard_freeze_duration = info.freezeTime,
            countdown_pause_time = info.holdBeforeStartTime,
            penalty_time = info.penaltyPerWrongAttempt,
            scoreboard_type = "pass-fail"
        )

    private suspend fun <T, CT> FlowCollector<EventProducer>.diffChange(
        old: MutableMap<String, T>,
        new: List<T>,
        id: T.() -> String,
        convert: (T) -> CT,
        toFinalEvent: (String, String, CT?) -> Event
    ) {
        for (n in new) {
            if (old[n.id()] != n) {
                updateEvent(n.id(), convert(n), toFinalEvent)
                old[n.id()] = n
            }
        }
    }

    private suspend fun <T, CT> FlowCollector<EventProducer>.diffRemove(
        old: MutableMap<String, T>,
        new: List<T>,
        id: T.() -> String,
        toFinalEvent: (String, String, CT?) -> Event
    ) {
        val values = new.map { it.id() }.toSet()
        for (k in old.keys) {
            if (k !in values) {
                updateEvent(k, null, toFinalEvent)
            }
        }
    }

    private suspend fun <T, CT> FlowCollector<EventProducer>.diff(
        old: MutableMap<String, T>,
        new: List<T>,
        id: T.() -> String,
        convert: (T) -> CT,
        toFinalEvent: (String, String, CT?) -> Event
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
            end_of_updates = null
        )

        ContestStatus.RUNNING -> State(
            ended = null,
            frozen = if (info.currentContestTime >= info.freezeTime) info.startTime + info.freezeTime else null,
            started = info.startTime,
            unfrozen = null,
            finalized = null,
            end_of_updates = null
        )

        ContestStatus.OVER -> State(
            ended = info.startTime + info.contestLength,
            frozen = info.startTime + info.freezeTime,
            started = info.startTime,
            unfrozen = null,
            finalized = null,
            end_of_updates = null
        )
    }

    private val submissionsCreated = mutableSetOf<Int>()
    private var teamIdToCdsId = mapOf<Int, String>()
    private var problemIdToCdsId = mapOf<Int, String>()

    private suspend fun FlowCollector<EventProducer>.processRun(info: ContestInfo, run: RunInfo) {
        if (run.id !in submissionsCreated) {
            submissionsCreated.add(run.id)
            updateEvent(
                run.id.toString(),
                Submission(
                    id = run.id.toString(),
                    language_id = unknownLanguage.id,
                    problem_id = problemIdToCdsId[run.problemId]!!,
                    team_id = teamIdToCdsId[run.teamId]!!,
                    time = info.startTime + run.time,
                    contest_time = run.time,
                ),
                Event::SubmissionEvent
            )
        }
        val result = run.result
        if (result is ICPCRunResult) {
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
    }

    private suspend fun <T> FlowCollector<EventProducer>.diff(oldInfo: ContestInfo?, newInfo: ContestInfo, getter: ContestInfo.() -> T, event : (String, T?) -> Event) {
        val old = oldInfo?.getter()
        val new = newInfo.getter()
        if (old != new) {
            updateEvent(new, event)
        }
    }

    private val groupsMap = mutableMapOf<String, GroupInfo>()
    private val orgsMap = mutableMapOf<String, OrganizationInfo>()
    private val problemsMap = mutableMapOf<String, ProblemInfo>()
    private val teamsMap = mutableMapOf<String, TeamInfo>()

    private suspend fun FlowCollector<EventProducer>.calculateDiff(oldInfo: ContestInfo?, newInfo: ContestInfo) {
        problemIdToCdsId = newInfo.problems.associate { it.id to it.contestSystemId }
        teamIdToCdsId = newInfo.teams.associate { it.id to it.contestSystemId }

        diff(oldInfo, newInfo, ::getContest, Event::ContestEventNamedWithSpec)
        diff(oldInfo, newInfo, ::getState, Event::StateEvent)
        if (oldInfo == null) {
            for (type in judgmentTypes.values) {
                updateEvent(type.id, type, Event::JudgementTypeEvent)
            }
            for (language in languages) {
                updateEvent(language.id, language, Event::LanguageEvent)
            }
        }
        diff(problemsMap, newInfo.problems, ProblemInfo::contestSystemId, ProblemInfo::toClicsProblem, Event::ProblemEvent)
        diffChange(groupsMap, newInfo.groups, GroupInfo::name, GroupInfo::toClicsGroup, Event::GroupsEvent)
        diffChange(orgsMap, newInfo.organizations, OrganizationInfo::cdsId, OrganizationInfo::toClicsOrg, Event::OrganizationEvent)

        diff(teamsMap, newInfo.teams, TeamInfo::contestSystemId, TeamInfo::toClicsTeam, Event::TeamEvent)

        diffRemove(groupsMap, newInfo.groups, GroupInfo::name, Event::GroupsEvent)
        diffRemove(orgsMap, newInfo.organizations, OrganizationInfo::cdsId, Event::OrganizationEvent)
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
                event.teamIds.map { teamIdToCdsId[it]!! },
                emptyList(),
                event.runIds.map { it.toString() }
            ),
            Event::CommentaryEvent
        )
    }


    private suspend fun FlowCollector<Event>.generateEventFeed(updates: Flow<ContestUpdate>) {
        var eventCounter = 1
        updates.withContestInfoBefore().transform { (update, infoBefore) ->
            when (update) {
                is InfoUpdate -> calculateDiff(infoBefore, update.newInfo)
                is RunUpdate -> processRun(infoBefore!!, update.newInfo)
                is AnalyticsUpdate -> processAnalytics(update.message)
            }
        }.collect {
            emit(it("live-cds-${eventCounter++}"))
        }
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


    fun Route.setUp(scope: CoroutineScope, updates: Flow<ContestUpdate>) {
        val eventFeed = flow {
            generateEventFeed(updates)
        }.shareIn(scope, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
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
        //val awardsFlow = eventFeed.filterIdEvent<Award, Event.AwardsEvent>(scope)
        val scoreboardFlow = updates.stateGroupedByTeam()
            .calculateScoreboardWithInfo(OptimismLevel.NORMAL).map {(info, scorboard) ->
                val tids = info.teams.associate { it.id to it.contestSystemId }
                Scoreboard(
                    time = info.startTime + scorboard.lastSubmitTime,
                    contest_time = scorboard.lastSubmitTime,
                    state = getState(info),
                    rows = scorboard.rows.map {
                        ScoreboardRow(
                            it.rank,
                            tids[it.teamId]!!,
                            ScoreboardRowScore(it.totalScore.toInt(), it.penalty.inWholeMinutes),
                            it.problemResults.mapIndexed { index, v ->
                                val iv = v as ICPCProblemResult
                                ScoreboardRowProblem(
                                    info.problems[index].contestSystemId,
                                    iv.wrongAttempts + (if (iv.isSolved) 1 else 0),
                                    iv.pendingAttempts,
                                    iv.isSolved,
                                    iv.lastSubmitTime?.inWholeMinutes.takeIf { iv.isSolved }
                                )
                            }
                        )
                    }
                )
            }.stateIn(scope, SharingStarted.Eagerly, null)
            .filterNotNull()

        val json = defaultJsonSettings()
        route("/api") {
            get {
                call.respond(
                    ApiInfo(
                        FeedVersion.`2022_07`,
                        "https://ccs-specs.icpc.io/2022-07/contest_api",
                        ApiProvider("icpc live")
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
                    //getId("awards", awardsFlow)
                    get("/scoreboard") { call.respond(scoreboardFlow.first()) }
                    get("/event-feed") {
                        call.respondBytesWriter {
                            merge(eventFeed.map { json.encodeToString(it) }, intervalFlow(2.minutes).map { "" }).collect {
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