package org.icpclive.clics.events

public sealed interface Event {
    public val token: String
}
public sealed interface UpdateContestEvent : Event
public sealed interface UpdateRunEvent: Event