package org.icpclive.ksp.clics

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.PrintWriter

@OptIn(KspExperimental::class)
private fun KSType.render(feedVersion: FeedVersion?) : String = buildString {
    if (feedVersion != null && declaration.isAnnotationPresent(SinceClics::class)) {
        append("org.icpclive.clics.${feedVersion.packageName}.objects.${declaration.simpleName.asString()}")
    } else {
        append(declaration.qualifiedName?.asString()!!)
    }
    val typeArgs = arguments
    if (arguments.isNotEmpty()) {
        append("<")
        append(
            typeArgs.map {
                val type = it.type?.resolve()
                "${it.variance.label} ${type?.render(feedVersion)}" +
                        if (type?.nullability == Nullability.NULLABLE) "?" else ""
            }.joinToString(", ")
        )
        append(">")
    }
    if (isMarkedNullable) append("?")
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

        PrintWriter(
            generator.createNewFile(
                dependencies = Dependencies(true, obj.containingFile!!),
                packageName = "org.icpclive.clics.${feedVersion.packageName}.objects",
                fileName = obj.simpleName.asString()
            )
        ).use {
            it.println("package org.icpclive.clics.${feedVersion.packageName}.objects")
            it.println()
            it.println("import kotlinx.serialization.*")
            it.println("import kotlinx.datetime.*")
            it.println("import kotlin.time.*")
            it.println("import java.awt.Color")
            it.println()
            it.println("@Serializable")
            it.println("public ${if (serialProperties.isNotEmpty()) "data " else ""}class ${obj.simpleName.asString()}(")
            for ((i, serialName) in serialProperties) {
                it.println("  @SerialName(\"$serialName\")")
                it.serilizableWith(i.type.resolve())
                if (i.parentDeclaration == obj) {
                    it.print("  override val ${i.simpleName.asString()}: ${i.type.resolve().render(feedVersion)}")
                    if (!i.isAnnotationPresent(Required::class)) {
                        it.print(" = ${defaultTypeValue(i.type.resolve(), i.qualifiedName!!.asString())}")
                    }
                    it.println(",")
                } else {
                    it.println("  val _${i.simpleName.asString()}: ${i.type.resolve().render(feedVersion)},")
                }
            }
            it.println(") : ${obj.qualifiedName!!.asString()} {")
            for (i in obj.getAllProperties()) {
                fun PrintWriter.defaultProperty(i: KSPropertyDeclaration) {
                    val type = i.type.resolve()
                    println("  override val ${i.simpleName.asString()}: ${type.render(feedVersion)} get() = ${defaultTypeValue(type, i.qualifiedName!!.asString())}")
                }
                if (i.hiddenIn(feedVersion)) {
                    it.defaultProperty(i)
                }
                if (i.inlinedIn(feedVersion)) {
                    it.println("  @Transient")
                    it.println("  override val ${i.simpleName.asString()}: ${i.type.resolve().makeNotNullable().render(null)} = object : ${i.type.resolve().makeNotNullable().render(null)} {")
                    for (j in (i.type.resolve().declaration as KSClassDeclaration).getAllProperties()) {
                        if (j.hiddenIn(feedVersion)) {
                            it.print("  ")
                            it.defaultProperty(j)
                        } else {
                            it.println("    override val ${j.simpleName.asString()} get() = _${j.simpleName.asString()}")
                        }
                    }
                    it.println("  }")
                }
            }

            it.println("}")
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

        PrintWriter(
            generator.createNewFile(
                dependencies = Dependencies(true, obj.containingFile!!),
                packageName = "org.icpclive.clics.${feedVersion.packageName}.events",
                fileName = eventName
            )
        ).use {
            it.println("package org.icpclive.clics.${feedVersion.packageName}.events")
            it.println()
            it.println("import kotlinx.serialization.*")
            it.println()
            it.println("@Serializable")
            it.println("@SerialName(\"${classSerialNames[0]}\")")
            it.println("public class $eventName (")
            if (feedVersion != FeedVersion.`2020_03`) {
                if (isIdEvent) {
                    it.println("  override val id: String,")
                }
                it.println("  override val token: String,")
                it.println("  override val data: ${obj.qualifiedName!!.asString()}?")
            } else {
                it.println("  @SerialName(\"id\") override val token: String,")
                it.println("  private val op: Operation,")
                it.println("  @SerialName(\"data\") private val _data: ${obj.qualifiedName!!.asString()}")
            }
            it.println(") : org.icpclive.clics.events.$eventName {")
            if (feedVersion == FeedVersion.`2020_03`) {
                it.println("  override val data: ${obj.qualifiedName!!.asString()}?")
                it.println("    get() = _data.takeIf { op != Operation.DELETE }")
                if (isIdEvent) {
                    it.println("  override val id: String get() = _data.id")
                }
            }
            it.println("}")
        }
    }

    private fun generateEventInterface(obj: KSClassDeclaration, eventName: String, eventSubtree: String, superName: String) {
        PrintWriter(
            generator.createNewFile(
                dependencies = Dependencies(true, obj.containingFile!!),
                packageName = "org.icpclive.clics.events",
                fileName = eventName
            )
        ).use {
            it.println("package org.icpclive.clics.events")
            it.println()
            it.println("public interface ${eventName} : $eventSubtree, $superName<${obj.qualifiedName!!.asString()}>")
        }
    }

    private fun PrintWriter.serilizableWith(resolve: KSType) {
        when (resolve.declaration.qualifiedName!!.asString()) {
            "java.awt.Color" -> println("  @Serializable(with = org.icpclive.util.ColorSerializer::class)")
            "org.icpclive.clics.Url" -> println("  @Contextual")
            "kotlinx.datetime.Instant" -> println("  @Serializable(with = org.icpclive.clics.ClicsTime.InstantSerializer::class)")
            "kotlin.time.Duration" -> println("  @Serializable(with = org.icpclive.clics.ClicsTime.DurationSerializer::class)")
        }
    }

    override fun finish() {
        if (files.isEmpty()) return
        for (feedVersion in FeedVersion.entries) {
            PrintWriter(
                generator.createNewFile(
                    Dependencies(true, sources = files.toTypedArray()),
                    "org.icpclive.clics.${feedVersion.packageName}",
                    fileName = "serializersModule"
                )
            ).use {
                it.println("package org.icpclive.clics.${feedVersion.packageName}")
                it.println()
                it.println("import kotlinx.serialization.*")
                it.println("import kotlinx.serialization.encoding.*")
                it.println("import kotlinx.serialization.modules.*")
                it.println("import org.icpclive.util.*")
                it.println("import org.icpclive.clics.*")
                it.println()
                it.println("internal fun serializersModule(): SerializersModule = SerializersModule {")
                for ((index, i) in objects[feedVersion]?.withIndex() ?: emptyList()) {
                    it.println("""
                    |  val wrapper$index = object : KSerializer<org.icpclive.clics.objects.${i}> {  
                    |     private val delegate = org.icpclive.clics.${feedVersion.packageName}.objects.${i}.serializer()
                    |     override val descriptor get() = delegate.descriptor
                    |     override fun deserialize(decoder: Decoder) = delegate.deserialize(decoder)
                    |     override fun serialize(encoder: Encoder, value: org.icpclive.clics.objects.${i}) = delegate.serialize(encoder, value as org.icpclive.clics.${feedVersion.packageName}.objects.${i})
                    |  }
                    |  polymorphicDefaultDeserializer(org.icpclive.clics.objects.${i}::class) { wrapper$index }
                    |  polymorphicDefaultSerializer(org.icpclive.clics.objects.${i}::class) { wrapper$index }
                    """.trimMargin())
                }
                for (i in events[feedVersion] ?: emptyList()) {
                    it.println("  polymorphic(org.icpclive.clics.events.${i}::class) {")
                    it.println("    subclass(org.icpclive.clics.${feedVersion.packageName}.events.${i}::class)")
                    it.println("  }")
                }
                it.println("  polymorphic(org.icpclive.clics.events.Event::class) {")
                for (i in events[feedVersion] ?: emptyList()) {
                    it.println("    subclass(org.icpclive.clics.${feedVersion.packageName}.events.${i}::class)")
                }
                it.println("   defaultDeserializer {")
                it.println("     when (it) {")
                for ((key, value) in alternativeNames[feedVersion] ?: emptyMap()) {
                    it.println("      \"$key\" -> ${value}.serializer()")
                }
                it.println("       else -> null")
                it.println("     }")
                it.println("   }")
                it.println(" }")
                it.println("}")
            }

        }
    }

}


class FeedVersionsProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) : SymbolProcessor {
        return FeedVersionsProcessor(environment.codeGenerator, environment.logger)
    }
}