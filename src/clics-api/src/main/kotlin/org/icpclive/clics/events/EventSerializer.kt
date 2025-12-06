package org.icpclive.clics.events

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.*
import org.icpclive.clics.objects.ObjectWithId

@Serializable
internal class IdEventSurrogate<T>(
    val id: String,
    val token: String?,
    val data: T?,
)

internal class IdEventSerializer<T : IdEvent<D>, D : ObjectWithId>(
    dataSerializer: KSerializer<D>,
    serialName: String,
    val constructor: (String, String?, D?) -> T,
) : KSerializer<T> {
    val delegate = IdEventSurrogate.serializer(dataSerializer)
    @OptIn(SealedSerializationApi::class)
    override val descriptor = SerialDescriptor(serialName, delegate.descriptor)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): T {
        val x = delegate.deserialize(decoder)
        return constructor(x.id, x.token, x.data)
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: T) {
        delegate.serialize(encoder, IdEventSurrogate(value.id, value.token?.value, value.data))
    }
}

@Serializable
internal class GlobalEventSurrogate<T>(
    val token: String?,
    val data: T,
)

internal class GlobalEventSerializer<T : GlobalEvent<D>, D>(
    dataSerializer: KSerializer<D>,
    serialName: String,
    val constructor: (String?, D) -> T,
) : KSerializer<T> {
    val delegate = GlobalEventSurrogate.serializer(dataSerializer)
    override val descriptor = SerialDescriptor(serialName, delegate.descriptor)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): T {
        val x = delegate.deserialize(decoder)
        return constructor(x.token, x.data)
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: T) {
        delegate.serialize(encoder, GlobalEventSurrogate(value.token?.value, value.data))
    }
}

@Serializable
internal class BatchEventSurrogate<T>(
    val token: String?,
    val data: List<T>,
)

internal class BatchEventSerializer<T : BatchEvent<D>, D>(
    dataSerializer: KSerializer<D>,
    serialName: String,
    val constructor: (String?, List<D>) -> T,
) : KSerializer<T> {
    val delegate = BatchEventSurrogate.serializer(dataSerializer)
    override val descriptor = SerialDescriptor(serialName, delegate.descriptor)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): T {
        val x = delegate.deserialize(decoder)
        return constructor(x.token, x.data)
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: T) {
        delegate.serialize(encoder, BatchEventSurrogate(value.token?.value, value.data))
    }
}

internal class LegacyEventSerializer<T : Event>(regularSerializer: KSerializer<T>) : JsonTransformingSerializer<T>(regularSerializer) {
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