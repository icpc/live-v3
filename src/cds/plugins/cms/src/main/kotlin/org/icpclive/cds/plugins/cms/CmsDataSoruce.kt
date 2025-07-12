package org.icpclive.cds.plugins.cms

import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.DataLoader
import org.icpclive.cds.ktor.KtorNetworkSettingsProvider
import org.icpclive.cds.plugins.cms.model.*
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.cds.util.getLogger
import org.icpclive.ksp.cds.Builder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Builder("cms")
public sealed interface CmsSettings : CDSSettings, KtorNetworkSettingsProvider {
    public val source: UrlOrLocalPath.Url
    public val activeContest: String
    public val otherContests: List<String>
    override fun toDataSource(): ContestDataSource = CmsDataSource(this)
}

internal class CmsDataSource(val settings: CmsSettings) : FullReloadContestDataSource(5.seconds) {
    private val contestsLoader = DataLoader.json<Map<String, Contest>>(settings.network, settings.source.subDir("contests/"))
    private val tasksLoader = DataLoader.json<Map<String, Task>>(settings.network, settings.source.subDir("/tasks/"))
    private val teamsLoader = DataLoader.json<Map<String, Team>>(settings.network, settings.source.subDir("/teams/"))
    private val usersLoader = DataLoader.json<Map<String, User>>(settings.network, settings.source.subDir("/users/"))
    private val submissionsLoader = DataLoader.json<Map<String, Submission>>(settings.network, settings.source.subDir("/submissions/"))
    private val subchangesLoader = DataLoader.json<Map<String, Subchange>>(settings.network, settings.source.subDir("subchanges/"))

    // cms sometimes, for some reason, don't report some of the old results.
    // Let's cache them ourselves just in case
    private val submissionResults = mutableMapOf<RunId, RunResult>()

    override suspend fun loadOnce(): ContestParseResult {
        val contests = contestsLoader.load()
        val mainContest = contests[settings.activeContest] ?: error("No data for contest ${settings.activeContest}")
        val finishedContestsProblems = mutableSetOf<String>()
        val runningContestProblems = mutableSetOf<String>()
        val problems = buildList {
            val problems = tasksLoader.load().entries.groupBy { it.value.contest }.mapValues {
                it.value.map { (k, v) ->
                    ProblemInfo(
                        id = k.toProblemId(),
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
                id = k.toOrganizationId(),
                displayName = v.name,
                fullName = v.name,
                logo = MediaType.Image(settings.source.toString())
            )
        }
        val teams = usersLoader.load().map { (k, v) ->
            TeamInfo(
                id = k.toTeamId(),
                fullName = "[${v.team}] ${v.f_name} ${v.l_name}",
                displayName = "${v.f_name} ${v.l_name}",
                groups = emptyList(),
                hashTag = null,
                medias = mapOf(
                    TeamMediaType.PHOTO to MediaType.Image("${settings.source}/faces/$k", true)
                ),
                isHidden = false,
                isOutOfContest = false,
                organizationId = v.team.toOrganizationId(),
                customFields = mapOf(
                    "country" to v.team,
                    "first_name" to v.f_name,
                    "last_name" to v.l_name
                ),
            )
        }
        val info = ContestInfo(
            name = mainContest.name,
            resultType = ContestResultType.IOI,
            startTime = mainContest.begin,
            contestLength = mainContest.end - mainContest.begin,
            freezeTime = null,
            problemList = problems,
            teamList = teams,
            groupList = emptyList(),
            organizationList = organizations,
            languagesList = emptyList(),
            penaltyRoundingMode = PenaltyRoundingMode.ZERO
        )

        subchangesLoader.load()
            .values
            .sortedBy { it.time }
            .forEach { it ->
                val scores = if (it.extra.isEmpty()) listOf(it.score) else it.extra.map { it.toDouble() }
                submissionResults[it.submission.toRunId()] = RunResult.IOI(scores)
            }
        val submissions = submissionsLoader.load().mapNotNull { (k, v) ->
            if (v.task !in runningContestProblems && v.task !in finishedContestsProblems) {
                return@mapNotNull null
            }
            RunInfo(
                id = k.toRunId(),
                result = submissionResults[k.toRunId()] ?: RunResult.InProgress(0.0),
                problemId = v.task.toProblemId(),
                teamId = v.user.toTeamId(),
                time = if (v.task in runningContestProblems) v.time - mainContest.begin else Duration.ZERO,
                languageId = null,
            )
        }.associateBy { it.id }.toMutableMap()

        return ContestParseResult(info, submissions.values.sortedBy { it.id.value }, emptyList())
    }

    companion object {
        val logger by getLogger()
    }
}
