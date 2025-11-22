package org.icpclive.cds.plugins.clics

import org.icpclive.cds.api.*
import org.icpclive.clics.events.*
import org.icpclive.clics.objects.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal class ClicsModel {
    private val contestInfoListeners = mutableListOf<suspend (ContestInfo) -> Unit>()
    private val runInfoListeners = mutableListOf<suspend (RunInfo) -> Unit>()
    private val commentaryMessageListeners = mutableListOf<suspend (CommentaryMessage) -> Unit>()

    private val judgementTypes = mutableMapOf<String, JudgementType>()
    private val problems = mutableMapOf<String, Problem>()
    private val languages = mutableMapOf<String, Language>()
    private val organizations = mutableMapOf<String, Organization>()
    private val teams = mutableMapOf<String, Team>()
    private val submissions = mutableMapOf<String, Submission>()
    private val removedSubmissionIds = mutableSetOf<String>()
    private val commentaries = mutableMapOf<String, Commentary>()
    private val submissionJudgmentIds = mutableMapOf<String, MutableSet<String>>()
    private val judgements = mutableMapOf<String, Judgement>()
    private val runs = mutableMapOf<String, Run>()
    private val judgmentRunIds = mutableMapOf<String, MutableSet<String>>()
    private val groups = mutableMapOf<String, Group>()

    private var startTime: Instant? = null
    private var contestLength = 5.hours
    private var freezeTime = 4.hours
    private var status: ContestStatus = ContestStatus.BEFORE()
    private var penaltyPerWrongAttempt = 20.minutes
    private var holdBeforeStartTime: Duration? = null
    private var name: String = ""

    suspend fun addContestInfoListener(callback: suspend (ContestInfo) -> Unit) {
        contestInfoListeners.add(callback)
        callback(contestInfo)
    }
    suspend fun addRunInfoListener(callback: suspend (RunInfo) -> Unit) {
        runInfoListeners.add(callback)
        for (i in submissions.values) {
            callback(i.toApi())
        }
    }
    suspend fun addCommentaryMessageListener(callback: suspend (CommentaryMessage) -> Unit) {
        commentaryMessageListeners.add(callback)
        for (i in commentaries.values) {
            callback(i.toApi())
        }
    }
    private suspend fun contestInfoUpdated() {
        val info = contestInfo
        for (listener in contestInfoListeners) {
            listener(info)
        }
    }
    private suspend fun submissionUpdated(id: String?) {
        val info = submissions[id]?.toApi() ?: return
        for (listener in runInfoListeners) {
            listener(info)
        }
    }
    private suspend fun commentaryUpdated(id: String) {
        val info = commentaries[id]?.toApi() ?: return
        for (listener in commentaryMessageListeners) {
            listener(info)
        }
    }

    private fun File.toApi(): MediaType? {
        val mime = mime ?: return null
        val href = href?.value ?: return null
        return when {
            mime.startsWith("image") -> MediaType.Image(url = href, width = width, height = height, filename = filename, hash = hash, tags = tag, mime = mime)
            mime.startsWith("audio") -> MediaType.Audio(url = href, filename = filename, hash = hash, tags = tag, mime = mime)
            mime.startsWith("video/m2ts") -> MediaType.M2tsVideo(url = href, filename = filename, hash = hash, tags = tag, mime = mime)
            mime.startsWith("application/vnd.apple.mpegurl") -> MediaType.HLSVideo(url = href, filename = filename, hash = hash, tags = tag, mime = mime)
            mime.startsWith("video") -> MediaType.Video(url = href, filename = filename, hash = hash, tags = tag, mime = mime)
            mime.startsWith("text") -> MediaType.Text(url = href, filename = filename, hash = hash, tags = tag, mime = mime)
            mime.startsWith("application/zip") -> MediaType.ZipArchive(url = href, filename = filename, hash = hash, tags = tag, mime = mime)
            else -> null
        }
    }

    private fun Group.toApi(): GroupInfo = GroupInfo(id.toGroupId(), name!!, isHidden = false, isOutOfContest = false)

    private fun Team.toApi(): TeamInfo {
        val teamOrganization = organizationId?.let { organizations[it] }
        return TeamInfo(
            id = id.toTeamId(),
            fullName = name,
            displayName = displayName ?: name,
            isHidden = hidden ?: false,
            groups = buildList {
                for (group in groupIds) {
                    groups[group]?.let {
                        add(it.id.toGroupId())
                    }
                }
                teamOrganization?.country?.takeIf { it.isNotEmpty() }?.let { add(it.toGroupId()) }
            },
            hashTag = teamOrganization?.twitterHashtag,
            medias = buildMap {
                put(TeamMediaType.PHOTO, photo.mapNotNull { it.toApi() })
                put(TeamMediaType.RECORD, video.mapNotNull { it.toApi() })
                put(TeamMediaType.CAMERA, webcam.mapNotNull { it.toApi() })
                put(TeamMediaType.SCREEN, desktop.mapNotNull { it.toApi() })
                put(TeamMediaType.AUDIO, audio.mapNotNull { it.toApi() })
                put(TeamMediaType.BACKUP, backup.mapNotNull { it.toApi() })
                put(TeamMediaType.KEYLOG, keyLog.mapNotNull { it.toApi() })
                put(TeamMediaType.TOOL_DATA, toolData.mapNotNull { it.toApi() })
            }.filterValues { it.isNotEmpty() },
            organizationId = organizationId?.toOrganizationId(),
            isOutOfContest = false,
            customFields = buildMap {
                put("clicsTeamFullName", name)
                put("clicsTeamDisplayName", displayName ?: name)
                label?.let { put("clicsTeamLabel", it) }
                icpcId?.let { put("icpc_id", it) }
            }
        )
    }

    private fun Submission.toApi(): RunInfo {
        val judgment = submissionJudgmentIds[id]?.mapNotNull { judgements[it] }?.filter { it.current != false }?.maxByOrNull { it.startContestTime }
        val problem = problems[problemId]
        val passedTests = judgment?.id?.let { judgmentRunIds[it] }?.size ?: 0
        val judgementType = judgementTypes[judgment?.judgementTypeId ?: judgment?.simplifiedJudgementTypeId]
        return RunInfo(
            id = id.toRunId(),
            result = if (judgementType == null) {
                val part = when (val count = problem?.testDataCount) {
                    null, 0 -> 0.0
                    else -> minOf(passedTests.toDouble() / count, 1.0)
                }
                RunResult.InProgress(part)
            } else {
                Verdict.lookup(
                    shortName = judgementType.id,
                    isAccepted = judgementType.solved,
                    isAddingPenalty = judgementType.penalty,
                ).toICPCRunResult()
            },
            problemId = problemId.toProblemId(),
            teamId = teamId.toTeamId(),
            time = contestTime,
            testedTime = judgment?.endContestTime,
            reactionVideos = reaction?.mapNotNull { it.toApi() } ?: emptyList(),
            languageId = languageId?.toLanguageId(),
            isHidden = id in removedSubmissionIds,
            sourceFiles = files.mapNotNull { it.toApi() }
        )
    }

    private fun Problem.toApi() = ProblemInfo(
        id = id.toProblemId(),
        displayName = label,
        fullName = name,
        ordinal = ordinal,
        color = rgb?.let { Color.normalize(it) }
    )

    private fun Organization.toApi() = OrganizationInfo(
        id = id.toOrganizationId(),
        displayName = name ?: formalName ?: id,
        fullName = formalName ?: name ?: id,
        logo = logo.mapNotNull { it.toApi() }
    )

    private fun Language.toApi() = LanguageInfo(
        id = id.toLanguageId(),
        name = name,
        extensions = extensions
    )

    val contestInfo: ContestInfo
        get() = ContestInfo(
            name = name,
            status = status,
            resultType = ContestResultType.ICPC,
            contestLength = contestLength,
            freezeTime = freezeTime,
            problemList = problems.values.map { it.toApi() },
            teamList = teams.values.map { it.toApi() },
            groupList = groups.values.map { it.toApi() },
            penaltyPerWrongAttempt = penaltyPerWrongAttempt,
            penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
            organizationList = organizations.values.map { it.toApi() },
            languagesList = languages.values.map { it.toApi() },
            cdsSupportsFinalization = true
        )

    private inline fun <reified T> putInSet(set: MutableMap<String, T>, id: String, data: T?, getId: T.() -> String) {
        if (data == null) {
            set.remove(id)
        } else {
            require(id == data.getId()) {
                "Mismatch of id in event and ${T::class.simpleName} object: in event = ${id}, in object = ${data.getId()}"
            }
            set[id] = data
        }
    }

    private inline fun <reified T> putInSetWithRemoved(set: MutableMap<String, T>, removed: MutableSet<String>, id: String, data: T?, getId: T.() -> String) {
        if (data == null) {
            removed.add(id)
        } else {
            require(id == data.getId()) {
                "Mismatch of id in event and ${T::class.simpleName} object: in event = ${id}, in object = ${data.getId()}"
            }
            set[id] = data
            removed.remove(id)
        }
    }

    private inline fun <reified T> putInSetLinked(
        set: MutableMap<String, T>,
        id: String,
        data: T?,
        getId: T.() -> String,
        referenceSet: MutableMap<String, MutableSet<String>>,
        getReferenceId: T.() -> String
    ) : List<String> = buildList {
        val old = set[id]
        old?.getReferenceId()?.let {
            referenceSet[it]?.remove(id)
            add(it)
        }
        data?.getReferenceId()?.let {
            referenceSet.getOrPut(it) { mutableSetOf() }.add(id)
            add(it)
        }
        putInSet(set, id, data, getId)
    }.distinct()

    private suspend fun processContest(contest: Contest?) {
        require(contest != null) { "Removing contest is not supported" }
        name = contest.formalName ?: contest.name ?: "Unknown"
        startTime = contest.startTime
        contestLength = contest.duration
        freezeTime = contestLength - (contest.scoreboardFreezeDuration ?: Duration.ZERO)
        holdBeforeStartTime = contest.countdownPauseTime
        penaltyPerWrongAttempt = (contest.penaltyTime ?: 20.minutes)
        if (status is ContestStatus.BEFORE) {
            status = ContestStatus.BEFORE(
                scheduledStartAt = startTime,
                holdTime = holdBeforeStartTime
            )
        }
        contestInfoUpdated()
    }

    private suspend fun processLanguage(id: String, language: Language?) {
        putInSet(languages, id, language, Language::id)
        contestInfoUpdated()
    }

    private suspend fun processProblem(id: String, problem: Problem?) {
        putInSet(problems, id, problem, Problem::id)
        contestInfoUpdated()
    }

    private suspend fun processOrganization(id: String, organization: Organization?) {
        putInSet(organizations, id, organization, Organization::id)
        contestInfoUpdated()
    }

    private suspend fun processTeam(id: String, team: Team?) {
        putInSet(teams, id, team, Team::id)
        contestInfoUpdated()
    }

    private suspend fun processJudgementType(id: String, judgementType: JudgementType?) {
        putInSet(judgementTypes, id, judgementType, JudgementType::id)
        for ((_, submission) in submissions) {
            if (submissionJudgmentIds[submission.id]?.any { judgements[it]?.judgementTypeId == id } == true) {
                submissionUpdated(submission.id)
            }
        }
    }

    private suspend fun processGroup(id: String, group: Group?) {
        putInSet(groups, id, group, Group::id)
        contestInfoUpdated()
    }

    private suspend fun processSubmission(id: String, submission: Submission?) {
        putInSetWithRemoved(submissions, removedSubmissionIds, id, submission, Submission::id)
        submissionUpdated(id)
    }

    private suspend fun processJudgement(id: String, judgement: Judgement?) {
        putInSetLinked(
            set = judgements,
            id = id,
            data = judgement,
            getId = Judgement::id,
            referenceSet = submissionJudgmentIds,
            getReferenceId = Judgement::submissionId
        ).forEach { submissionUpdated(it) }
    }

    private suspend fun processRun(id: String, run: Run?) {
        putInSetLinked(
            set = runs,
            id = id,
            data = run,
            getId = Run::id,
            referenceSet = judgmentRunIds,
            getReferenceId = Run::judgementId
        ).forEach { judgements[it]?.let { j -> submissionUpdated(j.submissionId)} }
    }

    private suspend fun processCommentary(id: String, commentary: Commentary?) {
        putInSet(commentaries, id, commentary, Commentary::id)
        commentaryUpdated(id)
    }

    private fun Commentary.toApi() = CommentaryMessage(
        id.toCommentaryMessageId(),
        message,
        time,
        contestTime,
        teamIds?.map { it.toTeamId() } ?: emptyList(),
        submissionIds?.map { it.toRunId() } ?: emptyList(),
    )

    private suspend fun processState(state: State?) {
        require(state != null) { "Removing state is not supported" }
        status = when {
            state.endOfUpdates != null -> ContestStatus.FINALIZED(
                startedAt = state.started!!,
                finishedAt = state.ended!!,
                finalizedAt = state.endOfUpdates!!,
                frozenAt = state.frozen
            )

            state.ended != null -> ContestStatus.OVER(
                startedAt = state.started!!,
                finishedAt = state.ended!!,
                frozenAt = state.frozen
            )

            state.started != null -> ContestStatus.RUNNING(
                startedAt = state.started!!,
                frozenAt = state.frozen
            )

            else -> ContestStatus.BEFORE(
                scheduledStartAt = startTime,
                holdTime = holdBeforeStartTime
            )
        }
        contestInfoUpdated()
    }

    private suspend fun <T> processBatch(batch: List<T>, keys: Set<String>, single: suspend (String, T?) -> Unit, id: T.() -> String) {
        for (kid in keys.toSet() - batch.map { it.id() }.toSet()) {
            single(kid, null)
        }
        for (e in batch) {
            single(e.id(), e)
        }
    }

    suspend fun processEvent(event: Event) {
        when (event) {
            is BatchCommentaryEvent -> processBatch(event.data, commentaries.keys, ::processCommentary, Commentary::id)
            is BatchGroupEvent -> processBatch(event.data, groups.keys, ::processGroup, Group::id)
            is BatchJudgementEvent -> processBatch(event.data, judgements.keys, ::processJudgement, Judgement::id)
            is BatchJudgementTypeEvent ->  processBatch(event.data, judgementTypes.keys, ::processJudgementType, JudgementType::id)
            is BatchLanguageEvent -> processBatch(event.data, languages.keys, ::processLanguage, Language::id)
            is BatchOrganizationEvent -> processBatch(event.data, organizations.keys, ::processOrganization, Organization::id)
            is BatchProblemEvent -> processBatch(event.data, problems.keys, ::processProblem, Problem::id)
            is BatchRunEvent -> processBatch(event.data, runs.keys, ::processRun, Run::id)
            is BatchSubmissionEvent -> processBatch(event.data, submissions.keys, ::processSubmission, Submission::id)
            is BatchTeamEvent -> processBatch(event.data, teams.keys, ::processTeam, Team::id)
            is ContestEvent -> processContest(event.data)
            is ProblemEvent -> processProblem(event.id, event.data)
            is OrganizationEvent -> processOrganization(event.id, event.data)
            is TeamEvent -> processTeam(event.id, event.data)
            is StateEvent -> processState(event.data)
            is JudgementTypeEvent -> processJudgementType(event.id, event.data)
            is GroupEvent -> processGroup(event.id, event.data)
            is LanguageEvent -> processLanguage(event.id, event.data)
            is SubmissionEvent -> processSubmission(event.id, event.data)
            is JudgementEvent -> processJudgement(event.id, event.data)
            is RunEvent -> processRun(event.id, event.data)
            is CommentaryEvent -> processCommentary(event.id, event.data)
            is PreloadFinishedEvent -> {}
            is AwardEvent, is AccountEvent, is PersonEvent, is ClarificationEvent -> {}
            is BatchAccountEvent, is BatchAwardEvent, is BatchPersonEvent, is BatchClarificationEvent -> {}
        }
    }

}
