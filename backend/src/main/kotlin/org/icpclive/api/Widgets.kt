@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LocationRectangle(
    val positionX: Int,
    val positionY: Int,
    val sizeX: Int,
    val sizeY: Int,
)

@Serializable
sealed class Widget(
    val widgetId: String,
    val location: LocationRectangle
)

@Serializable
@SerialName("AdvertisementWidget")
class AdvertisementWidget(val advertisement: Advertisement) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "advisement"
        val location = LocationRectangle(0, 860, 1920, 90)
    }
}

@Serializable
@SerialName("PictureWidget")
class PictureWidget(val picture: Picture) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "picture"
        val location = LocationRectangle(590, 50, 1300, 960)
    }
}
