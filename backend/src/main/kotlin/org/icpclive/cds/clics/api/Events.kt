package org.icpclive.cds.clics.api

import org.icpclive.cds.clics.api.v1.Event as EventV1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Event {
    abstract val token: String

    @Serializable
    sealed class UpdateContestEvent : Event()

    @Serializable
    sealed class UpdateRunEvent : Event()

    @Serializable
    sealed class IgnoredEvent : Event()

    @Serializable
    @SerialName("contests")
    data class ContestEvent(override val token: String, val data: Contest?) :
        UpdateContestEvent()

    @Serializable
    @SerialName("problems")
    data class ProblemEvent(val id: String, override val token: String, val data: Problem?) :
        UpdateContestEvent()

    @Serializable
    @SerialName("teams")
    data class TeamEvent(val id: String, override val token: String, val data: Team?) : UpdateContestEvent()

    @Serializable
    @SerialName("organizations")
    data class OrganizationEvent(val id: String, override val token: String, val data: Organization?) :
        UpdateContestEvent()

    @Serializable
    @SerialName("state")
    data class StateEvent(override val token: String, val data: State?) : UpdateContestEvent()

    @Serializable
    @SerialName("judgement-types")
    data class JudgementTypeEvent(val id: String, override val token: String, val data: JudgementType?) :
        UpdateContestEvent()

    @Serializable
    @SerialName("groups")
    data class GroupsEvent(val id: String, override val token: String, val data: Group?) : UpdateContestEvent()

    @Serializable
    @SerialName("submissions")
    data class SubmissionEvent(val id: String, override val token: String, val data: Submission?) :
        UpdateRunEvent()

    @Serializable
    @SerialName("judgements")
    data class JudgementEvent(val id: String, override val token: String, val data: Judgement?) :
        UpdateRunEvent()

    @Serializable
    @SerialName("runs")
    data class RunsEvent(val id: String, override val token: String, val data: Run?) : UpdateRunEvent()

    @Serializable
    @SerialName("commentary")
    data class CommentaryEvent(val id: String, override val token: String, val data: Commentary?) : Event()

    @Serializable
    @SerialName("awards")
    data class AwardsEvent(val id: String, override val token: String, val award: Award?) : IgnoredEvent()

    @Serializable
    @SerialName("languages")
    data class LanguageEvent(val id: String, override val token: String, val language: Language?) : IgnoredEvent()

    @Serializable
    @SerialName("clarifications")
    data class ClarificationEvent(val id: String, override val token: String, val clarification: Clarification?) : IgnoredEvent()


    data class PreloadFinishedEvent(override val token: String) : UpdateContestEvent()


    companion object {
        fun fromV1(event: EventV1) = when (event) {
            is EventV1.CommentaryEvent         -> CommentaryEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.AwardsEvent             -> AwardsEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.ClarificationEvent      -> ClarificationEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.LanguageEvent           -> LanguageEvent(event.data.id, event.id, if (event.op == Operation.DELETE) null else event.data)
            is EventV1.ContestEvent            -> ContestEvent(event.id, if (event.op == Operation.DELETE) null else event.data)
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