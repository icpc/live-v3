package org.icpclive.cds.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class MediaType {

    @Serializable
    @SerialName("Image")
    public data class Image(val url: String, val vertical: Boolean = false) : MediaType()

    @Serializable
    @SerialName("Object")
    public data class Object(val url: String) : MediaType()

    @Serializable
    @SerialName("Video")
    public data class Video(val url: String, val vertical: Boolean = false) : MediaType()

    @Serializable
    @SerialName("M2tsVideo")
    public data class M2tsVideo(val url: String, val vertical: Boolean = false) : MediaType()

    @Serializable
    @SerialName("HLSVideo")
    public data class HLSVideo(val url: String, val jwtToken: String? = null, val vertical: Boolean = false) : MediaType()

    /**
     * WebRTC proxy connection
     * @see <a href="https://github.com/kbats183/webrtc-proxy">https://github.com/kbats183/webrtc-proxy</a>
     */
    @Serializable
    @SerialName("WebRTCProxyConnection")
    public data class WebRTCProxyConnection(
        val url: String,
        val audioUrl: String? = null,
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
        val vertical: Boolean = false,
    ) : MediaType()
}
