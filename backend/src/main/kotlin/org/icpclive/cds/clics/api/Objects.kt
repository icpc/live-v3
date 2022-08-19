package org.icpclive.cds.clics.api

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.utils.ClicsTime
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
    val penalty_time: Int? = null
)

@Serializable
data class Problem(
    val id: String,
    val ordinal: Int = 0,
    val label: String = "",
    val name: String = "",
    val rgb: String? = null,
    val test_data_count: Int? = null
)

@Serializable
data class Media(
    val href: String
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
    val solved: Boolean? = null,
    val penalty: Boolean? = null
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
sealed class Event {
    abstract val id: String
    abstract val op: Operation
}

@Serializable
sealed class UpdateContestEvent : Event()

@Serializable
sealed class UpdateRunEvent : Event()

@Serializable
sealed class IgnoredEvent : Event()

@Serializable
@SerialName("contests")
data class ContestEvent(override val id: String, override val op: Operation, val data: Contest) : UpdateContestEvent()

@Serializable
@SerialName("problems")
data class ProblemEvent(override val id: String, override val op: Operation, val data: Problem) : UpdateContestEvent()

@Serializable
@SerialName("teams")
data class TeamEvent(override val id: String, override val op: Operation, val data: Team) : UpdateContestEvent()

@Serializable
@SerialName("organizations")
data class OrganizationEvent(override val id: String, override val op: Operation, val data: Organization) :
    UpdateContestEvent()

@Serializable
@SerialName("state")
data class StateEvent(override val id: String, override val op: Operation, val data: State) : UpdateContestEvent()

@Serializable
@SerialName("judgement-types")
data class JudgementTypeEvent(override val id: String, override val op: Operation, val data: JudgementType) :
    UpdateContestEvent()

@Serializable
@SerialName("groups")
data class GroupsEvent(override val id: String, override val op: Operation, val data: Group) : UpdateContestEvent()

@Serializable
@SerialName("submissions")
data class SubmissionEvent(override val id: String, override val op: Operation, val data: Submission) : UpdateRunEvent()

@Serializable
@SerialName("judgements")
data class JudgementEvent(override val id: String, override val op: Operation, val data: Judgement) : UpdateRunEvent()

@Serializable
@SerialName("runs")
data class RunsEvent(override val id: String, override val op: Operation, val data: Run) : UpdateRunEvent()

@Serializable
@SerialName("commentary")
data class CommentaryEvent(override val id: String, override val op: Operation, val data: Commentary): Event()

@Serializable
@SerialName("awards")
data class AwardsEvent(override val id: String, override val op: Operation) : IgnoredEvent()

@Serializable
@SerialName("languages")
data class LanguageEvent(override val id: String, override val op: Operation) : IgnoredEvent()

data class PreloadFinishedEvent(override val id: String, override val op: Operation) : UpdateContestEvent()
