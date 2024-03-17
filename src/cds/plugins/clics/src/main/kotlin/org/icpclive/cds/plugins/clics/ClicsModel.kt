package org.icpclive.cds.plugins.clics

import kotlinx.datetime.Instant
import org.icpclive.cds.api.*
import org.icpclive.cds.plugins.clics.model.ClicsJudgementTypeInfo
import org.icpclive.cds.plugins.clics.model.ClicsOrganisationInfo
import org.icpclive.clics.v202207.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class ClicsModel(private val addTeamNames: Boolean) {
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
    private var status = ContestStatus.BEFORE
    private var penaltyPerWrongAttempt = 20.minutes
    private var holdBeforeStartTime: Duration? = null
    private var name: String = ""

    fun getAllRuns() = submissions.values.map { it.toApi() }

    private fun teamName(org: String?, name: String) = when {
        org == null -> name
        addTeamNames -> "$org: $name"
        else -> org
    }

    private fun Media.mediaType(): MediaType? {
        if (mime.startsWith("image")) {
            return MediaType.Photo(href)
        }
        if (mime.startsWith("video/m2ts")) {
            return MediaType.M2tsVideo(href)
        }
        if (mime.startsWith("video")) {
            return MediaType.Video(href)
        }
        return null
    }

    private fun Group.toApi(): GroupInfo = GroupInfo(id.toGroupId(), name, isHidden = false, isOutOfContest = false)

    private fun Team.toApi(): TeamInfo {
        val teamOrganization = organization_id?.let { organisations[it] }
        return TeamInfo(
            id = id.toTeamId(),
            fullName = teamName(teamOrganization?.formalName, name),
            displayName = teamName(teamOrganization?.name, name),
            isHidden = hidden,
            groups = buildList {
                for (group in group_ids) {
                    groups[group]?.let {
                        add(it.id.toGroupId())
                    }
                }
                teamOrganization?.country?.let { add(it.toGroupId()) }
            },
            hashTag = teamOrganization?.hashtag,
            medias = buildMap {
                photo.firstOrNull()?.mediaType()?.let { put(TeamMediaType.PHOTO, it) }
                video.firstOrNull()?.mediaType()?.let { put(TeamMediaType.RECORD, it) }
                webcam.firstOrNull()?.mediaType()?.let { put(TeamMediaType.CAMERA, it) }
                desktop.firstOrNull()?.mediaType()?.let { put(TeamMediaType.SCREEN, it) }
            },
            organizationId = organization_id?.toOrganizationId(),
            isOutOfContest = false,
            customFields = mapOf(
                "name" to name,
            )
        )
    }

    private fun Submission.toApi(): RunInfo {
        val judgment = submissionJudgmentIds[id]?.mapNotNull { judgements[it] }?.maxByOrNull { it.start_contest_time }
        val problem = problems[problem_id]
        val passedTests = judgment?.id?.let { judgmentRunIds[it] }?.size ?: 0
        val judgementType = judgementTypes[judgment?.judgement_type_id]
        return RunInfo(
            id = id.toRunId(),
            result = if (judgementType == null) {
                val part = when (val count = problem?.test_data_count) {
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
            problemId = problem_id.toProblemId(),
            teamId = team_id.toTeamId(),
            time = contest_time,
            reactionVideos = reaction?.mapNotNull { it.mediaType() } ?: emptyList(),
        )
    }

    private fun Problem.toApi() = ProblemInfo(
        id = id.toProblemId(),
        displayName = label,
        fullName = name,
        ordinal = ordinal,
        color = rgb
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
            startTime = startTime ?: Instant.fromEpochSeconds(0),
            contestLength = contestLength,
            freezeTime = freezeTime,
            problemList = problems.values.map { it.toApi() },
            teamList = teams.values.map { it.toApi() },
            groupList = groups.values.map { it.toApi() },
            penaltyPerWrongAttempt = penaltyPerWrongAttempt,
            holdBeforeStartTime = holdBeforeStartTime,
            penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
            organizationList = organisations.values.map { it.toApi() },
            cdsSupportsFinalization = true
        )

    fun processContest(contest: Contest) {
        name = contest.formal_name ?: ""
        startTime = contest.start_time
        contestLength = contest.duration
        freezeTime = contestLength - (contest.scoreboard_freeze_duration ?: Duration.ZERO)
        holdBeforeStartTime = contest.countdown_pause_time
        penaltyPerWrongAttempt = contest.penalty_time ?: 20.minutes
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
                name = organization.name,
                formalName = organization.formal_name ?: organization.name,
                logo = organization.logo.lastOrNull()?.mediaType(),
                hashtag = organization.twitter_hashtag,
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
        val submissionId = (judgement ?: oldJudgment)!!.submission_id
        if (judgement != null && oldJudgment != null) require(judgement.submission_id == oldJudgment.submission_id) { "Judgment ${judgement.id} submission id changed from ${oldJudgment.submission_id} to ${judgement.submission_id}" }
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
        val judgementId = (run ?: oldRun)!!.judgement_id
        if (oldRun != null && run != null) require(run.judgement_id == oldRun.judgement_id) { "Run $id judgment id changed from ${oldRun.id} to ${run.id}" }
        val judgement = judgements[judgementId]
        val submission = submissions[judgement?.submission_id]
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
            commentary.contest_time,
            commentary.team_ids?.map { it.toTeamId() } ?: emptyList(),
            commentary.submission_ids?.map { it.toRunId() } ?: emptyList(),
        )


    fun processState(state: State) {
        status = when {
            state.end_of_updates != null -> ContestStatus.FINALIZED
            state.ended != null -> ContestStatus.OVER
            state.started != null -> ContestStatus.RUNNING
            else -> ContestStatus.BEFORE
        }
    }

}
