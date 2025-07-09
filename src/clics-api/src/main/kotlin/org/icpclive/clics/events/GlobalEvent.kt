package org.icpclive.clics.events

public sealed interface GlobalEvent<out T> : Event {
    public val data: T
}