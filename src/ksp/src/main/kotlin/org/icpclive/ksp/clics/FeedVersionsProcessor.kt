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
                genetateVersionObjectClass(feedVersion, obj)
            }
        }
        for (obj in objects.filter { it.eventType != EventType.NO }) {
            generator.generateFile(
                dependencies = Dependencies(true, obj.containingFile),
                packageName = "org.icpclive.clics.events",
                fileName = obj.eventName
            ) {
                if (obj.eventType == EventType.ID) {
                    +"public interface ${obj.eventName} : IdEvent<${obj.qualifiedName}>"
                    +"public interface Batch${obj.eventName} : BatchEvent<${obj.qualifiedName}>"
                } else {
                    +"public interface ${obj.eventName} : GlobalEvent<${obj.qualifiedName}>"

                }
            }

            for (feedVersion in FeedVersion.entries.filter { it >= obj.sinceVersion }) {
                generateVersionEventClass(feedVersion, obj)
            }
        }
        allObjects.addAll(objects)
        return ret
    }

    private fun String.toCamelCase() : String {
        return replace(Regex("[A-Z]")) { "_${it.value.lowercase()}" }
    }

    @OptIn(KspExperimental::class)
    private fun genetateVersionObjectClass(feedVersion: FeedVersion, obj: ObjectDescription) {
        fun KSPropertyDeclaration.hiddenIn(feedVersion: FeedVersion) =
            getAnnotationsByType(SinceClics::class).any { it.feedVersion > feedVersion }

        fun KSPropertyDeclaration.inlinedIn(feedVersion: FeedVersion) =
            getAnnotationsByType(InlinedBefore::class).any { it.feedVersion > feedVersion }

        fun process(
            s: Sequence<KSPropertyDeclaration>,
            prefix: String
        ): Sequence<Pair<KSPropertyDeclaration, String>> = sequence {
            for (property in s) {
                if (property.hiddenIn(feedVersion)) continue
                if (property.inlinedIn(feedVersion)) {
                    val ann = property.getAnnotationsByType(InlinedBefore::class).single()
                    yieldAll(process((property.type.resolve().declaration as KSClassDeclaration).getAllProperties(), prefix + ann.prefix))
                    continue
                }
                yield(property to "$prefix${property.simpleName.asString().toCamelCase()}")
            }
        }

        val serialProperties = process(obj.declaration.getAllProperties(), "").toList()

        generator.generateFile(
            dependencies = Dependencies(true, obj.containingFile),
            packageName = "org.icpclive.clics.${feedVersion.packageName}.objects",
            fileName = obj.simpleName
        ) {
            imports("kotlinx.serialization.*", "kotlinx.datetime.*", "kotlin.time.*")

            +"@Serializable"
            withParameters(
                "public ${if (serialProperties.isNotEmpty()) "data " else ""}class ${obj.simpleName}",
                serialProperties,
                { (i, serialName) ->
                    +"@SerialName(\"$serialName\")"
                    when (i.type.resolve().declaration.qualifiedName!!.asString()) {
                        "org.icpclive.clics.Url" -> +"@Contextual"
                        "kotlinx.datetime.Instant" -> serializable("org.icpclive.clics.time.InstantSerializer")
                        "kotlin.time.Duration" -> {
                            val longBefore = i.getAnnotationsByType(LongMinutesBefore::class).singleOrNull()
                            if (longBefore != null && feedVersion < longBefore.feedVersion) {
                                serializable("org.icpclive.cds.util.serializers.DurationInMinutesSerializer")
                            } else {
                                serializable("org.icpclive.clics.time.DurationSerializer")
                            }
                        }
                    }
                    if (i.parentDeclaration == obj.declaration) {
                        property(
                            listOf(Modifier.OVERRIDE),
                            i.simpleName.asString(),
                            i.type.resolve().render(feedVersion),
                            if (!i.isAnnotationPresent(Required::class)) {
                                defaultTypeValue(i.type.resolve(), i.qualifiedName!!.asString())
                            } else null
                        )
                    } else {
                        property(emptyList(), "_${i.simpleName.asString()}", i.type.resolve().render(feedVersion))
                    }
                },
                " : ${obj.qualifiedName}"
            ) {
                for (i in obj.declaration.getAllProperties()) {
                    fun defaultProperty(i: KSPropertyDeclaration) {
                        val type = i.type.resolve()
                        +"override val ${i.simpleName.asString()}: ${type.render(feedVersion)} get() = ${defaultTypeValue(type, i.qualifiedName!!.asString())}"
                    }
                    if (i.hiddenIn(feedVersion)) {
                        defaultProperty(i)
                    }
                    if (i.inlinedIn(feedVersion)) {
                        +"@Transient"
                        val renderedType = i.type.resolve().makeNotNullable().render(null)
                        withCodeBlock("override val ${i.simpleName.asString()}: $renderedType = object : $renderedType") {
                            for (j in (i.type.resolve().declaration as KSClassDeclaration).getAllProperties()) {
                                if (j.hiddenIn(feedVersion)) {
                                    defaultProperty(j)
                                } else {
                                    +"override val ${j.simpleName.asString()} get() = _${j.simpleName.asString()}"
                                }
                            }
                        }
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


    private fun generateVersionEventClass(
        feedVersion: FeedVersion,
        obj: ObjectDescription
    ) {
        generator.generateFile(
            dependencies = Dependencies(true, obj.containingFile),
            packageName = "org.icpclive.clics.${feedVersion.packageName}.events",
            fileName = obj.eventName
        ) {
            imports("kotlinx.serialization.*")
            +"@Serializable"
            withParameters(
                "public class ${obj.eventName}",
                buildList {
                    if (feedVersion != FeedVersion.`2020_03`) {
                        if (obj.eventType == EventType.ID) {
                            add("override val id: String")
                        }
                        add("@Contextual override val token: org.icpclive.clics.events.EventToken? = null")
                        add("@Contextual override val data: ${obj.qualifiedName}?")
                    } else {
                        add("@SerialName(\"id\") @Contextual override val token: org.icpclive.clics.events.EventToken? = null")
                        add("private val op: Operation")
                        add("@Contextual @SerialName(\"data\") private val _data: ${obj.qualifiedName}")
                    }
                },
                { +it },
                end = " : org.icpclive.clics.events.${obj.eventName}"
            ) {
                if (feedVersion == FeedVersion.`2020_03`) {
                    property(listOf(Modifier.OVERRIDE), "data", "${obj.qualifiedName}?") {
                        +"get() = _data.takeIf { op != Operation.DELETE }"
                    }
                    if (obj.eventType == EventType.ID) {
                        property(listOf(Modifier.OVERRIDE), "id", "String") {
                            +"get() = _data.id"
                        }
                    }
                }
            }
            if (feedVersion >= FeedVersion.`2022_07` && obj.eventType == EventType.ID) {
                +"@Serializable"
                withParameters(
                    "public class ${obj.batchEventName}",
                    buildList {
                        add("@Contextual override val token: org.icpclive.clics.events.EventToken? = null")
                        add("override val data: kotlin.collections.List<@Contextual ${obj.qualifiedName}>")
                    },
                    { +it },
                    end = " : org.icpclive.clics.events.${obj.batchEventName}"
                )
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
                    "org.icpclive.cds.util.*", "kotlinx.serialization.json.*"
                )
                val goodObjects = allObjects.filter { it.sinceVersion <= feedVersion }
                val objects = goodObjects.map {
                    "org.icpclive.clics.${feedVersion.packageName}.objects.${it.simpleName}" to "org.icpclive.clics.objects.${it.simpleName}"
                }
                val events = buildList {
                    for (i in goodObjects) {
                        if (i.eventType != EventType.NO) {
                            add("org.icpclive.clics.${feedVersion.packageName}.events.${i.eventName}" to "org.icpclive.clics.events.${i.eventName}")
                        }
                        if (i.eventType == EventType.ID && feedVersion != FeedVersion.`2020_03`) {
                            add("org.icpclive.clics.${feedVersion.packageName}.events.${i.batchEventName}" to "org.icpclive.clics.events.${i.batchEventName}")
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
                                        +"\"$name\" -> org.icpclive.clics.${feedVersion.packageName}.events.${if (obj.eventType == EventType.ID) obj.batchEventName else obj.eventName}.serializer()"
                                    }
                                }
                                +"else -> throw SerializationException(\"Unknown event type \${typeElement.content}\")"
                            }
                        }
                        withCodeBlock("return when (typeElement.content)") {
                            for (obj in goodObjects) {
                                if (obj.eventType == EventType.NO) continue
                                for (name in obj.names) {
                                    +"\"$name\" -> org.icpclive.clics.${feedVersion.packageName}.events.${obj.eventName}.serializer()"
                                }
                            }
                            +"else -> throw SerializationException(\"Unknown event type \${typeElement.content}\")"
                        }
                    }
                }
                +"@Suppress(\"UNCHECKED_CAST\")"
                withCodeBlock("internal fun serializersModule(): SerializersModule = SerializersModule") {
                    for ((childClass, superClass) in objects + events) {
                        withCodeBlock("contextual(${superClass}::class)") {
                            +"${childClass}.serializer() as KSerializer<org.icpclive.clics.events.Event>"
                        }
                    }
                    withCodeBlock("polymorphicDefaultSerializer(org.icpclive.clics.events.Event::class)") {
                        withCodeBlock("when (it)") {
                            for ((childClass, _) in events) {
                                +"is $childClass -> $childClass.serializer() as SerializationStrategy<org.icpclive.clics.events.Event>"
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