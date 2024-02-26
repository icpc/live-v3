package org.icpclive.clics.v202207

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import org.icpclive.clics.*

@Serializable
public sealed class Event {
    public abstract val token: String

    @Serializable
    public sealed class UpdateContestEvent : Event()

    @Serializable
    public sealed class UpdateRunEvent : Event()

    public sealed class ContestEvent : UpdateContestEvent(), GlobalEvent<Contest>

    @Serializable
    @SerialName("contest")
    internal data class ContestEventNamedNonWithSpec(override val token: String, override val data: Contest?) : ContestEvent()
    @Serializable
    @SerialName("contests")
    internal data class ContestEventNamedWithSpec(override val token: String, override val data: Contest?) : ContestEvent()

    @Serializable
    @SerialName("problems")
    public data class ProblemEvent(override val id: String, override val token: String, override val data: Problem?) :
        UpdateContestEvent(), IdEvent<Problem>

    @Serializable
    @SerialName("teams")
    public data class TeamEvent(override val id: String, override val token: String, override val data: Team?) :
        UpdateContestEvent(), IdEvent<Team>

    @Serializable
    @SerialName("organizations")
    public data class OrganizationEvent(override val id: String, override val token: String, override val data: Organization?) :
        UpdateContestEvent(), IdEvent<Organization>

    @Serializable
    @SerialName("state")
    public data class StateEvent(override val token: String, override val data: State?) : UpdateContestEvent(),
        GlobalEvent<State>

    @Serializable
    @SerialName("judgement-types")
    public data class JudgementTypeEvent(override val id: String, override val token: String, override val data: JudgementType?) :
        UpdateContestEvent(), IdEvent<JudgementType>

    @Serializable
    @SerialName("groups")
    public data class GroupsEvent(override val id: String, override val token: String, override val data: Group?) :
        UpdateContestEvent(), IdEvent<Group>

    @Serializable
    @SerialName("submissions")
    public data class SubmissionEvent(override val id: String, override val token: String, override val data: Submission?) :
        UpdateRunEvent(), IdEvent<Submission>

    @Serializable
    @SerialName("judgements")
    public data class JudgementEvent(override val id: String, override val token: String, override val data: Judgement?) :
        UpdateRunEvent(), IdEvent<Judgement>

    @Serializable
    @SerialName("runs")
    public data class RunEvent(override val id: String, override val token: String, override val data: Run?) :
        UpdateRunEvent(), IdEvent<Run>

    @Serializable
    @SerialName("commentary")
    public data class CommentaryEvent(override val id: String, override val token: String, override val data: Commentary?) :
        Event(), IdEvent<Commentary>

    @Serializable
    @SerialName("awards")
    public data class AwardsEvent(override val id: String, override val token: String, override val data: Award?) :
        UpdateContestEvent(), IdEvent<Award>

    @Serializable
    @SerialName("languages")
    public data class LanguageEvent(override val id: String, override val token: String, override val data: Language?) :
        UpdateContestEvent(), IdEvent<Language>

    @Serializable
    @SerialName("accounts")
    public data class AccountEvent(override val id: String, override val token: String, override val data: Account?)
        : UpdateContestEvent(), IdEvent<Account>

    @Serializable
    @SerialName("persons")
    public data class PersonEvent(override val id: String, override val token: String, override val data: Person?) : UpdateContestEvent(),
        IdEvent<Person>

    @Serializable
    @SerialName("map-info")
    public data class MapEvent(override val token: String) : UpdateContestEvent()

    @Serializable
    @SerialName("start-status")
    public data class StartStatusEvent(override val token: String): UpdateContestEvent()

    @Serializable
    @SerialName("clarifications")
    public data class ClarificationEvent(override val id: String, override val token: String, override val data: Clarification?) : UpdateContestEvent(),
        IdEvent<Clarification>


    public data class PreloadFinishedEvent(override val token: String) : UpdateContestEvent()

    public companion object {
        public fun ContestEvent(token: String, data: Contest?) : ContestEvent = ContestEventNamedWithSpec(token, data)
    }
}