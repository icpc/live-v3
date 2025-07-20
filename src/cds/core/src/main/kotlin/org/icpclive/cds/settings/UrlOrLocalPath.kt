package org.icpclive.cds.settings

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.icpclive.cds.util.map
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.relativeTo

/**
 * Represents a URL or a local file path.
 *
 * This is a place where ccs data can be located.
 *
 * In serialized json can be represented as three different ways.
 * * As just a string. Then it would be interpreted as url if starts with https:// or http://, otherwise as a local path
 * * As an object with url, login and password
 * * As an object with url, and auth field representing [Authorization] object.
 *
 * The first two are just shortcuts for common cases.
 */
public sealed class UrlOrLocalPath {
    public abstract fun subDir(s: String): UrlOrLocalPath

    public class Url(public val value: String, public val auth: Authorization? = null) : UrlOrLocalPath() {
        public override fun subDir(s: String): Url = Url("${value.removeSuffix("/")}/$s", auth)
        public fun withBasicAuth(login: Credential, password: Credential): Url = Url(this.value, (auth ?: Authorization()).withBasicAuth(login, password))
        public fun withCookie(name: String, value: Credential): Url = Url(this.value, (auth ?: Authorization()).withCookie(name, value))
        public fun withHeader(name: String, value: Credential): Url = Url(this.value, (auth ?: Authorization()).withHeader(name, value))
        override fun toString(): String = value
    }

    public class Local(public val value: Path) : UrlOrLocalPath() {
        public override fun subDir(s: String): Local = Local(value.resolve(s))
        override fun toString(): String = value.toString()
    }
}

@Serializable
public class Authorization(
    public val basic: BasicAuth? = null,
    public val cookies: Map<String, @Contextual Credential> = emptyMap(),
    public val headers: Map<String, @Contextual Credential> = emptyMap(),
) {
    @Serializable
    public class BasicAuth(public val login: @Contextual Credential, public val password: @Contextual Credential)
    public fun withBasicAuth(login: Credential, password: Credential): Authorization = Authorization(BasicAuth(login, password), cookies, headers)
    public fun withCookie(name: String, value: Credential): Authorization = Authorization(basic, cookies + (name to value), headers)
    public fun withHeader(name: String, value: Credential): Authorization = Authorization(basic, cookies, headers + (name to value))
}

private interface UrlOrLocalPathSurrogate {
    @JvmInline
    @Serializable
    value class Raw(val s: String): UrlOrLocalPathSurrogate

    @Serializable
    class WithLoginPassword(val url: String, val login: @Contextual Credential, val password: @Contextual Credential)

    @Serializable
    class WithWholeAuth(val url: String, val auth: Authorization): UrlOrLocalPathSurrogate
}

internal class UrlOrLocalPathSerializer(
    val localFilesDeserializationBase: Path,
    val localFilesSerializationBase: UrlOrLocalPath = UrlOrLocalPath.Local(localFilesDeserializationBase),
) : KSerializer<UrlOrLocalPath> {
    override fun serialize(encoder: Encoder, value: UrlOrLocalPath) {
        when (value) {
            is UrlOrLocalPath.Local -> raw.serialize(encoder, value)
            is UrlOrLocalPath.Url -> when {
                value.auth == null -> raw.serialize(encoder, value)
                value.auth.headers.isEmpty() && value.auth.cookies.isEmpty() && value.auth.basic != null -> withLoginPassword.serialize(encoder, value)
                else -> withWholeAuth.serialize(encoder, value)
            }
        }
    }

    override fun deserialize(decoder: Decoder): UrlOrLocalPath {
        val input = decoder as JsonDecoder
        val tree = input.decodeJsonElement()

        return input.json.decodeFromJsonElement(when {
            tree is JsonPrimitive -> raw
            tree is JsonObject && tree.containsKey("login") -> withLoginPassword
            tree is JsonObject && tree.containsKey("auth") -> withWholeAuth
            else -> throw SerializationException("Can't choose url deserializer")
        }, tree)
    }

    internal val raw = UrlOrLocalPathSurrogate.Raw.serializer().map(
        onDeserialize = {
            if (isHttpUrl(it.s)) {
                UrlOrLocalPath.Url(it.s)
            } else {
                val fixedPath = localFilesDeserializationBase.parent.resolve(it.s).toAbsolutePath()
                require(fixedPath.exists()) { "File $fixedPath mentioned in settings doesn't exist" }
                UrlOrLocalPath.Local(fixedPath)
            }
        },
        onSerialize = {
            when (it) {
                is UrlOrLocalPath.Url -> UrlOrLocalPathSurrogate.Raw(it.toString())
                is UrlOrLocalPath.Local -> {
                    UrlOrLocalPathSurrogate.Raw(it.value.relativeTo(localFilesDeserializationBase).fold(localFilesSerializationBase) { acc, part -> acc.subDir(part.name)}.toString())
                }
            }
        }
    )
    internal val withLoginPassword = UrlOrLocalPathSurrogate.WithLoginPassword.serializer().map(
        onDeserialize = { UrlOrLocalPath.Url(it.url, Authorization(Authorization.BasicAuth(it.login, it.password), emptyMap())) },
        onSerialize = { UrlOrLocalPathSurrogate.WithLoginPassword(it.value, it.auth!!.basic!!.login, it.auth.basic.password) }
    )
    internal val withWholeAuth = UrlOrLocalPathSurrogate.WithWholeAuth.serializer().map(
        onDeserialize = { UrlOrLocalPath.Url(it.url, it.auth) },
        onSerialize = { UrlOrLocalPathSurrogate.WithWholeAuth(it.value, it.auth ?: Authorization()) }
    )

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("UrlOrLocalPath", SerialKind.CONTEXTUAL) {
        element("Raw", raw.descriptor)
        element("WithLoginPassword", withLoginPassword.descriptor)
        element("WithWholeAuth", withWholeAuth.descriptor)
    }

    private companion object {
        private fun isHttpUrl(text: String) = text.startsWith("http://") || text.startsWith("https://")
    }
}