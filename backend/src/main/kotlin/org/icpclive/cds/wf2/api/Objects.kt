package org.icpclive.cds.wf2.api

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.utils.ClicksTime
import kotlin.time.Duration

@Serializable
data class WF2Contest(
    val start_time: Instant? = null,
    @Serializable(with = ClicksTime.DurationSerializer::class)
    val duration: Duration,
    @Serializable(with = ClicksTime.DurationSerializer::class)
    val scoreboard_freeze_duration: Duration?
)

@Serializable
data class WF2Problem(
    val ordinal: Int,
    val label: String,
    val name: String,
    val rgb: String?,
    val test_data_count: Int? = null
)

@Serializable
data class WF2Media(
    val href: String
)

@Serializable
data class WF2Organisation(
    val id: String,
    val name: String,
    val formal_name: String? = null,
    val logo: List<WF2Media>,
    val twitter_hashtag: String? = null
)

@Serializable
data class WF2Team(
    val id: String,
    val organization_id: String? = null,
    val name: String,
    val photo: List<WF2Media> = emptyList(),
    val video: List<WF2Media> = emptyList(),
    val desktop: List<WF2Media> = emptyList(),
    val webcam: List<WF2Media> = emptyList(),
)
