package org.icpclive.cds.cms

import org.icpclive.api.*
import org.icpclive.cds.cms.model.*
import org.icpclive.cds.common.ContestParseResult
import org.icpclive.cds.common.FullReloadContestDataSource
import org.icpclive.cds.common.jsonLoader
import org.icpclive.cds.settings.CmsSettings
import org.icpclive.util.Enumerator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class CmsDataSource(val settings: CmsSettings) : FullReloadContestDataSource(5.seconds) {
    private val contestsLoader = jsonLoader<Map<String, Contest>>(settings.network, null) { "${settings.url}/contests/" }
    private val tasksLoader = jsonLoader<Map<String, Task>>(settings.network, null) { "${settings.url}/tasks/"}
    private val teamsLoader = jsonLoader<Map<String, Team>>(settings.network, null) { "${settings.url}/teams/"}
    private val usersLoader = jsonLoader<Map<String, User>>(settings.network, null) { "${settings.url}/users/"}
    private val submissionsLoader = jsonLoader<Map<String, Submission>>(settings.network, null) { "${settings.url}/submissions/"}
    private val subchangesLoader = jsonLoader<Map<String, Subchange>>(settings.network, null) { "${settings.url}/subchanges/"}
    private val problemId = Enumerator<String>()
    private val teamId = Enumerator<String>()
    private val submissionId = Enumerator<String>()

    override suspend fun loadOnce(): ContestParseResult {
        val contests = contestsLoader.load()
        val mainContest = contests[settings.activeContest] ?: error("No data for contest ${settings.activeContest}")
        val finishedContestsProblems = mutableSetOf<String>()
        val runningContestProblems = mutableSetOf<String>()
        val problems = buildList {
            val problems = tasksLoader.load().entries.groupBy { it.value.contest }.mapValues {
                it.value.map { (k, v) ->
                    ProblemInfo(
                        displayName = v.short_name,
                        fullName = v.name,
                        id = problemId[k],
                        contestSystemId = k,
                        ordinal = 0,
                        scoreMergeMode = when (v.score_mode) {
                            ScoreMode.max -> ScoreMergeMode.MAX_TOTAL
                            ScoreMode.max_subtask -> ScoreMergeMode.MAX_PER_GROUP
                        },
                        minScore = 0.0,
                        maxScore = v.max_score
                    )
                }
            }
            for (other in settings.otherContests) {
                for (p in problems[other] ?: emptyList()) {
                    add(p.copy(ordinal = size))
                    finishedContestsProblems.add(p.contestSystemId)
                }
            }
            for (p in problems[settings.activeContest] ?: emptyList()) {
                add(p.copy(ordinal = size))
                runningContestProblems.add(p.contestSystemId)
            }
        }
        val organizations = teamsLoader.load().map { (k, v) ->
            OrganizationInfo(
                cdsId = k,
                displayName = v.name,
                fullName = v.name
            )
        }
        val teams = usersLoader.load().map {(k, v) ->
            TeamInfo(
                id = teamId[k],
                fullName = "[${v.team}] ${v.f_name} ${v.l_name}",
                displayName = "${v.f_name} ${v.l_name}",
                contestSystemId = k,
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                isHidden = false,
                isOutOfContest = false,
                organizationId = v.team,
                customFields = mapOf(
                    "country" to v.team,
                    "first_name" to v.f_name,
                    "last_name" to v.l_name
                )
            )
        }
        val info = ContestInfo(
            name = mainContest.name,
            status = ContestStatus.byCurrentTime(mainContest.begin, mainContest.end - mainContest.begin),
            resultType = ContestResultType.IOI,
            startTime = mainContest.begin,
            contestLength = mainContest.end - mainContest.begin,
            freezeTime = mainContest.end - mainContest.begin,
            problemList = problems,
            teamList = teams,
            groupList = emptyList(),
            organizationList = organizations,
            penaltyRoundingMode = PenaltyRoundingMode.ZERO,
        )
        val submissions = submissionsLoader.load().mapNotNull { (k, v) ->
            if (v.task !in runningContestProblems && v.task !in finishedContestsProblems) {
                return@mapNotNull null
            }
            RunInfo(
                id = submissionId[k],
                result = null,
                percentage = 0.0,
                problemId = problemId[v.task],
                teamId = teamId[v.user],
                time = if (v.task in runningContestProblems) v.time - mainContest.begin else Duration.ZERO
            )
        }.associateBy { it.id }.toMutableMap()
        subchangesLoader.load().entries.sortedBy { it.value.time }.forEach {(_, it) ->
            val r = submissions[submissionId[it.submission]] ?: return@forEach
            submissions[r.id] = r.copy(result = IOIRunResult(it.extra.map { it.toDouble() }))
        }
        return ContestParseResult(info, submissions.values.sortedBy { it.id }, emptyList())
    }
}