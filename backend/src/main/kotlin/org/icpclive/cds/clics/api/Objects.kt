package org.icpclive.cds.clics.api

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.utils.ClicsTime
import kotlin.time.Duration

@Serializable
data class Contest(
    val start_time: Instant? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val duration: Duration,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val scoreboard_freeze_duration: Duration?
)

@Serializable
data class Problem(
    val id: String,
    val ordinal: Int,
    val label: String,
    val name: String,
    val rgb: String?,
    val test_data_count: Int? = null
)

@Serializable
data class Media(
    val href: String
)

@Serializable
data class Organisation(
    val id: String,
    val name: String,
    val formal_name: String? = null,
    val logo: List<Media>,
    val twitter_hashtag: String? = null
)

@Serializable
data class Team(
    val id: String,
    val organization_id: String? = null,
    val name: String,
    val photo: List<Media> = emptyList(),
    val video: List<Media> = emptyList(),
    val desktop: List<Media> = emptyList(),
    val webcam: List<Media> = emptyList(),
)

@Serializable
data class Submission(
    val id: String,
    val language_id: String,
    val problem_id: String,
    val team_id: String,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration
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
