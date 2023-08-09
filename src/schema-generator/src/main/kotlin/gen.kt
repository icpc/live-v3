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
    PrimitiveKind.BYTE -> "integer"
    PrimitiveKind.CHAR -> "integer"
    PrimitiveKind.DOUBLE -> "number"
    PrimitiveKind.FLOAT -> "number"
    PrimitiveKind.INT -> "integer"
    PrimitiveKind.LONG -> "integer"
    PrimitiveKind.SHORT -> "integer"
    PrimitiveKind.STRING -> "string"
}

fun SerialDescriptor.toJsonSchemaType(extras: Map<String, JsonElement> = emptyMap(), extraTypeProperty: String? = null) : JsonElement {
    val kind = kind
    val data = when (kind) {
        PolymorphicKind.OPEN -> TODO("Open polymorphic types are not supported")
        SerialKind.CONTEXTUAL -> TODO("Contextual types are not supported")
        PolymorphicKind.SEALED -> {
            require(extraTypeProperty == null)
            val typeFieldName = getElementName(0)
            val contextualDescriptor = getElementDescriptor(1)
            mapOf(
                "oneOf" to JsonArray(
                    contextualDescriptor.elementDescriptors.map {
                        it.toJsonSchemaType(extraTypeProperty = typeFieldName)
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
                return getElementDescriptor(0).toJsonSchemaType(extras, extraTypeProperty)
            }
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(
                        listOfNotNull(extraTypeProperty).associateWith {
                            JsonObject(
                                mapOf(
                                    "const" to JsonPrimitive(
                                        serialName
                                    )
                                )
                            )
                        } +
                                (0 until elementsCount).associate { getElementName(it) to getElementDescriptor(it).toJsonSchemaType() }
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
                "items" to getElementDescriptor(0).toJsonSchemaType()
            )
        }
        StructureKind.MAP -> {
            val keysSerializer = getElementDescriptor(0)
            val valuesSerializaer = getElementDescriptor(1)
            when (keysSerializer.kind) {
                PrimitiveKind.STRING -> {
                    JsonObject(mapOf(
                        "type" to JsonPrimitive("object"),
                        "patternProperties" to JsonObject(
                            mapOf(".*" to valuesSerializaer.toJsonSchemaType())
                        )
                    ))
                }
                SerialKind.ENUM -> {
                    JsonObject(mapOf(
                        "type" to JsonPrimitive("object"),
                        "properties" to JsonObject(
                            (0 until keysSerializer.elementsCount).map {
                                keysSerializer.getElementName(it) to valuesSerializaer.toJsonSchemaType()
                            }.toMap()
                        )
                    ))
                }
                else -> error("Unsupported map key: $keysSerializer")
            }
        }
        StructureKind.OBJECT -> TODO("Object types are not supported")
    }
    return JsonObject(extras + data)
}

fun SerialDescriptor.toJsonSchema(title: String, path: String) : JsonElement {
    return toJsonSchemaType(
        extras = mapOf(
            "\$schema" to JsonPrimitive("https://json-schema.org/draft/2020-12/schema"),
            "\$id" to JsonPrimitive("https://github.com/icpc/live-v3/blob/main/$path"),
            "title" to JsonPrimitive(title)
        )
    )
}


private val json = Json {
    prettyPrint = true
}

class GenCommand: CliktCommand() {
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