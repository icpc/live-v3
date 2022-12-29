package org.icpclive.cds.clics.api

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.clics.ClicsTime
import org.icpclive.util.ColorSerializer
import java.awt.Color
import kotlin.time.Duration

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
    val start_time: Instant? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val duration: Duration,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val scoreboard_freeze_duration: Duration?,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val countdown_pause_time: Duration? = null,
    val penalty_time: Int? = null
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
    val logo: List<Media> = emptyList(),
    val twitter_hashtag: String? = null
)

@Serializable
data class Team(
    val id: String,
    val organization_id: String? = null,
    val group_ids: List<String> = emptyList(),
    val name: String = "",
    val is_hidden: Boolean = false,
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
    val solved: Boolean = false,
    val penalty: Boolean = false
)

@Serializable
data class Submission(
    val id: String,
    val language_id: String,
    val problem_id: String,
    val team_id: String,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration,
    val reaction: List<Media>? = null,
)

@Serializable
data class Judgement(
    val id: String,
    val submission_id: String,
    val judgement_type_id: String?,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val start_contest_time: Duration,
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
data class Language(val id: String)

@Serializable
data class Award(val id: String)

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
