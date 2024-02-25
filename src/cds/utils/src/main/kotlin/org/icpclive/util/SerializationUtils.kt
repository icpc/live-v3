package org.icpclive.util

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModuleBuilder
import java.awt.Color
import java.io.InputStream
import java.lang.Exception

public object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString("#%02x%02x%02x%02x".format(value.red, value.green, value.blue, value.alpha))
    }

    override fun deserialize(decoder: Decoder): Color {
        val data = decoder.decodeString()
        return try {
            if (data.startsWith("0x")) {
                return Color(data.toUInt(radix = 16).toInt(), data.length == 8)
            }
            val str = data.removePrefix("#")
            when (str.length) {
                8 -> Color(
                    str.substring(0, 2).toInt(radix = 16),
                    str.substring(2, 4).toInt(radix = 16),
                    str.substring(4, 6).toInt(radix = 16),
                    str.substring(6, 8).toInt(radix = 16),
                )

                6 -> Color(
                    str.substring(0, 2).toInt(radix = 16),
                    str.substring(2, 4).toInt(radix = 16),
                    str.substring(4, 6).toInt(radix = 16),
                )

                3 -> Color(
                    str[0].digitToInt(16) * 0x11,
                    str[1].digitToInt(16) * 0x11,
                    str[2].digitToInt(16) * 0x11,
                )

                else -> {
                    throw NumberFormatException()
                }
            }
        } catch (e: NumberFormatException) {
            getLogger(ColorSerializer::class).error("Failed to parse color from $data", e)
            Color.BLACK
        }
    }
}

public object RegexSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Regex", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) : Regex {
        val s = decoder.decodeString()
        return try {
            Regex(s)
        } catch (e: Exception) {
            throw SerializationException("Failed to compile regexp: $s", e)
        }
    }

    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }
}

@OptIn(ExperimentalSerializationApi::class)
public fun defaultJsonSettings(): Json = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    allowStructuredMapKeys = true
    prettyPrint = false
    useArrayPolymorphism = false
    explicitNulls = false
}

@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> Json.decodeFromStreamIgnoringComments(stream: InputStream) : T = decodeFromJsonElement(decodeFromStream<JsonElement>(stream).cleanFromComments())
public inline fun <reified T> Json.decodeFromStringIgnoringComments(data: String) : T = decodeFromJsonElement(decodeFromString<JsonElement>(data).cleanFromComments())

@PublishedApi internal fun JsonElement.cleanFromComments() : JsonElement {
    return when (this) {
        is JsonArray -> JsonArray(map { it.cleanFromComments() })
        is JsonObject -> JsonObject(filter { !it.key.startsWith("#") }.mapValues { it.value.cleanFromComments() })
        is JsonPrimitive, JsonNull -> this
    }
}

public inline fun <reified T: Any> SerializersModuleBuilder.postProcess(
    crossinline onDeserialize: (T) -> T = { it },
    crossinline onSerialize: (T) -> T = { it },
) {
    postProcess(serializer<T>(), onDeserialize, onSerialize)
}

public inline fun <reified T: Any, reified S: Any> SerializersModuleBuilder.postProcess(
    serializer: KSerializer<S> = serializer<S>(),
    crossinline onDeserialize: (S) -> T,
    crossinline onSerialize: (T) -> S,
) {
    contextual(T::class, object : KSerializer<T> {
        private val delegate = serializer
        override val descriptor get() = delegate.descriptor
        override fun deserialize(decoder: Decoder) = onDeserialize(delegate.deserialize(decoder))
        override fun serialize(encoder: Encoder, value: T) = delegate.serialize(encoder, onSerialize(value))
    })
}