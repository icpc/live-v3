package org.icpclive.cds.plugins.clics

import org.icpclive.cds.api.*
import org.icpclive.cds.plugins.clics.model.ClicsJudgementTypeInfo
import org.icpclive.cds.plugins.clics.model.ClicsOrganizationInfo
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

    private val judgementTypes = mutableMapOf<String, ClicsJudgementTypeInfo>()
    private val problems = mutableMapOf<String, Problem>()
    private val languages = mutableMapOf<String, Language>()
    private val organizations = mutableMapOf<String, ClicsOrganizationInfo>()
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
        val info = commentaries[id]!!.toApi()
        for (listener in commentaryMessageListeners) {
            listener(info)
        }
    }

    private fun mediaType(file: File): MediaType? {
        val mime = file.mime ?: return null
        val href = file.href?.value ?: return null
        return when {
            mime.startsWith("image") -> MediaType.Image(href)
            mime.startsWith("audio") -> MediaType.Audio(href)
            mime.startsWith("video/m2ts") -> MediaType.M2tsVideo(href)
            mime.startsWith("application/vnd.apple.mpegurl") -> MediaType.HLSVideo(href)
            mime.startsWith("video") -> MediaType.Video(href)
            mime.startsWith("text") -> MediaType.Text(href)
            mime.startsWith("application/zip") -> MediaType.ZipArchive(href)
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
            hashTag = teamOrganization?.hashtag,
            medias = buildMap {
                put(TeamMediaType.PHOTO, photo.mapNotNull { mediaType(it) })
                put(TeamMediaType.RECORD, video.mapNotNull { mediaType(it) })
                put(TeamMediaType.CAMERA, webcam.mapNotNull { mediaType(it) })
                put(TeamMediaType.SCREEN, desktop.mapNotNull { mediaType(it) })
                put(TeamMediaType.AUDIO, audio.mapNotNull { mediaType(it) })
                put(TeamMediaType.BACKUP, backup.mapNotNull { mediaType(it) })
                put(TeamMediaType.KEYLOG, keyLog.mapNotNull { mediaType(it) })
                put(TeamMediaType.TOOL_DATA, toolData.mapNotNull { mediaType(it) })
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
        val judgementType = judgementTypes[judgment?.judgementTypeId]
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
                    isAccepted = judgementType.isAccepted,
                    isAddingPenalty = judgementType.isAddingPenalty,
                ).toICPCRunResult()
            },
            problemId = problemId.toProblemId(),
            teamId = teamId.toTeamId(),
            time = contestTime,
            testedTime = judgment?.endContestTime,
            reactionVideos = reaction?.mapNotNull { mediaType(it) } ?: emptyList(),
            languageId = languageId?.toLanguageId(),
            isHidden = id in removedSubmissionIds,
            sourceFiles = files.mapNotNull { mediaType(it) }
        )
    }

    private fun Problem.toApi() = ProblemInfo(
        id = id.toProblemId(),
        displayName = label,
        fullName = name,
        ordinal = ordinal,
        color = rgb?.let { Color.normalize(it) }
    )

    private fun ClicsOrganizationInfo.toApi() = OrganizationInfo(
        id = id.toOrganizationId(),
        displayName = name,
        fullName = formalName,
        logo = logo
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
        if (language == null) {
            languages.remove(id)
        } else {
            require(id == language.id) {
                "Mismatch of id in event and language object: in event = ${id}, in object = ${language.id}"
            }
            languages[language.id] = language
        }
        contestInfoUpdated()
    }

    private suspend fun processProblem(id: String, problem: Problem?) {
        if (problem == null) {
            problems.remove(id)
        } else {
            require(id == problem.id) {
                "Mismatch of id in event and problem object: in event = ${id}, in object = ${problem.id}"
            }
            problems[problem.id] = problem
        }
        contestInfoUpdated()
    }

    private suspend fun processOrganization(id: String, organization: Organization?) {
        if (organization == null) {
            organizations.remove(id)
        } else {
            require(id == organization.id) {
                "Mismatch of id in event and organization object: in event = ${id}, in object = ${organization.id}"
            }
            organizations[organization.id] = ClicsOrganizationInfo(
                id = organization.id,
                name = organization.name!!,
                formalName = organization.formalName ?: organization.name!!,
                logo = organization.logo?.mapNotNull { mediaType(it) }.orEmpty(),
                hashtag = organization.twitterHashtag,
                country = organization.country
            )
        }
        contestInfoUpdated()
    }

    private suspend fun processTeam(id: String, team: Team?) {
        if (team == null) {
            teams.remove(id)
        } else {
            require(id == team.id) {
                "Mismatch of id in event and team object: in event = ${id}, in object = ${team.id}"
            }
            teams[id] = team
        }
        contestInfoUpdated()
    }

    private suspend fun processJudgementType(id: String, judgementType: JudgementType?) {
        if (judgementType == null) {
            judgementTypes.remove(id)
        } else {
            require(id == judgementType.id) {
                "Mismatch of id in event and judgemnet type object: in event = ${id}, in object = ${judgementType.id}"
            }
            judgementTypes[judgementType.id] = ClicsJudgementTypeInfo(
                id = judgementType.id,
                isAccepted = judgementType.solved,
                isAddingPenalty = judgementType.penalty,
            )
        }
        for ((_, submission) in submissions) {
            if (submissionJudgmentIds[submission.id]?.any { judgements[it]?.judgementTypeId == id } == true) {
                submissionUpdated(submission.id)
            }
        }
    }

    private suspend fun processGroup(id: String, group: Group?) {
        if (group == null) {
            groups.remove(id)
        } else {
            require(id == group.id) {
                "Mismatch of id in event and group object: in event = ${id}, in object = ${group.id}"
            }
            groups[id] = group
        }
        contestInfoUpdated()
    }

    private suspend fun processSubmission(id: String, submission: Submission?) {
        if (submission == null) {
            removedSubmissionIds.add(id)
        } else {
            require(id == submission.id) {
                "Mismatch of id in event and submission object: in event = ${id}, in object = ${submission.id}"
            }
            submissions[id] = submission
            removedSubmissionIds.remove(id)
        }
        submissionUpdated(id)
    }

    private suspend fun processJudgement(id: String, judgement: Judgement?) {
        val oldJudgment = judgements[id]
        if (judgement == oldJudgment) return
        oldJudgment?.submissionId?.takeIf { it != judgement?.submissionId }?.let {
            submissionJudgmentIds[it]?.remove(id)
            submissionUpdated(it)
        }
        if (judgement == null) {
            judgements.remove(id)
        } else {
            require(id == judgement.id) {
                "Mismatch of id in event and judgement object: in event = ${id}, in object = ${judgement.id}"
            }
            val submissionId = judgement.submissionId
            judgements[judgement.id] = judgement
            submissionJudgmentIds.getOrPut(submissionId) { mutableSetOf() }.add(judgement.id)
            submissionUpdated(judgement.submissionId)
        }
    }

    private suspend fun processRun(id: String, run: Run?) {
        val oldRun = runs[id]
        if (oldRun == run) return
        val judgementId = (run ?: oldRun)!!.judgementId
        oldRun?.judgementId?.takeIf { it != run?.judgementId }?.let {
            judgmentRunIds[it]?.remove(id)
            submissionUpdated(judgements[it]?.submissionId)
        }
        if (run == null) {
            runs.remove(id)
        } else {
            val judgement = judgements[judgementId]
            require(id == run.id) {
                "Mismatch of id in event and run object: in event = ${id}, in object = ${run.id}"
            }
            runs[id] = run
            judgmentRunIds.getOrPut(judgementId) { mutableSetOf() }.add(id)
            submissionUpdated(judgement?.submissionId)
        }
    }

    private suspend fun processCommentary(id: String, commentary: Commentary?) {
        if (commentary == null) {
            commentaries.remove(id)
        } else {
            commentaries[id] = commentary
        }
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
