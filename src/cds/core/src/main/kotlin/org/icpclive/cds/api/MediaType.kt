package org.icpclive.cds.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class MediaType {
    public abstract val isMedia: Boolean

    @Serializable
    @SerialName("Image")
    public data class Image(val url: String, override val isMedia: Boolean = true, val vertical: Boolean = false) : MediaType()

    @Serializable
    @SerialName("Object")
    public data class Object(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("Video")
    public data class Video(val url: String, override val isMedia: Boolean = true, val vertical: Boolean = false) : MediaType()

    @Serializable
    @SerialName("M2tsVideo")
    public data class M2tsVideo(val url: String, override val isMedia: Boolean = true, val vertical: Boolean = false) : MediaType()

    @Serializable
    @SerialName("HLSVideo")
    public data class HLSVideo(val url: String, val jwtToken: String? = null, override val isMedia: Boolean = true, val vertical: Boolean = false) : MediaType()

    /**
     * WebRTC proxy connection
     * @see <a href="https://github.com/kbats183/webrtc-proxy">https://github.com/kbats183/webrtc-proxy</a>
     */
    @Serializable
    @SerialName("WebRTCProxyConnection")
    public data class WebRTCProxyConnection(
        val url: String,
        val audioUrl: String? = null,
        override val isMedia: Boolean = true,
        val vertical: Boolean = false,
    ) : MediaType()

    /**
     * WebRTC grabber connection
     * https://github.com/irdkwmnsb/webrtc-grabber
     */
    @Serializable
    @SerialName("WebRTCGrabberConnection")
    public data class WebRTCGrabberConnection(
        val url: String,
        val peerName: String,
        val streamType: String,
        val credential: String?,
        override val isMedia: Boolean = true,
        val vertical: Boolean = false,
    ) :
        MediaType()

    @Serializable
    @SerialName("TaskStatus")
    public data class TaskStatus(val teamId: TeamId) : MediaType() {
        override val isMedia: Boolean = false
    }

    @Serializable
    @SerialName("TimeLine")
    public data class TimeLine(val teamId: TeamId) : MediaType() {
        override val isMedia: Boolean = false
    }

    public fun noMedia(): MediaType = when (this) {
        is Image -> copy(isMedia = false)
        is Video -> copy(isMedia = false)
        is Object -> copy(isMedia = false)
        is M2tsVideo -> copy(isMedia = false)
        is WebRTCProxyConnection -> copy(isMedia = false)
        is WebRTCGrabberConnection -> copy(isMedia = false)
        else -> this
    }
}
