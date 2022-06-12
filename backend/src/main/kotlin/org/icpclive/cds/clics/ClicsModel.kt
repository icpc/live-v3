package org.icpclive.cds.clics

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.clics.api.*
import org.icpclive.cds.clics.model.*
import org.icpclive.utils.getLogger
import java.awt.Color
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class ClicsModel {
    private val judgementTypes = mutableMapOf<String, ClicsJudgementTypeInfo>()
    private val problems = mutableMapOf<String, ClicsProblemInfo>()
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

    val Team.internalId get() = id.hashCode()

    fun Team.toApi() : TeamInfo {
        val teamOrganization = organization_id?.let { organisations[it] }
        return TeamInfo(
            id = internalId,
            name = teamOrganization?.formalName ?: name,
            shortName = teamOrganization?.name ?: name,
            contestSystemId = id,
            groups = groupIds.mapNotNull { groups[it]?.name },
            hashTag = teamOrganization?.hashtag,
            medias = buildMap {
                photo.firstOrNull()?.let { put(MediaType.PHOTO, it.href) }
                video.firstOrNull()?.let { put(MediaType.RECORD, it.href) }
                webcam.firstOrNull()?.let { put(MediaType.CAMERA, it.href) }
                desktop.firstOrNull()?.let { put(MediaType.SCREEN, it.href) }
            }
        )
    }

    val contestInfo: ContestInfo
        get() = ContestInfo(
            status = status,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            problems = problems.values.map { it.toApi() },
            teams = teams.values.map { it.toApi() },
        )

    fun processContest(contest: Contest) {
        contest.start_time?.let { startTime = it }
        contestLength = contest.duration
        contest.scoreboard_freeze_duration?.let { freezeTime = contestLength - it }
    }

    fun parseColor(s: String) = when {
        s.startsWith("0x") -> Color.decode(s)
        s.startsWith("#") -> Color.decode("0x" + s.substring(1))
        else -> Color.decode("0x$s")
    }

    fun processProblem(operation: Operation, problem: Problem) {
        if (operation == Operation.DELETE) {
            problems.remove(problem.id)
        } else {
            problems[problem.id] = ClicsProblemInfo(
                id = problem.ordinal - 1, // todo: это не может работать
                letter = problem.label,
                name = problem.name,
                color = problem.rgb?.let {
                    try {
                        parseColor(it)
                    } catch (e: Exception) {
                        logger.warn("Failed to parse color $it, ignoring it")
                        null
                    }
                } ?: Color.GRAY,
                testCount = problem.test_data_count
            )
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
            logger.info("Remove judgementType $judgementType")
        } else {
            judgementTypes[judgementType.id] = ClicsJudgementTypeInfo(
                id = judgementType.id,
                isAccepted = judgementType.solved!!,
                isAddingPenalty = judgementType.penalty,
            )
            logger.info("Add judgementType $judgementType")
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
        if (run.time >= freezeTime) return run // TODO: why we can know it?
        judgement.end_contest_time?.let { run.lastUpdateTime = it.toLong(DurationUnit.MILLISECONDS) }
        judgement.judgement_type_id?.let { run.judgementType = judgementTypes[it] }
        logger.debug("Process $judgement")
        return run
    }

    fun processRun(casesRun: Run): ClicsRunInfo {
        val judgement = judgements[casesRun.judgement_id]
            ?: throw IllegalStateException("Failed to load run with judgment_id ${casesRun.judgement_id}")
        val run = submissions[judgement.submission_id]
            ?: throw IllegalStateException("Failed to load run with judgment_id ${casesRun.judgement_id}, submission_id ${judgement.submission_id}")
        if (run.time >= freezeTime) return run // TODO: why we can know it?
        run.lastUpdateTime = casesRun.contest_time.toLong(DurationUnit.MILLISECONDS)
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

    companion object {
        val logger = getLogger(ClicsModel::class)
    }
}
