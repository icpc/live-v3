package org.icpclive.gradle.tasks.impl

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.KClass

fun PrimitiveKind.toJsonTypeName(): String = when (this) {
    PrimitiveKind.BOOLEAN -> "boolean"
    PrimitiveKind.BYTE -> "number"
    PrimitiveKind.CHAR -> "number"
    PrimitiveKind.DOUBLE -> "number"
    PrimitiveKind.FLOAT -> "number"
    PrimitiveKind.INT -> "number"
    PrimitiveKind.LONG -> "number"
    PrimitiveKind.SHORT -> "number"
    PrimitiveKind.STRING -> "string"
}

fun SerialDescriptor.unwrapInlines(): SerialDescriptor = if (!isInline) this else getElementDescriptor(0).unwrapInlines()


@OptIn(ExperimentalSerializationApi::class)
fun SerialDescriptor.toJsonSchemaType(
    processed: MutableSet<String>,
    serializersModule: SerializersModule,
    definitions: MutableMap<String, JsonElement>,
    extras: Map<String, JsonElement> = emptyMap(),
    extraTypeProperty: String? = null,
    title: String? = null,
): JsonElement {
    if (kind is PrimitiveKind) {
        require(extraTypeProperty == null)
        return JsonObject(mapOf("type" to JsonPrimitive((kind as PrimitiveKind).toJsonTypeName())))
    }
    if (kind == SerialKind.CONTEXTUAL) {
        val kclass = capturedKClass
        if (kclass != null) {
            val defaultSerializer = serializersModule.serializer(kclass.java)
            return defaultSerializer.descriptor.toJsonSchemaType(processed, serializersModule, definitions, extras, extraTypeProperty)
        }
    }
    if (isInline) {
        return unwrapInlines().toJsonSchemaType(processed, serializersModule, definitions, extras, extraTypeProperty)
    }
    // Before processing, check for recursion
    val paramNamesWithTypes = elementDescriptors.map { it.serialName }
    val name = serialName + if (paramNamesWithTypes.isEmpty()) "" else "<${paramNamesWithTypes.joinToString(",")}>"
    val id = title ?: name
    if (!processed.contains(id)) {
        processed.add(id)
        val data = when (kind) {
            PolymorphicKind.OPEN -> {
                val subclasses = buildList {
                    serializersModule.dumpTo(object : SerializersModuleCollector {
                        override fun <T : Any> contextual(
                            kClass: KClass<T>,
                            provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
                        ) {
                        }

                        override fun <Base : Any, Sub : Base> polymorphic(
                            baseClass: KClass<Base>,
                            actualClass: KClass<Sub>,
                            actualSerializer: KSerializer<Sub>
                        ) {
                            if (baseClass == capturedKClass) {
                                add(actualSerializer as KSerializer<*>)
                            }
                        }

                        override fun <Base : Any> polymorphicDefaultDeserializer(
                            baseClass: KClass<Base>,
                            defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?
                        ) {
                        }

                        override fun <Base : Any> polymorphicDefaultSerializer(
                            baseClass: KClass<Base>,
                            defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?
                        ) {
                        }
                    })
                }
                val typeFieldName = getElementName(0)
                mapOf(
                    "oneOf" to JsonArray(
                        subclasses.map { it.descriptor }
                            .sortedBy { it.serialName }
                            .map {
                                it.toJsonSchemaType(
                                    processed,
                                    serializersModule,
                                    definitions,
                                    title = it.serialName.split(".").last(),
                                    extraTypeProperty = typeFieldName
                                )
                            }
                    )
                )
            }
            is PrimitiveKind -> error("Already handled")
            SerialKind.CONTEXTUAL -> {
                require(capturedKClass == null) { "Already handled" }
                mapOf(
                    "oneOf" to JsonArray(
                        elementDescriptors
                            .sortedBy { it.serialName }
                            .map {
                                it.toJsonSchemaType(
                                    processed,
                                    serializersModule,
                                    definitions,
                                    title = it.serialName.split(".").last(),
                                )
                            }
                    )
                )
            }
            PolymorphicKind.SEALED -> {
                require(extraTypeProperty == null)
                val typeFieldName = getElementName(0).takeIf { it != "NO_TYPE_FIELD" }
                val contextualDescriptor = getElementDescriptor(1)
                mapOf(
                    "oneOf" to JsonArray(
                        contextualDescriptor.elementDescriptors
                            .sortedBy { it.serialName }
                            .map {
                                it.toJsonSchemaType(
                                    processed,
                                    serializersModule,
                                    definitions,
                                    title = it.serialName.split(".").last(),
                                    extraTypeProperty = typeFieldName
                                )
                            }
                    )
                )
            }


            SerialKind.ENUM -> {
                require(extraTypeProperty == null)
                mapOf("enum" to JsonArray(elementNames.map { JsonPrimitive(it) }))
            }

            StructureKind.CLASS, StructureKind.OBJECT  -> {
                JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("object"),
                        "properties" to JsonObject(
                            listOfNotNull(extraTypeProperty).associateWith {
                                JsonObject(
                                    mapOf(
                                        "const" to JsonPrimitive(serialName),
                                        "default" to JsonPrimitive(serialName),
                                    )
                                )
                            } +
                                    (0 until elementsCount).associate {
                                        getElementName(it) to
                                                getElementDescriptor(it).toJsonSchemaType(processed, serializersModule, definitions)
                                    }
                        ),
                        "additionalProperties" to JsonPrimitive(false),
                        "required" to JsonArray(
                            (listOfNotNull(extraTypeProperty) +
                                    (0 until elementsCount).filterNot { isElementOptional(it) }
                                        .map { getElementName(it) }).map { JsonPrimitive(it) }
                        )
                    )
                )
            }

            StructureKind.LIST -> {
                mapOf(
                    "type" to JsonPrimitive("array"),
                    "items" to getElementDescriptor(0).toJsonSchemaType(processed, serializersModule, definitions)
                )
            }

            StructureKind.MAP -> {
                val keysSerializer = getElementDescriptor(0).unwrapInlines()
                val valuesSerializer = getElementDescriptor(1)
                when (keysSerializer.kind) {
                    PrimitiveKind.STRING -> {
                        JsonObject(
                            mapOf(
                                "type" to JsonPrimitive("object"),
                                "patternProperties" to JsonObject(
                                    mapOf(
                                        ".*" to valuesSerializer.toJsonSchemaType(
                                            processed,
                                            serializersModule,
                                            definitions
                                        )
                                    )
                                )
                            )
                        )
                    }

                    SerialKind.ENUM -> {
                        JsonObject(mapOf(
                            "type" to JsonPrimitive("object"),
                            "properties" to JsonObject(
                                (0 until keysSerializer.elementsCount).associate {
                                    keysSerializer.getElementName(it) to valuesSerializer.toJsonSchemaType(
                                        processed,
                                        serializersModule,
                                        definitions
                                    )
                                }
                            )
                        ))
                    }

                    else -> error("Unsupported map key: $keysSerializer")
                }
            }
        }.let {
            if (isNullable) {
                val schemaNull = JsonObject(mapOf("type" to JsonPrimitive("null")))
                if (it.keys.singleOrNull() == "oneOf") {
                    mapOf("oneOf" to JsonArray((it.values.single() as JsonArray).toList() + schemaNull))

                } else {
                    mapOf("oneOf" to JsonArray(listOf(JsonObject(it), schemaNull)))
                }
            } else {
                it
            }
        }
        val content = (extras + data).toMutableMap()
        if (title != null) {
            content["title"] = JsonPrimitive(title)
        }
        definitions[id] = JsonObject(content)
    }
    return JsonObject(mapOf("\$ref" to JsonPrimitive("#/\$defs/$id")))
}

fun SerialDescriptor.toJsonSchema(title: String, serializersModule: SerializersModule): JsonElement {
    val definitions = mutableMapOf<String, JsonElement>()
    val mainSchema = toJsonSchemaType(
        title = title,
        processed = mutableSetOf(),
        definitions = definitions,
        serializersModule = serializersModule,
    )
    return JsonObject(
        (mainSchema as JsonObject) +
                mapOf(
                    "\$defs" to JsonObject(definitions),
                )
    )
}