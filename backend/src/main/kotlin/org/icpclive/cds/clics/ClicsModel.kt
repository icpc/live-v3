package org.icpclive.cds.clics

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.clics.api.*
import org.icpclive.cds.clics.model.ClicsJudgementTypeInfo
import org.icpclive.cds.clics.model.ClicsOrganisationInfo
import org.icpclive.cds.clics.model.ClicsRunInfo
import org.icpclive.utils.getLogger
import kotlin.time.Duration.Companion.hours

class ClicsModel {
    private val judgementTypes = mutableMapOf<String, ClicsJudgementTypeInfo>()
    private val problems = mutableMapOf<String, Problem>()
    private val organisations = mutableMapOf<String, ClicsOrganisationInfo>()
    private val teams = mutableMapOf<String, Team>()
    private val submissionCdsIdToInt = mutableMapOf<String, Int>()
    val submissions = mutableMapOf<String, ClicsRunInfo>()
    private val judgements = mutableMapOf<String, Judgement>()
    private val groups = mutableMapOf<String, Group>()

    var startTime = Instant.fromEpochMilliseconds(0)
    var contestLength = 5.hours
    var freezeTime = 4.hours
    var status = ContestStatus.BEFORE
    var penaltyPerWrongAttempt = 20

    val Team.internalId get() = externalTeamId(id)

    fun Team.toApi(): TeamInfo {
        val teamOrganization = organization_id?.let { organisations[it] }
        return TeamInfo(
            id = internalId,
            name = teamOrganization?.formalName ?: name,
            shortName = teamOrganization?.name ?: name,
            contestSystemId = id,
            groups = group_ids.mapNotNull { groups[it]?.name },
            hashTag = teamOrganization?.hashtag,
            medias = buildMap {
                photo.firstOrNull()?.let { put(MediaType.PHOTO, it.href) }
                video.firstOrNull()?.let { put(MediaType.RECORD, it.href) }
                webcam.firstOrNull()?.let { put(MediaType.CAMERA, it.href) }
                desktop.firstOrNull()?.let { put(MediaType.SCREEN, it.href) }
            }
        )
    }

    fun Problem.toApi() = ProblemInfo(
        letter = label,
        name = name,
        color = rgb
    )

    val contestInfo: ContestInfo
        get() = ContestInfo(
            status = status,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            problems = problems.values.sortedBy { it.ordinal }.map { it.toApi() },
            teams = teams.values.map { it.toApi() },
            penaltyPerWrongAttempt = penaltyPerWrongAttempt
        )

    fun processContest(contest: Contest) {
        contest.start_time?.let { startTime = it }
        contestLength = contest.duration
        contest.scoreboard_freeze_duration?.let { freezeTime = contestLength - it }
        penaltyPerWrongAttempt = contest.penalty_time ?: 20
    }

    fun processProblem(operation: Operation, problem: Problem) {
        if (operation == Operation.DELETE) {
            problems.remove(problem.id)
        } else {
            problems[problem.id] = problem
        }
    }

    fun processOrganization(operation: Operation, organization: Organization) {
        if (operation == Operation.DELETE) {
            organisations.remove(organization.id)
        } else {
            organisations[organization.id] = ClicsOrganisationInfo(
                id = organization.id,
                name = organization.name,
                formalName = organization.formal_name ?: organization.name,
                logo = organization.logo.lastOrNull()?.href,
                hashtag = organization.twitter_hashtag
            )
        }
        // todo: update team if something changed
    }

    fun processTeam(operation: Operation, team: Team) {
        val id = team.id
        if (operation == Operation.DELETE) {
            teams.remove(id)
        } else {
            teams[id] = team
        }
    }

    fun processJudgementType(operation: Operation, judgementType: JudgementType) {
        if (operation == Operation.DELETE) {
            judgementTypes.remove(judgementType.id)
        } else {
            judgementTypes[judgementType.id] = ClicsJudgementTypeInfo(
                id = judgementType.id,
                isAccepted = judgementType.solved!!,
                isAddingPenalty = judgementType.penalty,
            )
        }
    }

    fun processGroup(operation: Operation, group: Group) {
        val id = group.id
        if (operation == Operation.DELETE) {
            groups.remove(id)
        } else {
            groups[id] = group
        }
    }

    fun processSubmission(submission: Submission): ClicsRunInfo {
        val id = submissionCdsIdToInt.getOrPut(submission.id) { submissionCdsIdToInt.size + 1 }
        val problem = problems[submission.problem_id]
            ?: throw IllegalStateException("Failed to load submission with problem_id ${submission.problem_id}")
        val team = teams[submission.team_id]
            ?: throw IllegalStateException("Failed to load submission with team_id ${submission.team_id}")
        val run = ClicsRunInfo(
            id = id,
            problem = problem,
            teamId = team.internalId,
            submissionTime = submission.contest_time
        )
        submissions[submission.id] = run
        return run
    }

    fun processJudgement(judgement: Judgement): ClicsRunInfo {
        val run = submissions[judgement.submission_id]
            ?: throw IllegalStateException("Failed to load judgment with submission_id ${judgement.submission_id}")
        judgements[judgement.id] = judgement
        if (run.submissionTime >= freezeTime) return run // TODO: why we can know it?
        judgement.judgement_type_id?.let { run.judgementType = judgementTypes[it] }
        logger.debug("Process $judgement")
        return run
    }

    fun processRun(casesRun: Run): ClicsRunInfo {
        val judgement = judgements[casesRun.judgement_id]
            ?: throw IllegalStateException("Failed to load run with judgment_id ${casesRun.judgement_id}")
        val run = submissions[judgement.submission_id]
            ?: throw IllegalStateException("Failed to load run with judgment_id ${casesRun.judgement_id}, submission_id ${judgement.submission_id}")
        if (run.submissionTime >= freezeTime) return run // TODO: why we can know it?
        val judgementType = judgementTypes[casesRun.judgement_type_id]
        if (judgementType?.isAccepted == true) { // may be WA runs also need to add
            run.passedCaseRun.add(casesRun.ordinal)
        }
        logger.debug("$casesRun with verdict $judgementType")
        return run
    }

    fun processState(state: State) {
        status = when {
            state.ended != null -> ContestStatus.OVER
            state.started != null -> ContestStatus.RUNNING
            else -> ContestStatus.BEFORE
        }
    }

    fun externalTeamId(cdsId: String) = cdsId.hashCode()
    fun externalSubmissionId(cdsId: String) =
        submissionCdsIdToInt[cdsId] ?: throw IllegalArgumentException("Failed to get external id of submissions")

    companion object {
        val logger = getLogger(ClicsModel::class)
    }
}
