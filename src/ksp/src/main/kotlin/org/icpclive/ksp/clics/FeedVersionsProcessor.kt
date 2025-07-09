package org.icpclive.ksp.clics

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import org.icpclive.ksp.common.*

@OptIn(KspExperimental::class)
private fun KSType.render(feedVersion: FeedVersion?) : String = render {
    if (feedVersion != null && declaration.isAnnotationPresent(SinceClics::class)) {
        "org.icpclive.clics.${feedVersion.packageName}.objects.${declaration.simpleName.asString()}"
    } else {
        declaration.qualifiedName?.asString()!!
    }
}


class FeedVersionsProcessor(private val generator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {

    enum class EventType {
        NO, ID, GLOBAL,
    }

    class ObjectDescription(
        val names: List<String>,
        val qualifiedName: String,
        val simpleName: String,
        val eventType: EventType,
        val sinceVersion: FeedVersion,
        val eventName: String,
        val batchEventName: String,
        val containingFile: KSFile,
        val declaration: KSClassDeclaration,
    )

    private val files = mutableListOf<KSFile>()
    private val allObjects = mutableListOf<ObjectDescription>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val ifaces = resolver.getSymbolsWithAnnotation(SinceClics::class.qualifiedName!!).filterIsInstance<KSClassDeclaration>()
        val (toProcess, ret) = ifaces.partition { it.validate() }

        files.addAll(toProcess.map { it.containingFile!! })

        val objects = toProcess.map {
            val isNoEvent = it.isAnnotationPresent(NoEvent::class)
            val classSerialNames = it.getAnnotationsByType(EventSerialName::class).singleOrNull()?.names?.toList()?.takeIf { it.isNotEmpty() } ?: run {
                if (!isNoEvent) {
                    logger.error("class ${it} must have @EventSerialName")
                }
                listOf("")
            }
            val isIdEvent = it.getAllProperties().any { it.simpleName.asString() == "id" } && it.simpleName.asString() != "Contest"
            ObjectDescription(
                names = classSerialNames,
                qualifiedName = it.qualifiedName!!.asString(),
                simpleName = it.simpleName.asString(),
                eventType = when {
                    isNoEvent -> EventType.NO
                    isIdEvent -> EventType.ID
                    else -> EventType.GLOBAL
                },
                sinceVersion = it.getAnnotationsByType(SinceClics::class).single().feedVersion,
                eventName = "${it.simpleName.asString()}Event",
                batchEventName = "Batch${it.simpleName.asString()}Event",
                containingFile = it.containingFile!!,
                declaration = it
            )
        }

        for (obj in objects) {
            for (feedVersion in FeedVersion.entries.filter { it >= obj.sinceVersion }) {
                generateVersionObjectSerializer(feedVersion, obj)
            }
        }
        for (obj in objects.filter { it.eventType != EventType.NO }) {
            generator.generateFile(
                dependencies = Dependencies(true, obj.containingFile),
                packageName = "org.icpclive.clics.events",
                fileName = obj.eventName
            ) {
                if (obj.eventType == EventType.ID) {
                    withParameters(
                        "public class ${obj.eventName}",
                        listOf(
                            "override val id: String",
                            "override val token: org.icpclive.clics.events.EventToken?",
                            "override val data: ${obj.qualifiedName}?",
                        ),
                        render = { +it },
                        end = ": IdEvent<${obj.qualifiedName}>"
                    )
                    withParameters(
                        "public class Batch${obj.eventName}",
                        listOf(
                            "override val token: org.icpclive.clics.events.EventToken?",
                            "override val data: kotlin.collections.List<${obj.qualifiedName}>",
                        ),
                        render = { +it },
                        end = ": BatchEvent<${obj.qualifiedName}>"
                    )
                } else {
                    withParameters(
                        "public class ${obj.eventName}",
                        listOf(
                            "override val token: org.icpclive.clics.events.EventToken?",
                            "override val data: ${obj.qualifiedName}",
                        ),
                        render = { +it },
                        end = ": GlobalEvent<${obj.qualifiedName}>"
                    )
                }
            }

        }
        allObjects.addAll(objects)
        return ret
    }

    private fun String.toSnakeCase() : String {
        return replace(Regex("[A-Z]")) { "_${it.value.lowercase()}" }
    }

    private fun KSType.isList() : Boolean {
        return declaration.qualifiedName?.asString() == "kotlin.collections.List"
    }

    @OptIn(KspExperimental::class)
    private fun generateVersionObjectSerializer(feedVersion: FeedVersion, obj: ObjectDescription) {
        fun KSPropertyDeclaration.hiddenIn(feedVersion: FeedVersion) =
            getAnnotationsByType(SinceClics::class).any { it.feedVersion > feedVersion }

        fun KSPropertyDeclaration.inlinedIn(feedVersion: FeedVersion) =
            getAnnotationsByType(InlinedBefore::class).any { it.feedVersion > feedVersion }

        data class SerialProperty(
            val name: String,
            val type: KSType,
            val jsonName: String,
            val accessName: String,
            val annotated: KSAnnotated
        )

        fun process(
            s: Sequence<KSPropertyDeclaration>,
        ): Sequence<SerialProperty> = sequence {
            for (property in s) {
                if (property.hiddenIn(feedVersion)) continue
                if (property.inlinedIn(feedVersion)) {
                    val ann = property.getAnnotationsByType(InlinedBefore::class).single()
                    val isNullable = property.type.resolve().isMarkedNullable
                    yieldAll(
                        process(
                            (property.type.resolve().declaration as KSClassDeclaration).getAllProperties(),
                        ).map {
                            SerialProperty(
                                name = it.name,
                                type = it.type.run { if (isNullable && !isList()) makeNullable() else this },
                                jsonName = ann.prefix + it.jsonName,
                                accessName = property.simpleName.asString() + (if (isNullable) "?." else ".") + it.accessName,
                                annotated = property,
                            )
                        }
                    )
                    continue
                }
                yield(
                    SerialProperty(
                        property.simpleName.asString(),
                        property.type.resolve(),
                        property.simpleName.asString().toSnakeCase(),
                        property.simpleName.asString(),
                        property
                    )
                )
            }
        }

        val serialProperties = process(obj.declaration.getAllProperties()).toList()

        generator.generateFile(
            dependencies = Dependencies(true, obj.containingFile),
            packageName = "org.icpclive.clics.${feedVersion.packageName}.serializers",
            fileName = obj.simpleName
        ) {
            imports("kotlinx.serialization.*",
                "kotlinx.serialization.encoding.*",
                "kotlinx.serialization.descriptors.*",
                "kotlinx.datetime.*",
                "kotlin.time.*",
                "kotlinx.serialization.builtins.ListSerializer"
            )

            fun getSerializer(type: KSType, annotated: KSAnnotated) : String {
                if (type.declaration.isAnnotationPresent(SinceClics::class)) {
                    return "${type.declaration.simpleName.asString()}Serializer"
                }

                return when (type.declaration.qualifiedName!!.asString()) {
                    "org.icpclive.clics.Url" -> "org.icpclive.clics.UrlSerializer"
                    "kotlinx.datetime.Instant" -> "org.icpclive.clics.time.InstantSerializer"
                    "kotlin.time.Duration" -> {
                        val longBefore = annotated.getAnnotationsByType(LongMinutesBefore::class).singleOrNull()
                        if (longBefore != null && feedVersion < longBefore.feedVersion) {
                            "org.icpclive.cds.util.serializers.DurationInMinutesSerializer"
                        } else {
                            "org.icpclive.clics.time.DurationSerializer"
                        }
                    }
                    "kotlin.collections.List" -> {
                       val listType = type.arguments.single().type!!.resolve()
                       "ListSerializer(" + getSerializer(listType, annotated) + ")"
                    }
                    else -> "serializer<${type.render(null)}>()"
                }
            }

            withCodeBlock(
                "public object ${obj.simpleName}Serializer : KSerializer<org.icpclive.clics.objects.${obj.simpleName}>"
            ) {
                for (i in serialProperties) {
                    +"private val ${i.name}SerializerCache = ${getSerializer(i.type.makeNotNullable(), i.annotated)}"
                }
                withCodeBlock("override val descriptor: SerialDescriptor = buildClassSerialDescriptor(\"${feedVersion}.${obj.simpleName}\")") {
                    for (i in serialProperties) {
                        +"element(\"${i.jsonName}\", ${i.name}SerializerCache.descriptor, emptyList(), isOptional = ${i.annotated.isAnnotationPresent(Required::class).not()})"
                    }
                }
                withCodeBlock("override fun serialize(encoder: Encoder, value: org.icpclive.clics.objects.${obj.simpleName})") {
                    withCodeBlock("encoder.encodeStructure(descriptor)") {
                        for ((index, i) in serialProperties.withIndex()) {
                            +"encode${if (i.type.isMarkedNullable) "Nullable" else ""}SerializableElement(descriptor, $index, ${i.name}SerializerCache, value.${i.accessName + if (i.type.isList() && i.accessName.contains("?")) " ?: emptyList()" else ""})"
                        }
                    }
                }
                withCodeBlock("override fun deserialize(decoder: Decoder): org.icpclive.clics.objects.${obj.simpleName}") {
                    withCodeBlock("return decoder.decodeStructure(descriptor)") {
                        for (i in serialProperties) {
                            if (i.type.isList()) {
                                +"var ${i.name}: ${i.type.render(null)} = emptyList()"
                            } else {
                                +"var ${i.name}: ${i.type.makeNullable().render(null)} = null"
                            }
                        }
                        withCodeBlock("while (true)") {
                            withCodeBlock("when (val index = decodeElementIndex(descriptor))") {
                                for ((index, i) in serialProperties.withIndex()) {
                                    +"$index -> ${i.name} = decode${if (i.type.isMarkedNullable) "Nullable" else ""}SerializableElement(descriptor, $index, ${i.name}SerializerCache, ${i.name})"
                                }
                                +"CompositeDecoder.DECODE_DONE -> break"
                                +$$"else -> error(\"Unexpected index: $index\")"
                            }
                        }
                        withParameters(
                            "org.icpclive.clics.objects.${obj.simpleName}",
                            obj.declaration.getAllProperties().toList(),
                            render = { i ->
                                fun defaultProperty(i: KSPropertyDeclaration) {
                                    val type = i.type.resolve()
                                    +"${i.simpleName.asString()} = ${defaultTypeValue(type, i.qualifiedName!!.asString())}"
                                }
                                if (i.hiddenIn(feedVersion)) {
                                    defaultProperty(i)
                                } else if (i.inlinedIn(feedVersion)) {
                                    val renderedType = i.type.resolve().makeNotNullable().render(null)
                                    withParameters("${i.simpleName.asString()} = $renderedType",
                                        (i.type.resolve().declaration as KSClassDeclaration).getAllProperties().toList(),
                                        render= {j ->
                                            if (j.hiddenIn(feedVersion)) {
                                                defaultProperty(j)
                                            } else {
                                                +"${j.simpleName.asString()} = ${j.simpleName.asString()}${
                                                    if (!j.type.resolve().isMarkedNullable && !j.type.resolve().isList()) "!!" else ""
                                                }"
                                            }
                                        })
                                } else {
                                    +"${i.simpleName.asString()} = ${i.simpleName.asString()}${if (!i.type.resolve().isMarkedNullable && !i.type.resolve().isList()) "!!" else ""}"
                                }
                            }
                        )
                    }
                }
            }
        }
    }



    private fun defaultTypeValue(type: KSType, name: String) : String {
        if (type.isMarkedNullable) return "null"
        return when (type.declaration.qualifiedName!!.asString()) {
            "kotlin.collections.List" -> "emptyList()"
            else -> {
                logger.error("Unknown default ${type.declaration.qualifiedName!!.asString()} for $name")
                "TODO()"
            }
        }
    }


    override fun finish() {
        if (files.isEmpty()) return
        for (feedVersion in FeedVersion.entries) {
            generator.generateFile(
                Dependencies(true, sources = files.toTypedArray()),
                "org.icpclive.clics.${feedVersion.packageName}",
                fileName = "serializersModule"
            ) {
                imports(
                    "kotlinx.serialization.*", "kotlinx.serialization.encoding.*", "kotlinx.serialization.modules.*",
                    "org.icpclive.cds.util.serializers.*", "org.icpclive.clics.*",
                    "org.icpclive.cds.util.*", "kotlinx.serialization.json.*",
                    "org.icpclive.clics.events.*"
                )
                val goodObjects = allObjects.filter { it.sinceVersion <= feedVersion }
                val objects = goodObjects.map {
                    "org.icpclive.clics.${feedVersion.packageName}.serializers.${it.simpleName}Serializer" to "org.icpclive.clics.objects.${it.simpleName}"
                }
                val events = buildList {
                    for (i in goodObjects) {
                        val serializer = "Serializer" + if (feedVersion == FeedVersion.`2020_03`) "New" else ""
                        if (i.eventType != EventType.NO) {
                            +"internal object ${i.eventName}$serializer : ${i.eventType.toString().lowercase().replaceFirstChar { it.uppercase() }}EventSerializer<${i.eventName}, ${i.qualifiedName}>(org.icpclive.clics.${feedVersion.packageName}.serializers.${i.simpleName}Serializer, \"${i.names.first()}\", ::${i.eventName})"
                            if (feedVersion == FeedVersion.`2020_03`) {
                                +"internal object ${i.eventName}Serializer : LegacyEventSerializer<${i.eventName}>(${i.eventName}$serializer)"
                            }
                            add("${i.eventName}Serializer" to i.eventName)
                        }
                        if (i.eventType == EventType.ID && feedVersion != FeedVersion.`2020_03`) {
                            +"internal object ${i.batchEventName}Serializer : BatchEventSerializer<${i.batchEventName}, ${i.qualifiedName}>(org.icpclive.clics.${feedVersion.packageName}.serializers.${i.simpleName}Serializer, \"${i.names.first()}\", ::${i.batchEventName})"
                            add("${i.batchEventName}Serializer" to i.batchEventName)
                        }
                    }
                }

                withCodeBlock("private object EventSerializer : JsonContentPolymorphicSerializer<org.icpclive.clics.events.Event>(org.icpclive.clics.events.Event::class)") {
                    withCodeBlock("override fun selectDeserializer(element: JsonElement): DeserializationStrategy<org.icpclive.clics.events.Event>") {
                        +"if (element !is JsonObject) throw SerializationException(\"Event expected to be object\")"
                        +"val typeElement = element[\"type\"] ?: throw SerializationException(\"Event expected to have type\")"
                        +"if (typeElement !is JsonPrimitive || !typeElement.isString) throw SerializationException(\"type expected to be string\")"
                        +"val id = element[\"id\"] ?: JsonNull"
                        withCodeBlock("if (id is JsonNull)") {
                            withCodeBlock("return when (typeElement.content)") {
                                for (obj in goodObjects) {
                                    if (obj.eventType == EventType.NO) continue
                                    if (obj.eventType == EventType.ID && feedVersion == FeedVersion.`2020_03`) continue
                                    for (name in obj.names) {
                                        +"\"$name\" -> ${if (obj.eventType == EventType.ID) obj.batchEventName else obj.eventName}Serializer"
                                    }
                                }
                                +"else -> throw SerializationException(\"Unknown event type \${typeElement.content}\")"
                            }
                        }
                        withCodeBlock("return when (typeElement.content)") {
                            for (obj in goodObjects) {
                                if (obj.eventType == EventType.NO) continue
                                for (name in obj.names) {
                                    +"\"$name\" -> ${obj.eventName}Serializer"
                                }
                            }
                            +"else -> throw SerializationException(\"Unknown event type \${typeElement.content}\")"
                        }
                    }
                }
                +"@Suppress(\"UNCHECKED_CAST\")"
                withCodeBlock("internal fun serializersModule(): SerializersModule = SerializersModule") {
                    for ((serializer, superClass) in objects) {
                        withCodeBlock("contextual(${superClass}::class)") {
                            +serializer
                        }
                    }
                    for ((serializer, superClass) in events) {
                        withCodeBlock("contextual(${superClass}::class)") {
                            +serializer
                        }
                    }
                    withCodeBlock("polymorphicDefaultSerializer(org.icpclive.clics.events.Event::class)") {
                        withCodeBlock("when (it)") {
                            for ((serializer, superClass) in events) {
                                +"is $superClass -> $serializer as KSerializer<Event>"
                            }
                            +"else -> null"
                        }
                    }

                    withCodeBlock("polymorphicDefaultDeserializer(org.icpclive.clics.events.Event::class)") {
                        +"EventSerializer"
                    }
                }
            }
        }
    }

}


class FeedVersionsProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) : SymbolProcessor {
        return FeedVersionsProcessor(environment.codeGenerator, environment.logger)
    }
}
