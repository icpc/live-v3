@file:OptIn(ExperimentalSerializationApi::class)

package org.icpclive.generator.schema


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.io.File

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

@OptIn(ExperimentalSerializationApi::class)
fun SerialDescriptor.toJsonSchemaType(
    processed: MutableSet<String>,
    definitions: MutableMap<String, JsonElement>,
    extras: Map<String, JsonElement> = emptyMap(),
    extraTypeProperty: String? = null,
    title: String? = null,
    plain: Boolean = false,
): JsonElement {
    // Before processing, check for recursion
    val paramNamesWithTypes = elementDescriptors.map { it.serialName }
    val name = serialName + if (paramNamesWithTypes.isEmpty()) "" else "<${paramNamesWithTypes.joinToString(",")}>"
    val id = title ?: name

    if (!plain && processed.contains(id)) {
        return JsonObject(mapOf("\$ref" to JsonPrimitive("#/\$defs/$id")))
    }

    processed.add(id)
    val data = when (val kind = kind) {
        PolymorphicKind.OPEN -> TODO("Open polymorphic types are not supported")
        SerialKind.CONTEXTUAL -> TODO("Contextual types are not supported")
        PolymorphicKind.SEALED -> {
            require(extraTypeProperty == null)
            val typeFieldName = getElementName(0)
            val contextualDescriptor = getElementDescriptor(1)
            mapOf(
                "oneOf" to JsonArray(
                    contextualDescriptor.elementDescriptors
                        .sortedBy { it.serialName }
                        .map {
                            it.toJsonSchemaType(
                                processed,
                                definitions,
                                plain = plain,
                                title = it.serialName.split(".").last(),
                                extraTypeProperty = typeFieldName
                            )
                        }
                )
            )
        }

        is PrimitiveKind -> {
            require(extraTypeProperty == null)
            mapOf("type" to JsonPrimitive(kind.toJsonTypeName()))
        }

        SerialKind.ENUM -> {
            require(extraTypeProperty == null)
            mapOf("enum" to JsonArray(elementNames.map { JsonPrimitive(it) }))
        }

        StructureKind.CLASS -> {
            if (isInline) {
                return getElementDescriptor(0).toJsonSchemaType(
                    processed,
                    definitions,
                    extras,
                    extraTypeProperty,
                    plain = plain
                )
            }
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
                                            getElementDescriptor(it).toJsonSchemaType(
                                                processed,
                                                definitions,
                                                plain = plain
                                            )
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
                "items" to getElementDescriptor(0).toJsonSchemaType(processed, definitions, plain = plain)
            )
        }

        StructureKind.MAP -> {
            val keysSerializer = getElementDescriptor(0)
            val valuesSerializer = getElementDescriptor(1)
            when (keysSerializer.kind) {
                PrimitiveKind.STRING -> JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("object"),
                        "additionalProperties" to valuesSerializer.toJsonSchemaType(
                            processed,
                            definitions,
                            plain = true
                        )
                    )
                )

                SerialKind.ENUM -> {
                    JsonObject(mapOf(
                        "type" to JsonPrimitive("object"),
                        "properties" to JsonObject(
                            (0 until keysSerializer.elementsCount).associate {
                                keysSerializer.getElementName(it) to valuesSerializer.toJsonSchemaType(
                                    processed,
                                    definitions,
                                    plain = true
                                )
                            }
                        )
                    ))
                }

                else -> error("Unsupported map key: $keysSerializer")
            }
        }

        StructureKind.OBJECT -> TODO("Object types are not supported")
    }
    val content = (extras + data).toMutableMap()
    if (title != null) {
        content["title"] = JsonPrimitive(title)
    }
    definitions[id] = JsonObject(content)
    return if (plain)
        JsonObject(content)
    else
        JsonObject(mapOf("\$ref" to JsonPrimitive("#/\$defs/$id")))
}

fun SerialDescriptor.toJsonSchema(title: String, path: String): JsonElement {
    val definitions = mutableMapOf<String, JsonElement>()
    val mainSchema = toJsonSchemaType(
        title = title,
        processed = mutableSetOf(),
        definitions = definitions,
    )
    return JsonObject(
        (mainSchema as JsonObject) +
                mapOf(
                    "\$schema" to JsonPrimitive("https://json-schema.org/draft/2020-12/schema"),
                    "\$id" to JsonPrimitive("https://github.com/icpc/live-v3/blob/main/$path"),
                    "\$defs" to JsonObject(definitions),
                )
    )
}


private val json = Json {
    prettyPrint = true
}

class GenCommand : CliktCommand() {
    private val className by argument(help = "Class name for which schema should be generated")
    private val output by option("--output", "-o", help = "File to print output").required()
    private val title by option("--title", "-t", help = "Title inside schema file").required()

    override fun run() {
        val serializer = serializer(Class.forName(className)).descriptor
        val schema = json.encodeToString(serializer.toJsonSchema(title, output))
        File(output).printWriter().use {
            it.println(schema)
        }
    }
}

fun main(args: Array<String>) {
    GenCommand().main(args)
}