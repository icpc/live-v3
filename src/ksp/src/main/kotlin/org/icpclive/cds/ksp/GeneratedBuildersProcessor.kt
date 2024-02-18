package org.icpclive.cds.ksp

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import kotlinx.serialization.*
import java.io.PrintWriter

class GeneratedBuildersProcessor(private val generator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    @OptIn(KspExperimental::class)
    private fun KSType.render(allowImpls: Boolean) : String = buildString {
        if (allowImpls && declaration.isAnnotationPresent(Builder::class)) {
            append(declaration.qualifiedName?.asString()!! + "Impl")
        } else {
            append(declaration.qualifiedName?.asString()!!)
        }
        val typeArgs = arguments
        if (arguments.isNotEmpty()) {
            append("<")
            append(
                typeArgs.map {
                    val type = it.type?.resolve()
                    "${it.variance.label} ${type?.render(allowImpls)}" +
                            if (type?.nullability == Nullability.NULLABLE) "?" else ""
                }.joinToString(", ")
            )
            append(">")
        }
        if (isMarkedNullable) append("?")
    }
    private val allGeneratedFiles = mutableListOf<String>()

    @OptIn(KspExperimental::class)
    private fun PrintWriter.printMembers(members: List<KSPropertyDeclaration>) {
        for (member in members) {
            val type = member.type.resolve().declaration.qualifiedName?.asString()
            val memberName = member.simpleName.asString()
            var serialName = memberName
            member.getAnnotationsByType(SerialName::class).singleOrNull()?.let {
                serialName = it.value
            }
            when (type) {
                "org.icpclive.cds.settings.Credential", "org.icpclive.cds.settings.UrlOrLocalPath" -> {
                    println("  @Contextual")
                }

                "kotlin.text.Regex" -> {
                    println("  @Serializable(with = RegexSerializer::class)")
                }

                "java.awt.Color" -> {
                    println("  @Serializable(with = ColorSerializer::class)")
                }

                "kotlinx.datetime.Instant" -> {
                    when {
                        member.isAnnotationPresent(Human::class) -> {
                            println("  @Serializable(with = HumanTimeSerializer::class)")
                        }

                        member.isAnnotationPresent(UnixMilliSeconds::class) -> {
                            println("  @Serializable(with = UnixMillisecondsSerializer::class)")
                            serialName = "${serialName}UnixMs"
                        }

                        member.isAnnotationPresent(UnixSeconds::class) -> {
                            println("  @Serializable(with = UnixSecondsSerializer::class)")
                            serialName = "${serialName}UnixSeconds"
                        }

                        else -> {
                            logger.error("No known serializer for ${member} in ${member.closestClassDeclaration()!!.simpleName}: add @Human or @UnixSeconds or @UnixMilliSeconds", member)
                        }
                    }
                }

                "kotlin.time.Duration" -> {
                    when {
                        member.isAnnotationPresent(Seconds::class) -> {
                            println("  @Serializable(with = DurationInSecondsSerializer::class)")
                            serialName = "${serialName}Seconds"
                        }

                        member.isAnnotationPresent(MilliSeconds::class) -> {
                            println("  @Serializable(with = DurationInMillisecondsSerializer::class)")
                            serialName = "${serialName}Ms"
                        }

                        else -> {
                            logger.error("No known serializer: add @Seconds or @MilliSeconds", member)
                        }
                    }
                }

                "kotlinx.datetime.TimeZone" -> {
                    println("  @Serializable(with = TimeZoneSerializer::class)")
                }
            }
            if (member.isAbstract() || member.isAnnotationPresent(AlwaysSerialize::class)) {
                println("  @SerialName(\"$serialName\") override val $memberName: ${member.type.resolve().render(true)}")
            } else {
                println("  @SerialName(\"$serialName\") override var $memberName: ${member.type.resolve().render(true)} = super.${memberName}")
                println("     private set")
            }
        }
    }


    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val toGenerate = resolver
            .getSymbolsWithAnnotation(Builder::class.qualifiedName!!)
        val ret = toGenerate.filter { !it.validate() }.toList()
        val interfacesToImplement = toGenerate
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                when {
                    Modifier.SEALED !in it.modifiers -> {
                        logger.error("${it.qualifiedName!!.asString()}: Class annotatated must @Builder must be sealed", it)
                        false
                    }
                    else -> true
                }
            }.toList()
        for (iface in interfacesToImplement) {
            val packageName = iface.packageName.asString()
            val ifaceName = iface.simpleName.asString()
            val builderName = "${ifaceName}Builder"
            val className = "${ifaceName}Impl"
            PrintWriter(
                generator.createNewFile(
                    dependencies = Dependencies(true, iface.containingFile!!),
                    packageName = packageName,
                    fileName = className
                )
            ).use {
                with(it) {
                    val allProperties = iface.getAllProperties().filter { it.isOpen() || it.isAbstract() }.toList()
                    val (abstractMembers, nonAbstractMembers) = allProperties.partition { it.isAbstract() }

                    println("package $packageName")
                    println()
                    println("import kotlinx.serialization.*")
                    println("import kotlinx.serialization.json.*")
                    println("import kotlinx.serialization.encoding.*")
                    println("import org.icpclive.util.*")
                    println()
                    println("@Serializable")
                    val classSerialName = iface.getAnnotationsByType(Builder::class).single().name.takeUnless { it.isEmpty() } ?: ifaceName
                    println("@SerialName(\"$classSerialName\")")
                    println("internal class $className : ${iface.qualifiedName!!.asString()} {")
                    it.printMembers(allProperties)

                    generateContructor(allProperties)

                    println("  override fun toString() = Json.encodeToString(this)")
                    println("  override fun equals(other: Any?): Boolean {")
                    println("    if (other !is ${className}) return false")
                    for (member in allProperties) {
                        println("    if (${member.simpleName.asString()} != other.${member.simpleName.asString()}) return false")
                    }
                    println("    return true")
                    println("  } ")

                    println("  override fun hashCode(): Int {")
                    println("    var hashCode = 0")
                    for (member in allProperties) {
                        println("    hashCode = hashCode * 31 + ${member.simpleName.asString()}.hashCode()")
                    }
                    println("    return hashCode")
                    println("  } ")


                    println("}")
                    println()

                    println("@Suppress(\"UNCHECKED_CAST\")")
                    println("public class $builderName : $ifaceName {")
                    for (member in abstractMembers) {
                        println("  override public var ${member.simpleName.asString()}: ${member.type.resolve().render(false)}")
                    }
                    for (member in nonAbstractMembers) {
                        println("  private var ${member.simpleName.asString()}_: ${member.type.resolve().makeNullable().render(false)} = null")
                        println("  override public var ${member.simpleName.asString()}: ${member.type.resolve().render(false)}")
                        println("    get() = ${member.simpleName.asString()}_ ?: super.${member.simpleName.asString()}")
                        println("    set(value) { ${member.simpleName.asString()}_ = value }")
                    }
                    println()
                    println("  public constructor(")
                    for (member in abstractMembers) {
                        if (!member.type.resolve().isMarkedNullable) {
                            println("    ${member.simpleName.asString()}: ${member.type.resolve().render(false)},")
                        }
                    }
                    println("  ) {")
                    for (member in abstractMembers) {
                        if (!member.type.resolve().isMarkedNullable) {
                            println("    this.${member.simpleName.asString()} = ${member.simpleName.asString()}")
                        } else {
                            println("    this.${member.simpleName.asString()} = null")
                        }
                    }
                    println("  }")


                    println("  public constructor(from: $ifaceName) {")
                    for (member in allProperties) {
                        println("    ${member.simpleName.asString()} = from.${member.simpleName.asString()}")
                    }
                    println("  }")


                    println("  public fun build() : ${ifaceName} = ${className}(")
                    for (member in allProperties) {
                        val cast = if (member.type.resolve().render(false) != member.type.resolve().render(true)) {
                            " as ${member.type.resolve().render(true)}"
                        } else {
                            ""
                        }
                        println("    ${member.simpleName.asString()}$cast,")
                    }
                    println("  )")
                    println("}")
                    println()
                    println("public inline fun ${ifaceName}(")
                    for (member in abstractMembers) {
                        if (!member.type.resolve().isMarkedNullable) {
                            println("  ${member.simpleName.asString()}: ${member.type.resolve().render(false)},")
                        }
                    }
                    println("  initializer: ${builderName}.() -> Unit = {},")
                    println(") : ${ifaceName} = ${builderName}(")
                    for (member in abstractMembers) {
                        if (!member.type.resolve().isMarkedNullable) {
                            println("  ${member.simpleName.asString()},")
                        }
                    }
                    println(").apply { initializer() }.build()")
                    println()

                    println("public inline fun ${ifaceName}.copy(initializer: ${builderName}.() -> Unit = {}): ${ifaceName} =")
                    println("    ${builderName}(this).apply { initializer() }.build()")

                    println()
                }
            }
            allGeneratedFiles.add("$packageName.${className}")
        }
        return ret
    }

    private fun PrintWriter.generateContructor(abstractMembers: List<KSPropertyDeclaration>) {
        println("  internal constructor(")
        for (member in abstractMembers) {
            println("    ${member.simpleName.asString()}: ${member.type.resolve().render(true)},")
        }
        println("  ) {")
        for (member in abstractMembers) {
            println("    this.${member.simpleName.asString()} = ${member.simpleName.asString()}")
        }
        println("  }")
    }
}

class GeneratedBuildersProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) : SymbolProcessor {
        return GeneratedBuildersProcessor(environment.codeGenerator, environment.logger)
    }
}