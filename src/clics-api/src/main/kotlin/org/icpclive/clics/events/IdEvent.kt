package org.icpclive.clics.events

import org.icpclive.clics.objects.ObjectWithId

public sealed interface IdEvent<out T: ObjectWithId> : Event {
    public val id: String
    public val data: T?
}