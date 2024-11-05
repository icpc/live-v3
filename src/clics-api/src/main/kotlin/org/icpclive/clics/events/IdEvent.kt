package org.icpclive.clics.events

public sealed interface IdEvent<out T> : Event {
    public val id: String
    public val data: T?
}