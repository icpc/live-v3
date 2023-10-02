@file:Suppress("UNUSED")
@file:UseContextualSerialization(Media::class)

package org.icpclive.clics.v202003

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.clics.ClicsTime
import org.icpclive.util.ColorSerializer
import org.icpclive.util.DurationInMinutesSerializer
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
) {
    fun upgrade() = org.icpclive.clics.v202207.Contest(
        id = id,
        name = name,
        formal_name = formal_name,
        start_time = start_time,
        countdown_pause_time = countdown_pause_time,
        duration = duration,
        scoreboard_freeze_duration = scoreboard_freeze_duration,
        scoreboard_type = scoreboard_type,
        penalty_time = penalty_time
    )
}

@Serializable
data class Problem(
    val id: String,
    val ordinal: Int = 0,
    val label: String = "",
    val name: String = "",
    @Serializable(ColorSerializer::class)
    val rgb: Color? = null,
    val test_data_count: Int? = null
) {
    fun upgrade() = org.icpclive.clics.v202207.Problem(
        id = id,
        ordinal = ordinal,
        label = label,
        name = name,
        rgb = rgb,
        test_data_count = test_data_count
    )
}

@Serializable
data class Media(
    val mime: String,
    val href: String,
) {
    fun upgrade() = org.icpclive.clics.v202207.Media(
        mime = mime,
        href = href
    )
}

@Serializable
data class Organization(
    val id: String,
    val name: String = "",
    val formal_name: String? = null,
    val country_flag: List<Media> = emptyList(),
    val logo: List<Media> = emptyList(),
    val twitter_hashtag: String? = null
) {
    fun upgrade() = org.icpclive.clics.v202207.Organization(
        id = id,
        name = name,
        formal_name = formal_name,
        country_flag = country_flag.map { it.upgrade() },
        logo = logo.map { it.upgrade() },
        twitter_hashtag = twitter_hashtag
    )
}

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
) {
    fun upgrade() = org.icpclive.clics.v202207.Team(
        id = id,
        organization_id = organization_id,
        group_ids = group_ids,
        name = name,
        hidden = hidden,
        photo = photo.map { it.upgrade() },
        video = video.map { it.upgrade() },
        desktop = desktop.map { it.upgrade() },
        webcam = webcam.map { it.upgrade() }
    )
}

@Serializable
data class Group(
    val id: String,
    val icpcId: String? = null,
    val name: String = "",
    val type: String? = null,
) {
    fun upgrade() = org.icpclive.clics.v202207.Group(
        id = id,
        icpcId = icpcId,
        name = name,
        type = type
    )
}

@Serializable
data class JudgementType(
    val id: String,
    val name: String,
    val solved: Boolean = false,
    val penalty: Boolean = false
) {
    fun upgrade() = org.icpclive.clics.v202207.JudgementType(
        id = id,
        name = name,
        solved = solved,
        penalty = penalty
    )
}

@Serializable
data class Submission(
    val id: String,
    val language_id: String? = null,
    val problem_id: String? = null,
    val team_id: String? = null,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val time: Instant? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration? = null,
    val reaction: List<Media>? = null,
) {
    fun upgrade() = org.icpclive.clics.v202207.Submission(
        id = id,
        language_id = language_id!!,
        problem_id = problem_id!!,
        team_id = team_id!!,
        time = time!!,
        contest_time = contest_time!!,
        reaction = reaction?.map { it.upgrade() }
    )
}

@Serializable
data class Judgement(
    val id: String,
    val submission_id: String? = null,
    val judgement_type_id: String? = null,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val start_time: Instant? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val start_contest_time: Duration? = null,
    @Serializable(with = ClicsTime.InstantSerializer::class)
    val end_time: Instant? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val end_contest_time: Duration? = null,
) {
    fun upgrade() = org.icpclive.clics.v202207.Judgement(
        id = id,
        submission_id = submission_id!!,
        judgement_type_id = judgement_type_id,
        start_time = start_time!!,
        start_contest_time = start_contest_time!!,
        end_time = end_time,
        end_contest_time = end_contest_time
    )
}

@Serializable
data class Run(
    val id: String,
    val judgement_id: String? = null,
    val ordinal: Int? = null,
    val judgement_type_id: String? = null,
    @Serializable(with = ClicsTime.DurationSerializer::class)
    val contest_time: Duration? = null,
) {
    fun upgrade() = org.icpclive.clics.v202207.Run(
        id = id,
        judgement_id = judgement_id!!,
        ordinal = ordinal!!,
        judgement_type_id = judgement_type_id!!,
        contest_time = contest_time!!
    )
}

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
) {
    fun upgrade() = org.icpclive.clics.v202207.State(
        ended = ended,
        frozen = frozen,
        thawed = thawed,
        started = started,
        unfrozen = unfrozen,
        finalized = finalized,
        end_of_updates = end_of_updates
    )
}

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
) {
    fun upgrade() = org.icpclive.clics.v202207.Commentary(
        id = id,
        time = time,
        contest_time = contest_time,
        message = message,
        team_ids = team_ids,
        problem_ids = problem_ids,
        submission_ids = submission_ids
    )
}

@Serializable
data class Clarification(val id: String) {
    fun upgrade() = org.icpclive.clics.v202207.Clarification(id)
}

@Serializable
data class Language(
    val id: String,
    val name: String? = null,
    val entry_point_required: Boolean? = null,
    val extensions: List<String>? = null,
) {
    fun upgrade() = org.icpclive.clics.v202207.Language(
        id = id,
        name = name,
        entry_point_required = entry_point_required,
        extensions = extensions
    )
}

@Serializable
data class Award(
    val id: String,
    val citation: String? = null,
    val team_ids: List<String>? = null
) {
    fun upgrade() = org.icpclive.clics.v202207.Award(
        id = id,
        citation = citation!!,
        team_ids = team_ids!!
    )
}

@Serializable
data class Person(
    val id: String,
    val name: String? = null,
    val role: String? = null
) {
    fun upgrade() = org.icpclive.clics.v202207.Person(
        id = id,
        name = name!!,
        role = role!!
    )
}

@Serializable
data class Account(
    val id: String,
    val username: String?,
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

    fun upgrade() = org.icpclive.clics.v202207.Account(
        id = id,
        username = username!!,
        password = password,
        type = type?.name?.let { org.icpclive.clics.v202207.Account.TYPE.valueOf(it) },
        ip = ip,
        team_id = team_id,
        person_id = person_id
    )
}
