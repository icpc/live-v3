package org.icpclive.clics.events

public sealed interface BatchEvent<out T> : GlobalEvent<List<T>> {
    override val data: List<T>
}