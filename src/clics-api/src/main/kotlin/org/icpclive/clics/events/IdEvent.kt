package org.icpclive.clics.events

public interface IdEvent<out T> {
    public val id: String
    public val data: T?
}