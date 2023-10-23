package org.icpclive.clics.v202207

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import org.icpclive.clics.*

@Serializable
sealed class Event {
    abstract val token: String

    @Serializable
    sealed class UpdateContestEvent : Event()

    @Serializable
    sealed class UpdateRunEvent : Event()

    sealed class ContestEvent : UpdateContestEvent(), GlobalEvent<Contest>

    @Serializable
    @SerialName("contest")
    internal data class ContestEventNamedNonWithSpec(override val token: String, override val data: Contest?) : ContestEvent()
    @Serializable
    @SerialName("contests")
    internal data class ContestEventNamedWithSpec(override val token: String, override val data: Contest?) : ContestEvent()

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
    data class RunEvent(override val id: String, override val token: String, override val data: Run?) :
        UpdateRunEvent(), IdEvent<Run>

    @Serializable
    @SerialName("commentary")
    data class CommentaryEvent(override val id: String, override val token: String, override val data: Commentary?) :
        Event(), IdEvent<Commentary>

    @Serializable
    @SerialName("awards")
    data class AwardsEvent(override val id: String, override val token: String, override val data: Award?) :
        UpdateContestEvent(), IdEvent<Award>

    @Serializable
    @SerialName("languages")
    data class LanguageEvent(override val id: String, override val token: String, override val data: Language?) :
        UpdateContestEvent(), IdEvent<Language>

    @Serializable
    @SerialName("accounts")
    data class AccountEvent(override val id: String, override val token: String, override val data: Account?)
        : UpdateContestEvent(), IdEvent<Account>

    @Serializable
    @SerialName("persons")
    data class PersonEvent(override val id: String, override val token: String, override val data: Person?) : UpdateContestEvent(),
        IdEvent<Person>

    @Serializable
    @SerialName("map-info")
    data class MapEvent(override val token: String) : UpdateContestEvent()

    @Serializable
    @SerialName("start-status")
    data class StartStatusEvent(override val token: String): UpdateContestEvent()

    @Serializable
    @SerialName("clarifications")
    data class ClarificationEvent(override val id: String, override val token: String, override val data: Clarification?) : UpdateContestEvent(),
        IdEvent<Clarification>


    data class PreloadFinishedEvent(override val token: String) : UpdateContestEvent()

    companion object {
        fun ContestEvent(token: String, data: Contest?) : ContestEvent = ContestEventNamedWithSpec(token, data)
    }
}