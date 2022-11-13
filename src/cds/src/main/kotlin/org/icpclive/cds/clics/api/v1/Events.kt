package org.icpclive.cds.clics.api.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.clics.api.*

@Serializable
sealed class Event {
    abstract val id: String
    abstract val op: Operation

    @Serializable
    sealed class UpdateContestEvent : Event()

    @Serializable
    sealed class UpdateRunEvent : Event()

    @Serializable
    sealed class IgnoredEvent : Event()

    @Serializable
    @SerialName("contests")
    data class ContestEvent(override val id: String, override val op: Operation, val data: Contest) :
        UpdateContestEvent()

    @Serializable
    @SerialName("problems")
    data class ProblemEvent(override val id: String, override val op: Operation, val data: Problem) :
        UpdateContestEvent()

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
    data class SubmissionEvent(override val id: String, override val op: Operation, val data: Submission) :
        UpdateRunEvent()

    @Serializable
    @SerialName("judgements")
    data class JudgementEvent(override val id: String, override val op: Operation, val data: Judgement) :
        UpdateRunEvent()

    @Serializable
    @SerialName("runs")
    data class RunsEvent(override val id: String, override val op: Operation, val data: Run) : UpdateRunEvent()

    @Serializable
    @SerialName("commentary")
    data class CommentaryEvent(override val id: String, override val op: Operation, val data: Commentary) : Event()

    @Serializable
    @SerialName("awards")
    data class AwardsEvent(override val id: String, override val op: Operation, val data: Award) : IgnoredEvent()

    @Serializable
    @SerialName("languages")
    data class LanguageEvent(override val id: String, override val op: Operation, val data: Language) : IgnoredEvent()

    @Serializable
    @SerialName("clarifications")
    data class ClarificationEvent(override val id: String, override val op: Operation, val data: Clarification) : IgnoredEvent()


    data class PreloadFinishedEvent(override val id: String, override val op: Operation) : UpdateContestEvent()
}