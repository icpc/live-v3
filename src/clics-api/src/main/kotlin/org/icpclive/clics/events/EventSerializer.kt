package org.icpclive.clics.events

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.icpclive.clics.v202003.events.Operation

@Serializable
internal class IdEventSurrogate<T>(
    val id: String,
    val token: EventToken?,
    val data: T?,
)

internal abstract class IdEventSerializer<T : IdEvent<D>, D>(
    dataSerializer: KSerializer<D>,
    serialName: String,
    val constructor: (String, EventToken?, D?) -> T,
) : KSerializer<T> {
    val delegate = IdEventSurrogate.serializer(dataSerializer)
    @OptIn(SealedSerializationApi::class)
    override val descriptor = SerialDescriptor(serialName, delegate.descriptor)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): T {
        val x = delegate.deserialize(decoder)
        return constructor(x.id, x.token, x.data)
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: T) {
        delegate.serialize(encoder, IdEventSurrogate(value.id, value.token, value.data))
    }
}

@Serializable
internal class GlobalEventSurrogate<T>(
    val token: EventToken?,
    val data: T,
)

internal abstract class GlobalEventSerializer<T : GlobalEvent<D>, D>(
    dataSerializer: KSerializer<D>,
    serialName: String,
    val constructor: (EventToken?, D) -> T,
) : KSerializer<T> {
    val delegate = GlobalEventSurrogate.serializer(dataSerializer)
    override val descriptor = SerialDescriptor(serialName, delegate.descriptor)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): T {
        val x = delegate.deserialize(decoder)
        return constructor(x.token, x.data)
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: T) {
        delegate.serialize(encoder, GlobalEventSurrogate(value.token, value.data))
    }
}

@Serializable
internal class BatchEventSurrogate<T>(
    val token: EventToken?,
    val data: List<T>,
)

internal abstract class BatchEventSerializer<T : BatchEvent<D>, D>(
    dataSerializer: KSerializer<D>,
    serialName: String,
    val constructor: (EventToken?, List<D>) -> T,
) : KSerializer<T> {
    val delegate = BatchEventSurrogate.serializer(dataSerializer)
    override val descriptor = SerialDescriptor(serialName, delegate.descriptor)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): T {
        val x = delegate.deserialize(decoder)
        return constructor(x.token, x.data)
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: T) {
        delegate.serialize(encoder, BatchEventSurrogate(value.token, value.data))
    }
}

internal abstract class LegacyEventSerializer<T : Event>(regularSerializer: KSerializer<T>) : JsonTransformingSerializer<T>(regularSerializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        require(element is JsonObject)
        val operation = element["op"]?.jsonPrimitive?.content
        if (operation == "delete") {
            return JsonObject(
                mapOf(
                    "type" to (element["type"] ?: JsonNull),
                    "token" to (element["id"] ?: JsonNull),
                    "id" to ((element["data"] as? JsonObject)?.get("id") ?: JsonNull)
                )
            )
        }
        if (operation == "create" || operation == "update") {
            return JsonObject(
                mapOf(
                    "type" to (element["type"] ?: JsonNull),
                    "token" to (element["id"] ?: JsonNull),
                    "id" to ((element["data"] as? JsonObject)?.get("id") ?: JsonNull),
                    "data" to (element["data"] ?: JsonNull)
                )
            )
        }
        throw SerializationException("Unknown event operation $operation, expected create/delete/update, got: $operation")
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        require(element is JsonObject)
        return JsonObject(
            mapOf(
                "id" to element["token"]!!,
                "operation" to JsonPrimitive(if ("data" in element) "create" else "delete"),
                "data" to (element["data"] ?: JsonObject(mapOf("id" to element["id"]!!)))
            )
        )
    }
}