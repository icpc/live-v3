package org.icpclive.cds.plugins.clics

import kotlinx.datetime.Instant
import org.icpclive.cds.api.*
import org.icpclive.cds.plugins.clics.model.ClicsJudgementTypeInfo
import org.icpclive.cds.plugins.clics.model.ClicsOrganisationInfo
import org.icpclive.clics.objects.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class ClicsModel {
    private val judgementTypes = mutableMapOf<String, ClicsJudgementTypeInfo>()
    private val problems = mutableMapOf<String, Problem>()
    private val organisations = mutableMapOf<String, ClicsOrganisationInfo>()
    private val teams = mutableMapOf<String, Team>()
    private val submissions = mutableMapOf<String, Submission>()
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

    fun getAllRuns() = submissions.values.map { it.toApi() }

    private fun mediaType(file: File?): MediaType? {
        val mime = file?.mime ?: return null
        val href = file.href?.value ?: return null
        return when {
            mime.startsWith("image") -> MediaType.Image(href)
            mime.startsWith("video/m2ts") -> MediaType.M2tsVideo(href)
            mime.startsWith("application/vnd.apple.mpegurl") -> MediaType.HLSVideo(href)
            mime.startsWith("video") -> MediaType.Video(href)
            else -> null
        }
    }

    private fun Group.toApi(): GroupInfo = GroupInfo(id.toGroupId(), name!!, isHidden = false, isOutOfContest = false)

    private fun Team.toApi(): TeamInfo {
        val teamOrganization = organizationId?.let { organisations[it] }
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
                teamOrganization?.country?.let { add(it.toGroupId()) }
            },
            hashTag = teamOrganization?.hashtag,
            medias = buildMap {
                mediaType(photo.firstOrNull())?.let { put(TeamMediaType.PHOTO, it) }
                mediaType(video.firstOrNull())?.let { put(TeamMediaType.RECORD, it) }
                mediaType(webcam.firstOrNull())?.let { put(TeamMediaType.CAMERA, it) }
                mediaType(desktop.firstOrNull())?.let { put(TeamMediaType.SCREEN, it) }
            },
            organizationId = organizationId?.toOrganizationId(),
            isOutOfContest = false,
            customFields = buildMap {
                put("clicsTeamFullName", name)
                put("clicsTeamDisplayName", displayName ?: name)
                label?.let { put("clicsTeamLabel", it) }
            }
        )
    }

    private fun Submission.toApi(): RunInfo {
        val judgment = submissionJudgmentIds[id]?.mapNotNull { judgements[it] }?.maxByOrNull { it.startContestTime }
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
        )
    }

    private fun Problem.toApi() = ProblemInfo(
        id = id.toProblemId(),
        displayName = label,
        fullName = name,
        ordinal = ordinal,
        color = rgb?.let { Color.normalize(it) }
    )

    private fun ClicsOrganisationInfo.toApi() = OrganizationInfo(
        id = id.toOrganizationId(),
        displayName = name,
        fullName = formalName,
        logo = logo
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
            organizationList = organisations.values.map { it.toApi() },
            cdsSupportsFinalization = true
        )

    fun processContest(contest: Contest) {
        name = contest.formalName ?: ""
        startTime = contest.startTime
        contestLength = contest.duration
        freezeTime = contestLength - (contest.scoreboardFreezeDuration ?: Duration.ZERO)
        holdBeforeStartTime = contest.countdownPauseTime
        penaltyPerWrongAttempt = (contest.penaltyTime ?: 20).minutes
        if (status is ContestStatus.BEFORE) {
            status = ContestStatus.BEFORE(
                scheduledStartAt = startTime,
                holdTime = holdBeforeStartTime
            )
        }
    }

    fun processProblem(id: String, problem: Problem?) {
        if (problem == null) {
            problems.remove(id)
        } else {
            require(id == problem.id)
            problems[problem.id] = problem
        }
    }

    fun processOrganization(id: String, organization: Organization?) {
        if (organization == null) {
            organisations.remove(id)
        } else {
            require(id == organization.id)
            organisations[organization.id] = ClicsOrganisationInfo(
                id = organization.id,
                name = organization.name!!,
                formalName = organization.formalName ?: organization.name!!,
                logo = mediaType(organization.logo?.lastOrNull()),
                hashtag = organization.twitterHashtag,
                country = organization.country
            )
        }
    }

    fun processTeam(id: String, team: Team?) {
        if (team == null) {
            teams.remove(id)
        } else {
            require(id == team.id)
            teams[id] = team
        }
    }

    fun processJudgementType(id: String, judgementType: JudgementType?) {
        if (judgementType == null) {
            judgementTypes.remove(id)
        } else {
            require(id == judgementType.id)
            judgementTypes[judgementType.id] = ClicsJudgementTypeInfo(
                id = judgementType.id,
                isAccepted = judgementType.solved,
                isAddingPenalty = judgementType.penalty,
            )
        }
    }

    fun processGroup(id: String, group: Group?) {
        if (group == null) {
            groups.remove(id)
        } else {
            require(id == group.id)
            groups[id] = group
        }
    }

    fun processSubmission(submission: Submission): RunInfo {
        submissions[submission.id] = submission
        return submission.toApi()
    }

    fun processJudgement(id: String, judgement: Judgement?): RunInfo? {
        val oldJudgment = judgements[id]
        if (judgement == oldJudgment) return null
        val submissionId = (judgement ?: oldJudgment)!!.submissionId
        if (judgement != null && oldJudgment != null) require(judgement.submissionId == oldJudgment.submissionId) { "Judgment ${judgement.id} submission id changed from ${oldJudgment.submissionId} to ${judgement.submissionId}" }
        val submission = submissions[submissionId]
        if (judgement == null) {
            judgements.remove(id)
            submissionJudgmentIds[submissionId]?.remove(id)
        } else {
            judgements[judgement.id] = judgement
            submissionJudgmentIds.getOrPut(submissionId) { mutableSetOf() }.add(judgement.id)
        }
        return submission?.toApi()
    }

    fun processRun(id: String, run: Run?): RunInfo? {
        val oldRun = runs[id]
        if (oldRun == run) {
            return null
        }
        val judgementId = (run ?: oldRun)!!.judgementId
        if (oldRun != null && run != null) require(run.judgementId == oldRun.judgementId) { "Run $id judgment id changed from ${oldRun.id} to ${run.id}" }
        val judgement = judgements[judgementId]
        val submission = submissions[judgement?.submissionId]
        if (run == null) {
            judgmentRunIds[judgementId]?.remove(id)
            runs.remove(id)
        } else {
            runs[id] = run
            judgmentRunIds.getOrPut(judgementId) { mutableSetOf() }.add(id)
        }
        return submission?.toApi()
    }

    fun processCommentary(commentary: Commentary) =
        AnalyticsCommentaryEvent(
            commentary.id,
            commentary.message,
            commentary.time,
            commentary.contestTime,
            commentary.teamIds?.map { it.toTeamId() } ?: emptyList(),
            commentary.submissionIds?.map { it.toRunId() } ?: emptyList(),
        )


    fun processState(state: State) {
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
    }

}
