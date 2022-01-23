@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Event

@Serializable
sealed class MainScreenEvent : Event()

@Serializable
@SerialName("ShowWidget")
class ShowWidgetEvent(val widget: Widget) : MainScreenEvent()

@Serializable
@SerialName("HideWidget")
class HideWidgetEvent(val id:String) : MainScreenEvent()