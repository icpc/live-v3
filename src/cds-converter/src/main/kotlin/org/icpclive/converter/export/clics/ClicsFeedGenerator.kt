package org.icpclive.converter.export.clics

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.stateInCompletable
import org.icpclive.clics.Url
import org.icpclive.clics.events.*
import org.icpclive.clics.objects.*
import org.icpclive.clics.objects.Award
import org.icpclive.clics.objects.ScoreboardRow
import org.icpclive.clics.withQueryParams
import kotlin.collections.map

data class EventWithOldValue(val event: Event, val oldValue: Any?)

typealias EventProducer = (EventToken) -> EventWithOldValue

internal class ClicsFeedGenerator(scope: CoroutineScope, updates: Flow<ContestStateWithScoreboard>, private val isAdmin: Boolean) {
    val eventFeed = generateEventFeed(updates)
        .shareIn(scope, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
        .transformWhile {
            emit(it)
            it.event !is StateEvent || it.event.data.endOfUpdates == null
        }
    val contestFlow = eventFeed.filterGlobalEvent<_, ContestEvent>(scope)
    val stateFlow = eventFeed.filterGlobalEvent<_, StateEvent>(scope)

    val judgementTypesFlow = eventFeed.filterIdEvent<_, JudgementTypeEvent>(scope)
    val languagesFlow = eventFeed.filterIdEvent<_, LanguageEvent>(scope)
    val problemsFlow = eventFeed.filterIdEvent<_, ProblemEvent>(scope)
    val groupsFlow = eventFeed.filterIdEvent<_, GroupEvent>(scope)
    val organizationsFlow = eventFeed.filterIdEvent<_, OrganizationEvent>(scope)
    val teamsFlow = eventFeed.filterIdEvent<_, TeamEvent>(scope)
    val submissionsFlow = eventFeed.filterIdEvent<_, SubmissionEvent>(scope)
    val judgementsFlow = eventFeed.filterIdEvent<_, JudgementEvent>(scope)
    //val runsFlow = eventFeed.filterIdEvent<_, RunsEvent>(scope)
    val commentaryFlow = eventFeed.filterIdEvent<_, CommentaryEvent>(scope)
    val personsFlow = eventFeed.filterIdEvent<_, PersonEvent>(scope)
    val accountsFlow = eventFeed.filterIdEvent<_, AccountEvent>(scope)
    //val clarificationsFlow = eventFeed.filterIdEvent<_, ClarificationEvent>(scope)
    val awardsFlow = eventFeed.filterIdEvent<_, AwardEvent>(scope)
    suspend fun getScoreboard() = currentState.filterNotNull().first().toClicsScoreboard()



    private fun sanitize(s: String) = s.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    private val TeamId.sanitizedValue get() = sanitize(value)
    private val OrganizationId.sanitizedValue get() = sanitize(value)
    private val LanguageId.sanitizedValue get() = sanitize(value)
    private val ProblemId.sanitizedValue get() = sanitize(value)
    private val GroupId.sanitizedValue get() = sanitize(value)
    private val RunId.sanitizedValue get() = sanitize(value)
    private val PersonId.sanitizedValue get() = sanitize(value)
    private val CommentaryMessageId.sanitizedValue get() = sanitize(value)
    private val AccountId.sanitizedValue get() = sanitize(value)

    private fun ProblemInfo.toClicsProblem() = Problem(
        id = id.sanitizedValue,
        ordinal = ordinal,
        label = displayName,
        name = fullName,
        rgb = color?.value,
        testDataCount = 1,
        maxScore = maxScore
    )

    private fun GroupInfo.toClicsGroup() = Group(
        id = id.sanitizedValue,
        name = displayName,
    )

    private fun OrganizationInfo.toClicsOrg() = Organization(
        id = id.sanitizedValue,
        name = displayName,
        formalName = fullName,
        logo = logo.mapNotNull { it.toClicsMedia() },
        country = country,
        countryFlag = countryFlag.mapNotNull { it.toClicsMedia() },
        countrySubdivision = countrySubdivision,
        countrySubdivisionFlag = countrySubdivisionFlag.mapNotNull { it.toClicsMedia() },
        icpcId = customFields["icpc_id"],
        twitterAccount = customFields["clicsTwitterAccount"],
        twitterHashtag = customFields["clicsTwitterHashtag"],
    )

    private fun LanguageInfo.toClicsLang() = Language(
        id = id.sanitizedValue,
        name = name,
        extensions = extensions
    )

    private fun PersonInfo.toClicsPerson() = Person(
        id = id.sanitizedValue,
        name = name,
        icpcId = icpcId,
        teamIds = teamIds.map { it.sanitizedValue },
        title = title,
        email = email,
        sex = sex,
        role = role,
        photo = photo.mapNotNull { it.toClicsMedia() },
    )

    private fun AccountInfo.toClicsAccount() = Account(
        id = id.sanitizedValue,
        username = username,
        name = name,
        password = password?.value?.takeIf { isAdmin },
        type = type,
        teamId = teamId?.sanitizedValue,
        personId = personId?.sanitizedValue,
    )

    private fun CommentaryMessage.toClicsCommentary() = Commentary(
        id = id.sanitizedValue,
        time = time,
        contestTime = relativeTime,
        message = message,
        tags = tags,
        teamIds = teamIds.map { it.sanitizedValue },
        problemIds = runIds.map { it.sanitizedValue },
        submissionIds = emptyList(),
    )


    private fun MediaType.toClicsMedia(): File? {
        return when (this) {
            is MediaType.Object -> null
            is MediaType.Image -> File(mime ?: "image", Url(url), width = width, height = height, filename = filename, tag = tags, hash = hash)
            is MediaType.Video -> File(mime ?: "video", Url(url), filename = filename, tag = tags, hash = hash)
            is MediaType.Audio -> File(mime ?: "audio", Url(url), filename = filename, tag = tags, hash = hash)
            is MediaType.Text -> File(mime ?: "text/plain", Url(url), filename = filename, tag = tags, hash = hash)
            is MediaType.ZipArchive -> File(mime ?: "application/zip", Url(url), filename = filename, tag = tags, hash = hash)
            is MediaType.M2tsVideo -> File(mime ?: "video/m2ts", Url(url), filename = filename, tag = tags, hash = hash)
            is MediaType.HLSVideo -> File(mime ?: "application/vnd.apple.mpegurl", Url(url), filename = filename, tag = tags, hash = hash)
            is MediaType.WebRTCGrabberConnection -> File(
                mime ?: "application/vnd.webrtc-grabber.stream",
                Url(this.url).withQueryParams("peerName" to peerName, "streamType" to streamType, "credential" to (credential ?: "")), filename = filename, tag = tags, hash = hash
            )

            is MediaType.WebRTCProxyConnection -> null
        }
    }

    private fun TeamInfo.toClicsTeam() = Team(
        id = id.sanitizedValue,
        label = customFields["label"] ?: id.sanitizedValue,
        icpcId = customFields["icpc_id"],
        name = fullName,
        displayName = displayName,
        hidden = isHidden,
        groupIds = groups.map { it.sanitizedValue },
        organizationId = organizationId?.sanitizedValue,
        photo = medias[TeamMediaType.PHOTO]?.mapNotNull { it.toClicsMedia() }.orEmpty(),
        video = medias[TeamMediaType.RECORD]?.mapNotNull { it.toClicsMedia() }.orEmpty(),
        desktop = medias[TeamMediaType.SCREEN]?.mapNotNull { it.toClicsMedia() }.orEmpty(),
        webcam = medias[TeamMediaType.CAMERA]?.mapNotNull { it.toClicsMedia() }.orEmpty(),
        audio = medias[TeamMediaType.AUDIO]?.mapNotNull { it.toClicsMedia() }.orEmpty(),
        backup = medias[TeamMediaType.BACKUP]?.mapNotNull { it.toClicsMedia() }.orEmpty(),
        keyLog = medias[TeamMediaType.KEYLOG]?.mapNotNull { it.toClicsMedia() }.orEmpty(),
        toolData = medias[TeamMediaType.TOOL_DATA]?.mapNotNull { it.toClicsMedia() }.orEmpty(),
    )

    private fun Verdict.toJudgmentType() = when (this) {
        Verdict.Accepted -> JudgementType("AC", "correct", solved = true, penalty = false)

        Verdict.Fail -> JudgementType("JE", "judging error", solved = false, penalty = false)
        Verdict.Ignored -> JudgementType("IG", "ignored", solved = false, penalty = false)
        Verdict.CompilationError -> JudgementType("CE", "compiler error", solved = false, penalty = false)

        Verdict.Challenged -> JudgementType("CH", "challenged", solved = false, penalty = true)

        Verdict.CompilationErrorWithPenalty -> JudgementType("CEP", "compiler error penalised", solved = false, penalty = true)
        Verdict.IdlenessLimitExceeded -> JudgementType("IL", "idleness limit", solved = false, penalty = true)
        Verdict.MemoryLimitExceeded -> JudgementType("MLE", "memory limit", solved = false, penalty = true)
        Verdict.OutputLimitExceeded -> JudgementType("OLE", "output limit", solved = false, penalty = true)
        Verdict.PresentationError -> JudgementType("PE", "presentation error", solved = false, penalty = true)
        Verdict.Rejected -> JudgementType("RE", "rejected", solved = false, penalty = true)
        Verdict.RuntimeError -> JudgementType("RTE", "run-time error", solved = false, penalty = true)
        Verdict.SecurityViolation -> JudgementType("SV", "security violation", solved = false, penalty = true)
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

    private suspend fun <ID, T> FlowCollector<EventProducer>.updateEvent(id: ID, data: T?, oldData: T?, block : (ID, EventToken, T?) -> Event) = emit {
        EventWithOldValue(block(id, it, data), oldData)
    }

    private suspend fun <T> FlowCollector<EventProducer>.updateEvent(data: T, oldData: T?, block : (EventToken, T) -> Event) = emit {
        EventWithOldValue(block(it, data), oldData)
    }

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

    private suspend fun <T: ObjectWithId> FlowCollector<EventProducer>.diffChange(
        old: MutableMap<String, T>,
        new: List<T>,
        toFinalEvent: (String, EventToken, T?) -> Event
    ) {
        for (n in new) {
            if (old[n.id] != n) {
                updateEvent(n.id, n, old[n.id],toFinalEvent)
                old[n.id] = n
            }
        }
    }

    private suspend fun <T : ObjectWithId> FlowCollector<EventProducer>.diffRemove(
        old: MutableMap<String, T>,
        new: List<T>,
        toFinalEvent: (String, EventToken, T?) -> Event
    ) {
        val values = new.map { it.id }.toSet()
        val toRemove = buildList {
            for (k in old.keys) {
                if (k !in values) {
                    updateEvent(k, null, old[k], toFinalEvent)
                    add(k)
                }
            }
        }
        for (i in toRemove) {
            old.remove(i)
        }
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
            id = run.id.sanitizedValue,
            languageId = (run.languageId ?: unknownLanguage.id).sanitizedValue,
            problemId = run.problemId.sanitizedValue,
            teamId = run.teamId.sanitizedValue,
            time = info.startTimeOrZero + run.time,
            contestTime = run.time,
            files = run.sourceFiles.mapNotNull { it.toClicsMedia() },
            reaction = run.reactionVideos.mapNotNull { it.toClicsMedia() },
        ).takeUnless { run.isHidden }
        val judgement = when (val result = run.result) {
            is RunResult.ICPC -> Judgement(
                id = run.id.sanitizedValue,
                submissionId = run.id.sanitizedValue,
                judgementTypeId = judgmentTypes[result.verdict]?.id,
                startTime = info.startTimeOrZero + run.time,
                startContestTime = run.time,
                endTime = info.startTimeOrZero + run.time,
                endContestTime = run.time,
                current = true,
            )

            is RunResult.IOI -> Judgement(
                id = run.id.sanitizedValue,
                submissionId = run.id.sanitizedValue,
                judgementTypeId = judgmentTypes[result.wrongVerdict ?: Verdict.Accepted]?.id,
                score = when (info.problems[run.problemId]?.scoreMergeMode) {
                    ScoreMergeMode.MAX_PER_GROUP, ScoreMergeMode.SUM, null -> result.scoreAfter
                    ScoreMergeMode.MAX_TOTAL, ScoreMergeMode.LAST, ScoreMergeMode.LAST_OK -> result.score.sum()
                },
                startTime = info.startTimeOrZero + run.time,
                startContestTime = run.time,
                endTime = info.startTimeOrZero + run.time,
                endContestTime = run.time,
                current = true,
            )
            is RunResult.InProgress -> Judgement(
                id = run.id.sanitizedValue,
                submissionId = run.id.sanitizedValue,
                startTime = info.startTimeOrZero + run.time,
                startContestTime = run.time,
                current = true,
            )
        }.takeUnless { run.isHidden }
        val (oldSubmission, oldJudgment) = submissions[run.id] ?: (null to null)
        if (submission != oldSubmission) {
            updateEvent(
                run.id.sanitizedValue,
                submission,
                oldSubmission,
                ::SubmissionEvent
            )
        }
        if (judgement != oldJudgment) {
            updateEvent(
                run.id.sanitizedValue,
                judgement,
                oldJudgment,
                ::JudgementEvent
            )
        }
    }

    private suspend fun <T> FlowCollector<EventProducer>.diff(oldInfo: ContestInfo?, newInfo: ContestInfo, getter: ContestInfo.() -> T, event : (EventToken, T) -> Event) {
        val old = oldInfo?.getter()
        val new = newInfo.getter()
        if (old != new) {
            updateEvent(new, old, event)
        }
    }

    private val groupsMap = mutableMapOf<String, Group>()
    private val orgsMap = mutableMapOf<String, Organization>()
    private val languagesMap = mutableMapOf<String, Language>()
    private val problemsMap = mutableMapOf<String, Problem>()
    private val teamsMap = mutableMapOf<String, Team>()
    private val awardsMap = mutableMapOf<String, Award>()
    private val personsMap = mutableMapOf<String, Person>()
    private val accountsMap = mutableMapOf<String, Account>()
    private val commentariesMap = mutableMapOf<String, Commentary>()

    @OptIn(InefficientContestInfoApi::class)
    private suspend fun FlowCollector<EventProducer>.calculateDiff(oldInfo: ContestInfo?, newInfo: ContestInfo) {
        diff(oldInfo, newInfo, ::getContest, ::ContestEvent)
        diff(oldInfo, newInfo, ::getState, ::StateEvent)
        if (oldInfo == null) {
            for (type in judgmentTypes.values) {
                updateEvent(type.id, type,  null,::JudgementTypeEvent)
            }
        }
        val clicsProblems = newInfo.problemList.map { it.toClicsProblem() }
        val clicsGroups = newInfo.groupList.map { it.toClicsGroup() }
        val clicsOrgs = newInfo.organizationList.map { it.toClicsOrg() }
        val clicsLangs = (newInfo.languagesList + unknownLanguage).map { it.toClicsLang() }
        val clicsTeams = newInfo.teamList.map { it.toClicsTeam() }
        val clicsPersons = newInfo.personsList.map { it.toClicsPerson() }
        val clicsAccounts = newInfo.accountsList.map { it.toClicsAccount() }

        diffChange(problemsMap, clicsProblems, ::ProblemEvent)
        diffChange(groupsMap, clicsGroups, ::GroupEvent)
        diffChange(orgsMap, clicsOrgs, ::OrganizationEvent)
        diffChange(languagesMap, clicsLangs, ::LanguageEvent)
        diffChange(teamsMap, clicsTeams, ::TeamEvent)
        diffChange(personsMap, clicsPersons, ::PersonEvent)
        diffChange(accountsMap, clicsAccounts, ::AccountEvent)

        diffRemove(accountsMap, clicsAccounts, ::AccountEvent)
        diffRemove(personsMap, clicsPersons, ::PersonEvent)
        diffRemove(teamsMap, clicsTeams, ::TeamEvent)
        diffRemove(groupsMap, clicsGroups, ::GroupEvent)
        diffRemove(orgsMap, clicsOrgs, ::OrganizationEvent)
        diffRemove(languagesMap, clicsLangs, ::LanguageEvent)
        diffRemove(problemsMap, clicsProblems, ::ProblemEvent)
    }

    private suspend fun FlowCollector<EventProducer>.processCommentaryMessage(event: CommentaryMessage) {
        diffChange(
            commentariesMap,
            listOf(event.toClicsCommentary()),
            ::CommentaryEvent
        )
    }

    private val currentState = MutableStateFlow<ContestStateWithScoreboard?>(null)

    private fun generateEventFeed(updates: Flow<ContestStateWithScoreboard>) : Flow<EventWithOldValue> {
        var eventCounter = 1
        return updates.transform { state ->
            currentState.value = state
            when (val event = state.state.lastEvent) {
                is InfoUpdate -> calculateDiff(state.state.infoBeforeEvent, event.newInfo)
                is RunUpdate -> processRun(state.state.infoBeforeEvent!!, event.newInfo)
                is CommentaryMessagesUpdate -> processCommentaryMessage(event.message)
            }
            val newAwards = state.toClicsAwards()
            diffChange(awardsMap, newAwards, ::AwardEvent)
            diffRemove(awardsMap, newAwards, ::AwardEvent)
        }.map { it(EventToken("live-cds-${eventCounter++}")) }
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
                    teamId.sanitizedValue,
                    ScoreboardRowScore(
                        numSolved = row.totalScore.toInt().takeIf { info.resultType == ContestResultType.ICPC },
                        totalTime = row.penalty.takeIf { info.resultType == ContestResultType.ICPC },
                        score = row.totalScore.takeIf { info.resultType == ContestResultType.IOI },
                        time = row.lastAccepted.takeIf { row.totalScore > 0 }
                    ),
                    row.problemResults.mapIndexed { index, v ->
                        when (v) {
                            is ICPCProblemResult -> ScoreboardRowProblem(
                                problemId = info.scoreboardProblems[index].id.sanitizedValue,
                                numJudged = v.wrongAttempts + (if (v.isSolved) 1 else 0),
                                numPending = v.pendingAttempts,
                                solved = v.isSolved,
                                time = v.lastSubmitTime?.inWholeMinutes.takeIf { v.isSolved }
                            )

                            is IOIProblemResult -> ScoreboardRowProblem(
                                problemId = info.scoreboardProblems[index].id.sanitizedValue,
                                numJudged = v.totalAttempts,
                                numPending = v.pendingAttempts,
                                score = v.score,
                                time = v.lastSubmitTime?.inWholeMinutes.takeIf { v.score != null }
                            )
                        }
                    }
                )
            }
        )
    }

    private fun ContestStateWithScoreboard.toClicsAwards() = buildList {
        val info = state.infoAfterEvent!!
        for (award in rankingAfter.awards) {
            add(Award(award.id, award.citation, award.teams.map { it.sanitizedValue }))
        }
        for ((index, problem) in info.scoreboardProblems.withIndex()) {
            add(
                Award(
                    "first-to-solve-${problem.id}",
                "First to solve problem ${problem.displayName}",
                rankingAfter.order.map { it to scoreboardRowAfter(it) }
                    .filter {
                        when (val result = it.second.problemResults[index]) {
                            is ICPCProblemResult -> result.isFirstToSolve
                            is IOIProblemResult -> result.isFirstBest
                        }
                    }
                    .map { it.first.sanitizedValue }
            ))
        }
    }
    private inline fun <X: Any, reified T: GlobalEvent<X>> Flow<EventWithOldValue>.filterGlobalEvent(scope: CoroutineScope) = map { it.event }
        .filterIsInstance<T>()
        .map { it.data }
        .stateInCompletable(scope)
        .filterNotNull()

    private inline fun <X: ObjectWithId, reified T: IdEvent<X>> Flow<EventWithOldValue>.filterIdEvent(scope: CoroutineScope) = map { it.event }
        .filterIsInstance<T>()
        .runningFold(persistentMapOf<String, X>()) { accumulator, value ->
            val data = value.data
            if (data == null) {
                accumulator.remove(value.id)
            } else {
                accumulator.put(value.id, data)
            }
        }.stateInCompletable(scope, emptyMap())

}