package org.icpclive.gradle.tasks.worker

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import kotlin.reflect.*
import kotlin.reflect.full.*

private fun PrimitiveKind.toJsonTypeName(): JsonPrimitive = JsonPrimitive(when (this) {
    PrimitiveKind.BOOLEAN -> "boolean"
    PrimitiveKind.BYTE -> "number"
    PrimitiveKind.CHAR -> "number"
    PrimitiveKind.DOUBLE -> "number"
    PrimitiveKind.FLOAT -> "number"
    PrimitiveKind.INT -> "number"
    PrimitiveKind.LONG -> "number"
    PrimitiveKind.SHORT -> "number"
    PrimitiveKind.STRING -> "string"
})

private val NULL_SCHEMA = JsonObject(mapOf("type" to JsonPrimitive("null")))
private val ALLOWED_IN_DEF_ID = buildSet {
    addAll('a'..'z')
    addAll('A'..'Z')
    addAll('0'..'9')
    addAll("-._!$&'()*+,;=:@?".asIterable())
}
fun JsonElement.toMaybeWrappedList() = when (this) {
    is JsonArray -> toList()
    else -> listOf(this)
}

private fun JsonObject.orNull(): JsonObject {
    if (size == 1) {
        get("type")?.let {
            return buildJsonObject {
                put("type", JsonArray(it.toMaybeWrappedList() + JsonPrimitive("null")))
            }
        }
        get("oneOf")?.let {
            return oneOf(it.toMaybeWrappedList() + NULL_SCHEMA)
        }
    }
    return oneOf(listOf(JsonObject(this@orNull), NULL_SCHEMA))
}

private fun SerialDescriptor.unwrapInlines(): SerialDescriptor {
    val inner = if (!isInline) this else getElementDescriptor(0).unwrapInlines()
    return if (isNullable) {
        inner.nullable
    } else {
        inner
    }
}

private fun SerialDescriptor.schemaDefId(stack: MutableList<SerialDescriptor> = mutableListOf()): String {
    fun String.sanitizeForDefId(): String = map {
        when (it) {
            '<' -> '('
            '>' -> ')'
            in ALLOWED_IN_DEF_ID -> it
            else -> '.'
        }
    }.joinToString("")
    if (stack.any { it === this }) return serialName.sanitizeForDefId()
    stack.add(this)
    try {
        val args = when (kind) {
            StructureKind.LIST, StructureKind.MAP, SerialKind.CONTEXTUAL -> elementDescriptors.toList()
            else -> emptyList()
        }
        return serialName.sanitizeForDefId() + if (args.isEmpty()) "" else args.joinToString(",", prefix = "(", postfix = ")") { it.schemaDefId(stack) }
    } finally {
        stack.removeAt(stack.size - 1)
    }
}

private data class DefinitionKey(val descriptor: SerialDescriptor, val extraTypeProperty: String?)


private fun oneOf(options: List<JsonElement>) : JsonObject {
    return jsonObjectOf(
        "oneOf" to JsonArray(options)
    )
}

