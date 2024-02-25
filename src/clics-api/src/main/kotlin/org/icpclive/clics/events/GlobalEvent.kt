package org.icpclive.clics.events

public interface GlobalEvent<out T> {
    public val data: T?
}