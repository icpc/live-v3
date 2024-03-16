package org.icpclive.cds.plugins.cms

import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ksp.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.plugins.cms.model.*
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.util.Enumerator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds

@Builder("cms")
public sealed interface CmsSettings : CDSSettings {
    public val url: String
    public val activeContest: String
    public val otherContests: List<String>
    override fun toDataSource(): ContestDataSource = CmsDataSource(this)
}

internal class CmsDataSource(val settings: CmsSettings) : FullReloadContestDataSource(5.seconds) {
    private val contestsLoader = jsonUrlLoader<Map<String, Contest>>(settings.network, null) { "${settings.url}/contests/" }
    private val tasksLoader = jsonUrlLoader<Map<String, Task>>(settings.network, null) { "${settings.url}/tasks/" }
    private val teamsLoader = jsonUrlLoader<Map<String, Team>>(settings.network, null) { "${settings.url}/teams/" }
    private val usersLoader = jsonUrlLoader<Map<String, User>>(settings.network, null) { "${settings.url}/users/" }
    private val submissionsLoader = jsonUrlLoader<Map<String, Submission>>(settings.network, null) { "${settings.url}/submissions/" }
    private val subchangesLoader = jsonUrlLoader<Map<String, Subchange>>(settings.network, null) { "${settings.url}/subchanges/" }
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
                        id = ProblemId(k),
                        displayName = v.short_name,
                        fullName = v.name,
                        ordinal = 0,
                        minScore = 0.0,
                        maxScore = v.max_score,
                        scoreMergeMode = when (v.score_mode) {
                            ScoreMode.max -> ScoreMergeMode.MAX_TOTAL
                            ScoreMode.max_subtask -> ScoreMergeMode.MAX_PER_GROUP
                        }
                    )
                }
            }
            for (other in settings.otherContests) {
                for (p in problems[other] ?: emptyList()) {
                    add(p.copy(ordinal = size))
                    finishedContestsProblems.add(p.id.value)
                }
            }
            for (p in problems[settings.activeContest] ?: emptyList()) {
                add(p.copy(ordinal = size))
                runningContestProblems.add(p.id.value)
            }
        }
        val organizations = teamsLoader.load().map { (k, v) ->
            OrganizationInfo(
                id = OrganizationId(k),
                displayName = v.name,
                fullName = v.name,
                logo = MediaType.Photo(settings.url)
            )
        }
        val teams = usersLoader.load().map { (k, v) ->
            TeamInfo(
                id = teamId[k],
                fullName = "[${v.team}] ${v.f_name} ${v.l_name}",
                displayName = "${v.f_name} ${v.l_name}",
                contestSystemId = k,
                groups = emptyList(),
                hashTag = null,
                medias = mapOf(
                    TeamMediaType.PHOTO to MediaType.Photo("${settings.url}/faces/$k", true)
                ),
                isHidden = false,
                isOutOfContest = false,
                organizationId = OrganizationId(v.team),
                customFields = mapOf(
                    "country" to v.team,
                    "first_name" to v.f_name,
                    "last_name" to v.l_name
                ),
            )
        }
        val info = ContestInfo(
            name = mainContest.name,
            status = ContestStatus.byCurrentTime(mainContest.begin, mainContest.end - mainContest.begin),
            resultType = ContestResultType.IOI,
            startTime = mainContest.begin,
            contestLength = mainContest.end - mainContest.begin,
            freezeTime = INFINITE,
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
                result = RunResult.InProgress(0.0),
                problemId = ProblemId(v.task),
                teamId = teamId[v.user],
                time = if (v.task in runningContestProblems) v.time - mainContest.begin else Duration.ZERO
            )
        }.associateBy { it.id }.toMutableMap()
        subchangesLoader.load().entries.sortedBy { it.value.time }.forEach { (_, it) ->
            val r = submissions[submissionId[it.submission]] ?: return@forEach
            val scores = if (it.extra.isEmpty()) listOf(it.score) else it.extra.map { it.toDouble() }
            submissions[r.id] = r.copy(result = RunResult.IOI(scores))
        }
        return ContestParseResult(info, submissions.values.sortedBy { it.id }, emptyList())
    }
}