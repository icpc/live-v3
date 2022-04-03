@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Event

@Serializable
sealed class MainScreenEvent : Event()

@Serializable
sealed class QueueEvent : Event()

@Serializable
sealed class TickerEvent: Event()

@Serializable
@SerialName("ShowWidget")
class ShowWidgetEvent(val widget: Widget) : MainScreenEvent()

@Serializable
@SerialName("HideWidget")
class HideWidgetEvent(val id: String) : MainScreenEvent()

@Serializable
@SerialName("MainScreenSnapshot")
class MainScreenSnapshotEvent(val widgets: List<Widget>) : MainScreenEvent()

@Serializable
@SerialName("AddRunToQueue")
class AddRunToQueueEvent(val info: RunInfo) : QueueEvent()

@Serializable
@SerialName("RemoveRunFromQueue")
class RemoveRunFromQueueEvent(val info: RunInfo) : QueueEvent()

@Serializable
@SerialName("ModifyRunInQueue")
class ModifyRunInQueueEvent(val info: RunInfo) : QueueEvent()

@Serializable
@SerialName("QueueSnapshot")
class QueueSnapshotEvent(val infos: List<RunInfo>) : QueueEvent()

@Serializable
@SerialName("AddMessage")
class AddMessageTickerEvent(val message: TickerMessage): TickerEvent()

@Serializable
@SerialName("RemoveMessage")
class RemoveMessageTickerEvent(val messageId: String): TickerEvent()

@Serializable
@SerialName("TickerSnapshot")
class TickerSnapshotEvent(val messages: List<TickerMessage>): TickerEvent()
