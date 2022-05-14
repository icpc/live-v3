package org.icpclive.cds.clics.api

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
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
data class Organization(
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
sealed class Event {
    abstract val id: String
}

@Serializable
sealed class UpdateContestEvent : Event()
@Serializable
sealed class UpdateRunEvent : Event()
@Serializable
sealed class IgnoredEvent : Event()
@Serializable
@SerialName("contests")
class ContestEvent(override val id: String, val data: Contest) : UpdateContestEvent()
@Serializable
@SerialName("problems")
class ProblemEvent(override val id: String, val data: Problem) : UpdateContestEvent()
@Serializable
@SerialName("teams")
class TeamEvent(override val id: String, val data: Team) : UpdateContestEvent()
@Serializable
@SerialName("organizations")
class OrganizationEvent(override val id: String, val data: Organization) : UpdateContestEvent()
@Serializable
@SerialName("state")
class StateEvent(override val id: String, val data: State) : UpdateContestEvent()
@Serializable
@SerialName("submissions")
class SubmissionEvent(override val id: String, val data: Submission) : UpdateRunEvent()
@Serializable
@SerialName("judgements")
class JudgementEvent(override val id: String, val data: Judgement) : UpdateRunEvent()
@Serializable
@SerialName("runs")
class RunsEvent(override val id: String, val data: Run) : UpdateRunEvent()
@Serializable
@SerialName("commentary")
class CommentaryEvent(override val id: String) : IgnoredEvent()
@Serializable
@SerialName("awards")
class AwardsEvent(override val id: String) : IgnoredEvent()
@Serializable
@SerialName("judgement-types")
class JudgementTypeEvent(override val id: String) : IgnoredEvent()
@Serializable
@SerialName("languages")
class LanguageEvent(override val id: String) : IgnoredEvent()
@Serializable
@SerialName("groups")
class GroupsEvent(override val id: String) : IgnoredEvent()
