@file:Suppress("UNUSED")
@file:UseContextualSerialization(Media::class)
package org.icpclive.clics.v202207

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.clics.ClicsTime
import org.icpclive.util.ColorSerializer
import org.icpclive.util.DurationInMinutesSerializer
import java.awt.Color
import kotlin.time.Duration

@Serializable
public data class ApiInfo(
    val version: String,
    val versionUrl: String,
    val name: String? = null,
    val logo: List<Media> = emptyList(),
)

@Serializable
public data class Endpoint(
    val type: String,
    val properties: List<String>
)

@Serializable
public data class Access(
    val capabilities: List<String>,
    val endpoints: List<Endpoint>
)

@Serializable
public data class Contest(
    val id: String,
    val name: String? = null,
    val formal_name: String? = null,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val start_time: Instant? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val countdown_pause_time: Duration? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val duration: Duration,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val scoreboard_freeze_duration: Duration?,
    val scoreboard_type: String? = null,
    @Serializable(with = DurationInMinutesSerializer::class)
    val penalty_time: Duration? = null,
    val banner: List<Media> = emptyList(),
    val logo: List<Media> = emptyList(),
)

@Serializable
public data class JudgementType(
    val id: String,
    val name: String,
    val penalty: Boolean,
    val solved: Boolean,
)

@Serializable
public data class Language(
    val id: String,
    val name: String? = null,
    val entry_point_required: Boolean? = null,
    val extensions: List<String>? = null,
)


@Serializable
public data class Scoreboard(
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val time: Instant,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration,
    val state: State,
    val rows: List<ScoreboardRow>
)

@Serializable
public data class ScoreboardRow(
    val rank: Int,
    val team_id: String,
    val score: ScoreboardRowScore,
    val problems: List<ScoreboardRowProblem>
)

@Serializable
public data class ScoreboardRowScore(
    val num_solved: Int,
    val total_time: Long
)

@Serializable
public data class ScoreboardRowProblem(
    val problem_id: String,
    val num_judged: Int,
    val num_pending: Int,
    val solved: Boolean,
    val time: Long? = null,
)

@Serializable
public data class Problem(
    val id: String,
    val ordinal: Int = 0,
    val label: String = "",
    val name: String = "",
    @Serializable(ColorSerializer::class)
    val rgb: Color? = null,
    val test_data_count: Int? = null
)

@Serializable
public data class Media(
    val mime: String = "", // not by spec
    val href: String,
    val fileName: String? = null,
    val hash: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
public data class Organization(
    val id: String,
    val name: String = "",
    val formal_name: String? = null,
    val country: String? = null,
    val country_flag: List<Media> = emptyList(),
    val logo: List<Media> = emptyList(),
    val twitter_hashtag: String? = null
)

@Serializable
public data class Team(
    val id: String,
    val organization_id: String? = null,
    val group_ids: List<String> = emptyList(),
    val name: String = "",
    val display_name: String? = null,
    val hidden: Boolean = false,
    val photo: List<Media> = emptyList(),
    val video: List<Media> = emptyList(),
    val desktop: List<Media> = emptyList(),
    val webcam: List<Media> = emptyList(),
)

@Serializable
public data class Group(
    val id: String,
    val icpcId: String? = null,
    val name: String = "",
    val type: String? = null,
)

@Serializable
public data class Submission(
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
public data class Judgement(
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
public data class Run(
    val id: String,
    val judgement_id: String,
    val ordinal: Int,
    val judgement_type_id: String,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration,
)

@Serializable
public data class State(
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
public data class Commentary(
    val id: String,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val time: Instant,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration,
    val message: String,
    val tags: List<String>,
    val team_ids: List<String>?,
    val problem_ids: List<String>?,
    val submission_ids: List<String>?,
)

@Serializable
public data class Award(
    val id: String,
    val citation: String,
    val team_ids: List<String>
)

@Serializable
public data class Person(
    val id: String,
    val name: String,
    val role: String
)

@Serializable
public data class Account(
    val id: String,
    val username: String,
    val password: String?,
    val type: TYPE?,
    val ip: String?,
    val team_id: String?,
    val person_id: String?
) {
    @Serializable
    public enum class TYPE {
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

@Serializable
public data class Clarification(
    val id: String,
)
