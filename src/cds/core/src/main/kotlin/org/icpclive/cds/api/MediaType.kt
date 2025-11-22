package org.icpclive.cds.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class MediaType() {
    public abstract val tags: List<String>
    public abstract val filename: String?
    public abstract val hash: String?
    public abstract val mime: String?

    @Serializable
    @SerialName("Image")
    public data class Image(
        val url: String,
        val width: Int? = null,
        val height: Int? = null,
        val vertical: Boolean = false,
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()

    @Serializable
    @SerialName("Audio")
    public data class Audio(
        val url: String,
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()

    @Serializable
    @SerialName("Text")
    public data class Text(
        val url: String,
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()

    @Serializable
    @SerialName("ZipArchive")
    public data class ZipArchive(
        val url: String,
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()
    
    @Serializable
    @SerialName("Object")
    public data class Object(
        val url: String,
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()

    @Serializable
    @SerialName("Video")
    public data class Video(
        val url: String,
        val vertical: Boolean = false,
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()

    @Serializable
    @SerialName("M2tsVideo")
    public data class M2tsVideo(
        val url: String,
        val vertical: Boolean = false,
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()

    @Serializable
    @SerialName("HLSVideo")
    public data class HLSVideo(
        val url: String,
        val jwtToken: String? = null,
        val vertical: Boolean = false,
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()

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
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
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
        override val tags: List<String> = emptyList(),
        override val filename: String? = null,
        override val hash: String? = null,
        override val mime: String? = null,
    ) : MediaType()
}
