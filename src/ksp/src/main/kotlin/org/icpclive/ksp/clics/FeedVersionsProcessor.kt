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

    private val objects = mutableMapOf<FeedVersion, MutableList<String>>()
    private val alternativeNames = mutableMapOf<FeedVersion, MutableMap<String, String>>()
    private val events = mutableMapOf<FeedVersion, MutableList<String>>()
    private val files = mutableListOf<KSFile>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val ifaces = resolver.getSymbolsWithAnnotation(SinceClics::class.qualifiedName!!).filterIsInstance<KSClassDeclaration>()
        val (toProcess, ret) = ifaces.partition { it.validate() }

        files.addAll(toProcess.map { it.containingFile!! })

        for (obj in toProcess) {
            val isNoEvent = obj.isAnnotationPresent(NoEvent::class)
            val isIdEvent = obj.getAllProperties().any { it.simpleName.asString() == "id" } && obj.simpleName.asString() != "Contest"
            val superName = if (isIdEvent)
                "IdEvent"
            else
                "GlobalEvent"
            val eventSubtree = when {
                obj.isAnnotationPresent(UpdateContestEvent::class) -> "UpdateContestEvent"
                obj.isAnnotationPresent(UpdateRunEvent::class) -> "UpdateRunEvent"
                else -> "Event"
            }

            val eventName = "${obj.simpleName.asString()}Event"

            if (!isNoEvent) generateEventInterface(obj, eventName, eventSubtree, superName)

            val sinceValue = obj.getAnnotationsByType(SinceClics::class).single()

            for (feedVersion in FeedVersion.entries) {
                if (sinceValue.feedVersion > feedVersion) continue
                if (!isNoEvent) generateVersionEventClass(obj, feedVersion, eventName, isIdEvent)

                genetateVersionObjectClass(feedVersion, obj)
            }
        }
        return ret
    }

    private fun String.toCamelCase() : String {
        return replace(Regex("[A-Z]")) { "_${it.value.lowercase()}" }
    }

    @OptIn(KspExperimental::class)
    private fun genetateVersionObjectClass(feedVersion: FeedVersion, obj: KSClassDeclaration) {
        objects.getOrPut(feedVersion) { mutableListOf() }.add(obj.simpleName.asString())
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

        val serialProperties = process(obj.getAllProperties(), "").toList()

        generator.generateFile(
            dependencies = Dependencies(true, obj.containingFile!!),
            packageName = "org.icpclive.clics.${feedVersion.packageName}.objects",
            fileName = obj.simpleName.asString()
        ) {
            imports("kotlinx.serialization.*", "kotlinx.datetime.*", "kotlin.time.*", "java.awt.Color")

            +"@Serializable"
            withParameters(
                "public ${if (serialProperties.isNotEmpty()) "data " else ""}class ${obj.simpleName.asString()}",
                serialProperties,
                { (i, serialName) ->
                    +"@SerialName(\"$serialName\")"
                    serilizableWith(i.type.resolve())
                    if (i.parentDeclaration == obj) {
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
                " : ${obj.qualifiedName!!.asString()}"
            ) {
                for (i in obj.getAllProperties()) {
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


    @OptIn(KspExperimental::class)
    private fun generateVersionEventClass(
        obj: KSClassDeclaration,
        feedVersion: FeedVersion,
        eventName: String,
        isIdEvent: Boolean,
    ) {
        events.getOrPut(feedVersion) { mutableListOf() }.add(eventName)
        val classSerialNames = obj.getAnnotationsByType(EventSerialName::class).singleOrNull()?.names?.toList()?.takeIf { it.isNotEmpty() } ?: run {
            logger.error("class ${obj} must have @EventSerialName")
            listOf("")
        }
        if (classSerialNames.size > 1) {
            for (i in classSerialNames.drop(1)) {
                alternativeNames.getOrPut(feedVersion) { mutableMapOf() }[i] = "org.icpclive.clics.${feedVersion.packageName}.events.${eventName}"
            }
        }

        generator.generateFile(
            dependencies = Dependencies(true, obj.containingFile!!),
            packageName = "org.icpclive.clics.${feedVersion.packageName}.events",
            fileName = eventName
        ) {
            imports("kotlinx.serialization.*")
            +"@Serializable"
            +"@SerialName(\"${classSerialNames[0]}\")"
            withParameters(
                "public class $eventName",
                buildList {
                    if (feedVersion != FeedVersion.`2020_03`) {
                        if (isIdEvent) {
                            add("override val id: String")
                        }
                        add("override val token: String")
                        add("override val data: ${obj.qualifiedName!!.asString()}?")
                    } else {
                        add("@SerialName(\"id\") override val token: String")
                        add("private val op: Operation")
                        add("@SerialName(\"data\") private val _data: ${obj.qualifiedName!!.asString()}")
                    }
                },
                { +it },
                end = " : org.icpclive.clics.events.$eventName"
            ) {
                if (feedVersion == FeedVersion.`2020_03`) {
                    property(listOf(Modifier.OVERRIDE), "data", "${obj.qualifiedName!!.asString()}?") {
                        +"get() = _data.takeIf { op != Operation.DELETE }"
                    }
                    if (isIdEvent) {
                        property(listOf(Modifier.OVERRIDE), "id", "String") {
                            +"get() = _data.id"
                        }
                    }
                }
            }
        }
    }

    private fun generateEventInterface(obj: KSClassDeclaration, eventName: String, eventSubtree: String, superName: String) {
        generator.generateFile(
            dependencies = Dependencies(true, obj.containingFile!!),
            packageName = "org.icpclive.clics.events",
            fileName = eventName
        ) {
            +"public interface ${eventName} : $eventSubtree, $superName<${obj.qualifiedName!!.asString()}>"
        }
    }

    private fun MyCodeGenerator.serilizableWith(resolve: KSType) {
        when (resolve.declaration.qualifiedName!!.asString()) {
            "java.awt.Color" -> serializable("org.icpclive.cds.util.ColorSerializer")
            "org.icpclive.clics.Url" -> +"@Contextual"
            "kotlinx.datetime.Instant" -> serializable("org.icpclive.clics.ClicsTime.InstantSerializer")
            "kotlin.time.Duration" -> serializable("org.icpclive.clics.ClicsTime.DurationSerializer")
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
                    "org.icpclive.cds.util.*", "org.icpclive.cds.util.datetime.*",
                    "org.icpclive.clics.*"
                )
                withCodeBlock("internal fun serializersModule(): SerializersModule = SerializersModule") {
                    for ((index, i) in objects[feedVersion]?.withIndex() ?: emptyList()) {
                        withCodeBlock("val wrapper$index = object : KSerializer<org.icpclive.clics.objects.${i}>") {
                            +"private val delegate = org.icpclive.clics.${ feedVersion.packageName }.objects.${ i }.serializer()"
                            +"override val descriptor get() = delegate.descriptor"
                            +"override fun deserialize(decoder: Decoder) = delegate.deserialize(decoder)"
                            +"override fun serialize(encoder: Encoder, value: org.icpclive.clics.objects.${i}) = delegate.serialize(encoder, value as org.icpclive.clics.${ feedVersion.packageName }.objects.${i})"
                        }
                        +"polymorphicDefaultDeserializer(org.icpclive.clics.objects.${i}::class) { wrapper$index }"
                        +"polymorphicDefaultSerializer(org.icpclive.clics.objects.${i}::class) { wrapper$index }"
                    }
                    for (i in events[feedVersion] ?: emptyList()) {
                        withCodeBlock("polymorphic(org.icpclive.clics.events.${i}::class)") {
                            +"subclass(org.icpclive.clics.${feedVersion.packageName}.events.${i}::class)"
                        }
                    }
                    withCodeBlock("polymorphic(org.icpclive.clics.events.Event::class)") {
                        for (i in events[feedVersion] ?: emptyList()) {
                            +"subclass(org.icpclive.clics.${feedVersion.packageName}.events.${i}::class)"
                        }
                        withCodeBlock("defaultDeserializer") {
                            withCodeBlock("when (it)") {
                                for ((key, value) in alternativeNames[feedVersion] ?: emptyMap()) {
                                    +"\"$key\" -> ${value}.serializer()"
                                }
                                +"else -> null"
                            }
                        }
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