private fun jsonObjectOf(vararg pairs: Pair<String, JsonElement>): JsonObject {
    return JsonObject(mapOf(*pairs))
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.toJsonSchemaType(
    processed: MutableMap<DefinitionKey, String>,
    serializersModule: SerializersModule,
    definitions: MutableMap<String, JsonElement>,
    extraTypeProperty: String? = null,
): JsonObject {
    (kind as? PrimitiveKind)?.let { kind ->
        require(extraTypeProperty == null)
        val primitive = buildJsonObject {
            put("type", kind.toJsonTypeName())
        }
        return if (isNullable) primitive.orNull() else primitive
    }
    if (kind == SerialKind.CONTEXTUAL) {
        capturedKClass?.let { kClass ->
            val defaultSerializer = serializersModule.serializer(kClass.java)
            val contextual = defaultSerializer.descriptor.let { if (isNullable) it.nullable else it }
            return contextual.toJsonSchemaType(processed, serializersModule, definitions, extraTypeProperty)
        }
    }
    if (isInline) {
        return unwrapInlines().toJsonSchemaType(processed, serializersModule, definitions, extraTypeProperty)
    }

    fun List<SerialDescriptor>.oneOf(typeFieldName: String?) = oneOf(
        map {
            it.toJsonSchemaType(processed, serializersModule, definitions, extraTypeProperty = typeFieldName)
        }
    )

    val key = DefinitionKey(this, extraTypeProperty)
    if (!processed.contains(key)) {
        val name = schemaDefId()
        val id = generateSequence(1) { it + 1 }
            .map { if (it == 1) name else "$name-$it" }
            .first { it !in processed.values }
        processed[key] = id
        val data = when (kind) {
            PolymorphicKind.OPEN -> {
                openPolymorphicSubclasses(serializersModule)
                    .map { it.descriptor }
                    .sortedBy { it.serialName }
                    .oneOf(getElementName(0))
            }

            is PrimitiveKind -> error("Already handled")
            SerialKind.CONTEXTUAL -> {
                require(capturedKClass == null) { "Already handled" }
                elementDescriptors
                    .sortedBy { it.serialName }
                    .oneOf(null)
            }

            PolymorphicKind.SEALED -> {
                require(extraTypeProperty == null)
                getElementDescriptor(1)
                    .elementDescriptors
                    .sortedBy { it.serialName }
                    .oneOf(getElementName(0).takeIf { it != "NO_TYPE_FIELD" })
            }

            SerialKind.ENUM -> {
                require(extraTypeProperty == null)
                jsonObjectOf(
                    "enum" to JsonArray(elementNames.map { JsonPrimitive(it) })
                )
            }

            StructureKind.CLASS, StructureKind.OBJECT -> {
                val requiredProperties = buildList {
                    addAll(listOfNotNull(extraTypeProperty))
                    for (index in 0 until this@toJsonSchemaType.elementsCount) {
                        if (!isElementOptional(index)) {
                            add(getElementName(index))
                        }
                    }
                }

                buildJsonObject {
                    put("type", "object")

                    put("properties", buildJsonObject {
                        if (extraTypeProperty != null) {
                            put(
                                extraTypeProperty, buildJsonObject {
                                    put("const", serialName)
                                    put("default", serialName)
                                }
                            )
                        }
                        for (index in 0 until elementsCount) {
                            put(
                                getElementName(index),
                                getElementDescriptor(index).toJsonSchemaType(processed, serializersModule, definitions)
                            )
                        }
                    })
                    put("additionalProperties", false)
                    put("required", JsonArray(requiredProperties.map { JsonPrimitive(it) }))
                }
            }

            StructureKind.LIST -> {
                jsonObjectOf(
                    "type" to JsonPrimitive("array"),
                    "items" to getElementDescriptor(0).toJsonSchemaType(processed, serializersModule, definitions)
                )
            }

            StructureKind.MAP -> {
                val keysSerializer = getElementDescriptor(0).unwrapInlines()
                val valuesSerializer = getElementDescriptor(1)
                when (keysSerializer.kind) {
                    PrimitiveKind.STRING -> {
                        jsonObjectOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to valuesSerializer.toJsonSchemaType(
                                processed,
                                serializersModule,
                                definitions
                            )
                        )
                    }

                    SerialKind.ENUM -> {
                        jsonObjectOf(
                            "type" to JsonPrimitive("object"),
                            "properties" to JsonObject(
                                (0 until keysSerializer.elementsCount).associate {
                                    keysSerializer.getElementName(it) to valuesSerializer.toJsonSchemaType(
                                        processed,
                                        serializersModule,
                                        definitions
                                    )
                                }
                            ),
                            "additionalProperties" to JsonPrimitive(false)
                        )
                    }

                    else -> error("Unsupported map key: $keysSerializer")
                }
            }
        }
        definitions[id] = if (isNullable) data.orNull() else data
    }
    return jsonObjectOf(
        $$"$ref" to JsonPrimitive($$"#/$defs/$${processed.getValue(key)}")
    )
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.openPolymorphicSubclasses(serializersModule: SerializersModule): List<KSerializer<*>> = buildList {
    serializersModule.dumpTo(object : SerializersModuleCollector {
        override fun <T : Any> contextual(
            kClass: KClass<T>,
            provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>,
        ) {
        }

        override fun <Base : Any, Sub : Base> polymorphic(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>,
        ) {
            if (baseClass == capturedKClass) {
                add(actualSerializer as KSerializer<*>)
            }
        }

        override fun <Base : Any> polymorphicDefaultDeserializer(
            baseClass: KClass<Base>,
            defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?,
        ) {
        }

        override fun <Base : Any> polymorphicDefaultSerializer(
            baseClass: KClass<Base>,
            defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?,
        ) {
        }
    })
}

private fun SerialDescriptor.toJsonSchema(title: String, serializersModule: SerializersModule): JsonElement {
    val definitions = mutableMapOf<String, JsonElement>()
    val mainSchema = toJsonSchemaType(
        processed = mutableMapOf(),
        definitions = definitions,
        serializersModule = serializersModule,
    )
    require(mainSchema.keys.single() == $$"$ref")
    return buildJsonObject {
        put($$"$schema", "https://json-schema.org/draft/2020-12/schema")
        put("title", title)
        put($$"$ref", mainSchema[$$"$ref"]!!)
        put($$"$defs", JsonObject(definitions))
    }
}

interface SchemaGeneratorWorkParameters : WorkParameters {
    val rootClass: Property<String>
    val title: Property<String>
    val outputLocation: RegularFileProperty
}

abstract class SchemaGeneratorWorkAction : WorkAction<SchemaGeneratorWorkParameters> {
    @Suppress("UNCHECKED_CAST")
    private fun <T: Any> KClass<*>.findFunctionByReturnClass(retClass: KClass<T>) = functions.singleOrNull {
        it.parameters.all { it.kind == KParameter.Kind.INSTANCE } && it.returnType.classifier == retClass
    } as? KCallable<T>

    override fun execute() {
        val classLoader = Thread.currentThread().contextClassLoader
        val clazz = classLoader.loadClass(parameters.rootClass.get()).kotlin
        val companion = clazz.companionObject
        val moduleMethod = companion?.findFunctionByReturnClass(SerializersModule::class)
        val serializersModule = moduleMethod?.call(companion.objectInstance) ?: EmptySerializersModule()
        val serializer = serializer(clazz.starProjectedType).descriptor
        val json = Json { prettyPrint = true }
        val schema = json.encodeToString(serializer.toJsonSchema(parameters.title.get(), serializersModule)) + "\n"
        parameters.outputLocation.get().asFile.writeText(schema)
    }
}
