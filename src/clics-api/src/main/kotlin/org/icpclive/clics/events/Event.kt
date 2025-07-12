package org.icpclive.clics.events

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class EventToken(public val value: String)

public sealed interface Event {
    public val token: EventToken?
}