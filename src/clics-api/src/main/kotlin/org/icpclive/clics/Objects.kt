@file:Suppress("UNUSED")
@file:UseContextualSerialization(Media::class)
package org.icpclive.clics

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.util.ColorSerializer
import org.icpclive.util.DurationInMinutesSerializer
import java.awt.Color
import kotlin.time.Duration

@Serializable
data class ApiProvider(
    val name: String,
    val version: String? = null,
    val logo: List<Media> = emptyList(),
)

@Serializable
data class ApiInfo(
    val version: String,
    val versionUrl: String,
    val provider: ApiProvider
)

@Serializable
data class Endpoint(
    val type: String,
    val properties: List<String>
)

@Serializable
data class Access(
    val capabilities: List<String>,
    val endpoints: List<Endpoint>
)

@Serializable
data class Scoreboard(
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val time: Instant,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration,
    val state: State,
    val rows: List<ScoreboardRow>
)

@Serializable
data class ScoreboardRow(
    val rank: Int,
    val team_id: String,
    val score: ScoreboardRowScore,
    val problems: List<ScoreboardRowProblem>
)

@Serializable
data class ScoreboardRowScore(
    val num_solved: Int,
    val total_time: Long
)

@Serializable
data class ScoreboardRowProblem(
    val problem_id: String,
    val num_judged: Int,
    val num_pending: Int,
    val solved: Boolean,
    val time: Long? = null,
)

@Serializable
enum class Operation {
    @SerialName("create")
    CREATE,

    @SerialName("update")
    UPDATE,

    @SerialName("delete")
    DELETE
}

@Serializable
data class Contest(
    val id: String,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val start_time: Instant? = null,
    val name: String? = null,
    val formal_name: String? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val duration: Duration,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val scoreboard_freeze_duration: Duration?,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val countdown_pause_time: Duration? = null,
    @Serializable(with = DurationInMinutesSerializer::class)
    val penalty_time: Duration? = null,
    val scoreboard_type: String? = null
)

@Serializable
data class Problem(
    val id: String,
    val ordinal: Int = 0,
    val label: String = "",
    val name: String = "",
    @Serializable(ColorSerializer::class)
    val rgb: Color? = null,
    val test_data_count: Int? = null
)

@Serializable
data class Media(
    val mime: String,
    val href: String,
)

@Serializable
data class Organization(
    val id: String,
    val name: String = "",
    val formal_name: String? = null,
    val country_flag: List<Media> = emptyList(),
    val logo: List<Media> = emptyList(),
    val twitter_hashtag: String? = null
)

@Serializable
data class Team(
    val id: String,
    val organization_id: String? = null,
    val group_ids: List<String> = emptyList(),
    val name: String = "",
    val hidden: Boolean = false,
    val photo: List<Media> = emptyList(),
    val video: List<Media> = emptyList(),
    val desktop: List<Media> = emptyList(),
    val webcam: List<Media> = emptyList(),
)

@Serializable
data class Group(
    val id: String,
    val icpcId: String? = null,
    val name: String = "",
    val type: String? = null,
)

@Serializable
data class JudgementType(
    val id: String,
    val name: String,
    val solved: Boolean = false,
    val penalty: Boolean = false
)

@Serializable
data class Submission(
    val id: String,
    val language_id: String,
    val problem_id: String,
    val team_id: String,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val time: Instant,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration,
    val reaction: List<Media>? = null,
)

@Serializable
data class Judgement(
    val id: String,
    val submission_id: String,
    val judgement_type_id: String?,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val start_time: Instant,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val start_contest_time: Duration,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val end_time: Instant?,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val end_contest_time: Duration?,
)

@Serializable
data class Run(
    val id: String,
    val judgement_id: String,
    val ordinal: Int,
    val judgement_type_id: String,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration,
)

@Serializable
data class State(
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val ended: Instant?,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val frozen: Instant?,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val thawed: Instant?,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val started: Instant?,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val unfrozen: Instant?,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val finalized: Instant?,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val end_of_updates: Instant?,
)

@Serializable
data class Commentary(
    val id: String,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val time: Instant,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration,
    val message: String,
    val team_ids: List<String>?,
    val problem_ids: List<String>?,
    val submission_ids: List<String>?,
)

@Serializable
data class Clarification(val id: String)

@Serializable
data class Language(
    val id: String,
    val name: String? = null,
    val entry_point_required: Boolean? = null,
    val extensions: List<String>? = null,
)

@Serializable
data class Award(
    val id: String,
    val citation: String,
    val team_ids: List<String>
)

@Serializable
data class Person(
    val id: String,
    val name: String,
    val role: String
)

@Serializable
data class Account(
    val id: String,
    val username: String,
    val password: String?,
    val type: TYPE?,
    val ip: String?,
    val team_id: String?,
    val person_id: String?
) {
    @Serializable
    enum class TYPE {
        @SerialName("team")
        TEAM,

        @SerialName("judge")
        JUDGE,

        @SerialName("admin")
        ADMIN,

        @SerialName("analyst")
        ANALYST,

        @SerialName("staff")
        STAFF
    }
}
