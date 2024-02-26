package org.icpclive.clics.v202003

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class Event {
    public abstract val id: String
    public abstract val op: Operation

    @Serializable
    @SerialName("contests")
    public data class ContestEvent(override val id: String, override val op: Operation, val data: Contest) : Event()

    @Serializable
    @SerialName("problems")
    public data class ProblemEvent(override val id: String, override val op: Operation, val data: Problem) : Event()

    @Serializable
    @SerialName("teams")
    public data class TeamEvent(override val id: String, override val op: Operation, val data: Team) : Event()

    @Serializable
    @SerialName("organizations")
    public data class OrganizationEvent(override val id: String, override val op: Operation, val data: Organization) : Event()

    @Serializable
    @SerialName("state")
    public data class StateEvent(override val id: String, override val op: Operation, val data: State) : Event()

    @Serializable
    @SerialName("judgement-types")
    public data class JudgementTypeEvent(override val id: String, override val op: Operation, val data: JudgementType) : Event()

    @Serializable
    @SerialName("groups")
    public data class GroupsEvent(override val id: String, override val op: Operation, val data: Group) : Event()

    @Serializable
    @SerialName("submissions")
    public data class SubmissionEvent(override val id: String, override val op: Operation, val data: Submission) : Event()

    @Serializable
    @SerialName("judgements")
    public data class JudgementEvent(override val id: String, override val op: Operation, val data: Judgement) : Event()

    @Serializable
    @SerialName("runs")
    public data class RunEvent(override val id: String, override val op: Operation, val data: Run) : Event()

    @Serializable
    @SerialName("commentary")
    public data class CommentaryEvent(override val id: String, override val op: Operation, val data: Commentary) : Event()

    @Serializable
    @SerialName("awards")
    public data class AwardsEvent(override val id: String, override val op: Operation, val data: Award) : Event()

    @Serializable
    @SerialName("languages")
    public data class LanguageEvent(override val id: String, override val op: Operation, val data: Language) : Event()

    @Serializable
    @SerialName("clarifications")
    public data class ClarificationEvent(override val id: String, override val op: Operation, val data: Clarification) : Event()


    public data class PreloadFinishedEvent(override val id: String, override val op: Operation) : Event()
}

public fun Event.upgrade(): org.icpclive.clics.v202207.Event = when (this) {
    is Event.AwardsEvent -> org.icpclive.clics.v202207.Event.AwardsEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.ClarificationEvent -> org.icpclive.clics.v202207.Event.ClarificationEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.CommentaryEvent -> org.icpclive.clics.v202207.Event.CommentaryEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.ContestEvent -> org.icpclive.clics.v202207.Event.ContestEventNamedWithSpec(id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.GroupsEvent -> org.icpclive.clics.v202207.Event.GroupsEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.JudgementEvent -> org.icpclive.clics.v202207.Event.JudgementEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.JudgementTypeEvent -> org.icpclive.clics.v202207.Event.JudgementTypeEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.LanguageEvent -> org.icpclive.clics.v202207.Event.LanguageEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.OrganizationEvent -> org.icpclive.clics.v202207.Event.OrganizationEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.PreloadFinishedEvent -> org.icpclive.clics.v202207.Event.PreloadFinishedEvent(id)
    is Event.ProblemEvent -> org.icpclive.clics.v202207.Event.ProblemEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.RunEvent -> org.icpclive.clics.v202207.Event.RunEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.StateEvent -> org.icpclive.clics.v202207.Event.StateEvent(id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.SubmissionEvent -> org.icpclive.clics.v202207.Event.SubmissionEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
    is Event.TeamEvent -> org.icpclive.clics.v202207.Event.TeamEvent(data.id, id, data.takeIf { op != Operation.DELETE }?.upgrade())
}