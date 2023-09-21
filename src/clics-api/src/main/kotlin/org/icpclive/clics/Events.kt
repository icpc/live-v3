package org.icpclive.clics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.clics.v1.Event as EventV1

interface IdEvent<T> {
    val id: String
    val data: T?
}

interface GlobalEvent<T> {
    val data: T?
}


@Serializable
sealed class Event {
    abstract val token: String

    @Serializable
    sealed class UpdateContestEvent : Event()

    @Serializable
    sealed class UpdateRunEvent : Event()

    @Serializable
    sealed class IgnoredEvent : Event()

    sealed class ContestEvent : UpdateContestEvent(), GlobalEvent<Contest>

    @Serializable
    @SerialName("contest")
    data class ContestEventNamedNonWithSpec(override val token: String, override val data: Contest?) : ContestEvent()
    @Serializable
    @SerialName("contests")
    data class ContestEventNamedWithSpec(override val token: String, override val data: Contest?) : ContestEvent()

    @Serializable
    @SerialName("problems")
    data class ProblemEvent(override val id: String, override val token: String, override val data: Problem?) :
        UpdateContestEvent(), IdEvent<Problem>

    @Serializable
    @SerialName("teams")
    data class TeamEvent(override val id: String, override val token: String, override val data: Team?) :
        UpdateContestEvent(), IdEvent<Team>

    @Serializable
    @SerialName("organizations")
    data class OrganizationEvent(override val id: String, override val token: String, override val data: Organization?) :
        UpdateContestEvent(), IdEvent<Organization>

    @Serializable
    @SerialName("state")
    data class StateEvent(override val token: String, override val data: State?) : UpdateContestEvent(),
        GlobalEvent<State>

    @Serializable
    @SerialName("judgement-types")
    data class JudgementTypeEvent(override val id: String, override val token: String, override val data: JudgementType?) :
        UpdateContestEvent(), IdEvent<JudgementType>

    @Serializable
    @SerialName("groups")
    data class GroupsEvent(override val id: String, override val token: String, override val data: Group?) :
        UpdateContestEvent(), IdEvent<Group>

    @Serializable
    @SerialName("submissions")
    data class SubmissionEvent(override val id: String, override val token: String, override val data: Submission?) :
        UpdateRunEvent(), IdEvent<Submission>

    @Serializable
    @SerialName("judgements")
    data class JudgementEvent(override val id: String, override val token: String, override val data: Judgement?) :
        UpdateRunEvent(), IdEvent<Judgement>

    @Serializable
    @SerialName("runs")
    data class RunsEvent(override val id: String, override val token: String, override val data: Run?) :
        UpdateRunEvent(), IdEvent<Run>

    @Serializable
    @SerialName("commentary")
    data class CommentaryEvent(override val id: String, override val token: String, override val data: Commentary?) :
        Event(), IdEvent<Commentary>

    @Serializable
    @SerialName("awards")
    data class AwardsEvent(override val id: String, override val token: String, override val data: Award?) :
        IgnoredEvent(), IdEvent<Award>

    @Serializable
    @SerialName("languages")
    data class LanguageEvent(override val id: String, override val token: String, override val data: Language?) :
        IgnoredEvent(), IdEvent<Language>

    @Serializable
    @SerialName("clarifications")
    data class ClarificationEvent(override val id: String, override val token: String, override val data: Clarification?) :
        IgnoredEvent(), IdEvent<Clarification>

    @Serializable
    @SerialName("accounts")
    data class AccountEvent(override val id: String, override val token: String, override val data: Account?)
        : IgnoredEvent(), IdEvent<Account>

    @Serializable
    @SerialName("persons")
    data class PersonEvent(override val id: String, override val token: String, override val data: Person) : IgnoredEvent(),
        IdEvent<Person>

    @Serializable
    @SerialName("map-info")
    data class MapEvent(override val token: String) : IgnoredEvent()

    @Serializable
    @SerialName("start-status")
    data class StartStatusEvent(override val token: String): IgnoredEvent()


    data class PreloadFinishedEvent(override val token: String) : UpdateContestEvent()


    companion object {
        fun fromV1(event: EventV1) = when (event) {
            is EventV1.CommentaryEvent         -> CommentaryEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.AwardsEvent             -> AwardsEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.ClarificationEvent      -> ClarificationEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.LanguageEvent           -> LanguageEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.ContestEvent            -> ContestEventNamedWithSpec(event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.GroupsEvent             -> GroupsEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.JudgementTypeEvent      -> JudgementTypeEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.OrganizationEvent       -> OrganizationEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.PreloadFinishedEvent    -> PreloadFinishedEvent(event.id)
            is EventV1.ProblemEvent            -> ProblemEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.StateEvent              -> StateEvent(event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.TeamEvent               -> TeamEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.JudgementEvent          -> JudgementEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.RunsEvent               -> RunsEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.SubmissionEvent         -> SubmissionEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
        }
    }
}