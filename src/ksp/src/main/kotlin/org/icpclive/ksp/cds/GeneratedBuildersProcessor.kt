package org.icpclive.ksp.cds

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.Modifier
import kotlinx.serialization.*
import org.icpclive.ksp.common.*

class GeneratedBuildersProcessor(private val generator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    @OptIn(KspExperimental::class)
    private fun KSType.render(allowImpls: Boolean) : String = render {
        if (allowImpls && declaration.isAnnotationPresent(Builder::class))
            declaration.qualifiedName?.asString()!! + "Impl"
        else
            declaration.qualifiedName?.asString()!!
    }

    @OptIn(KspExperimental::class)
    private fun MyCodeGenerator.generateMembers(members: List<KSPropertyDeclaration>) {
        for (member in members) {
            val type = member.type.resolve().declaration.qualifiedName?.asString()
            val memberName = member.simpleName.asString()
            var serialName = memberName
            member.getAnnotationsByType(SerialName::class).singleOrNull()?.let {
                serialName = it.value
            }
            when (type) {
                "org.icpclive.cds.settings.Credential", "org.icpclive.cds.settings.UrlOrLocalPath" -> +"@Contextual"

                "kotlin.text.Regex" -> serializable("RegexSerializer")

                "java.awt.Color" -> serializable("ColorSerializer")

                "kotlinx.datetime.Instant" -> {
                    when {
                        member.isAnnotationPresent(Human::class) -> serializable("HumanTimeSerializer")
                        member.isAnnotationPresent(UnixMilliSeconds::class) -> {
                            serializable("UnixMillisecondsSerializer")
                            serialName = "${serialName}UnixMs"
                        }

                        member.isAnnotationPresent(UnixSeconds::class) -> {
                            serializable("UnixSecondsSerializer")
                            serialName = "${serialName}UnixSeconds"
                        }

                        else -> logger.error(
                            "No known serializer for ${member} in ${member.closestClassDeclaration()!!.simpleName}: add @Human or @UnixSeconds or @UnixMilliSeconds",
                            member
                        )
                    }
                }

                "kotlin.time.Duration" -> {
                    when {
                        member.isAnnotationPresent(Seconds::class) -> {
                            serializable("DurationInSecondsSerializer")
                            serialName = "${serialName}Seconds"
                        }

                        member.isAnnotationPresent(MilliSeconds::class) -> {
                            +"  @Serializable(with = DurationInMillisecondsSerializer::class)"
                            serialName = "${serialName}Ms"
                        }

                        else -> {
                            logger.error("No known serializer: add @Seconds or @MilliSeconds", member)
                        }
                    }
                }

                "kotlinx.datetime.TimeZone" -> serializable("TimeZoneSerializer")
            }
            +"@SerialName(\"$serialName\")"
            if (member.isAbstract() || member.isAnnotationPresent(AlwaysSerialize::class)) {
                property(listOf(Modifier.OVERRIDE), memberName, member.type.resolve().render(true))
            } else {
                mutableProperty(listOf(Modifier.OVERRIDE), memberName, member.type.resolve().render(true), "super.${memberName}") {
                    +"private set"
                }
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
            generator.generateFile(
                dependencies = Dependencies(true, iface.containingFile!!),
                packageName = packageName,
                fileName = className
            ) {
                val allProperties = iface.getAllProperties().filter { it.isOpen() || it.isAbstract() }.toList()
                val (abstractMembers, nonAbstractMembers) = allProperties.partition { it.isAbstract() }
                val notNullAbstractMembers = abstractMembers.filterNot { it.type.resolve().isMarkedNullable }
                val classSerialName = iface.getAnnotationsByType(Builder::class).single().name.takeUnless { it.isEmpty() } ?: ifaceName

                imports("kotlinx.serialization.*", "kotlinx.serialization.json.*", "kotlinx.serialization.encoding.*", "org.icpclive.util.*")

                +"@Serializable"
                +"@SerialName(\"$classSerialName\")"
                withCodeBlock("internal class $className : ${iface.qualifiedName!!.asString()}") {
                    generateMembers(allProperties)
                    +""
                    generateConstructor(allProperties)
                    +""

                    withCodeBlock("override fun toString() : String") {
                        ret("Json.encodeToString(this)")
                    }
                    withCodeBlock("override fun equals(other: Any?): Boolean") {
                        If("other !is ${className}") {
                            ret("false")
                        }
                        for (member in allProperties) {
                            If("${member.simpleName.asString()} != other.${member.simpleName.asString()}") {
                                ret("false")
                            }
                        }
                        ret("true")
                    }

                    withCodeBlock("override fun hashCode(): Int") {
                        +"var hashCode = 0"
                        for (member in allProperties) {
                            +"hashCode = hashCode * 31 + ${member.simpleName.asString()}.hashCode()"
                        }
                        ret("hashCode")
                    }
                }

                +"@Suppress(\"UNCHECKED_CAST\")"
                withCodeBlock("public class $builderName : $ifaceName") {
                    for (member in abstractMembers) {
                        mutableProperty(listOf(Modifier.PUBLIC, Modifier.OVERRIDE), member.simpleName.asString(), member.type.resolve().render(false))
                    }

                    for (member in nonAbstractMembers) {
                        mutableProperty(listOf(Modifier.PRIVATE), member.simpleName.asString() + "_", member.type.resolve().makeNullable().render(false), "null")
                        mutableProperty(listOf(Modifier.PUBLIC, Modifier.OVERRIDE), member.simpleName.asString(), member.type.resolve().render(false)) {
                            +"get() = ${member.simpleName.asString()}_ ?: super.${member.simpleName.asString()}"
                            +"set(value) { ${member.simpleName.asString()}_ = value }"
                        }
                    }
                    +""
                    withParameters("public constructor",
                        abstractMembers.filter { !it.type.resolve().isMarkedNullable },
                        { +"${it.simpleName.asString()}: ${it.type.resolve().render(false)}" }
                    ) {
                        for (member in abstractMembers) {
                            if (!member.type.resolve().isMarkedNullable) {
                                +"this.${member.simpleName.asString()} = ${member.simpleName.asString()}"
                            } else {
                                +"this.${member.simpleName.asString()} = null"
                            }
                        }
                    }

                    withCodeBlock("public constructor(from: $ifaceName)") {
                        for (member in allProperties) {
                            +"${member.simpleName.asString()} = from.${member.simpleName.asString()}"
                        }
                    }

                    withParameters("public fun build() : ${ifaceName} = ${className}", allProperties, { member ->
                        val cast = if (member.type.resolve().render(false) != member.type.resolve().render(true)) {
                            " as ${member.type.resolve().render(true)}"
                        } else {
                            ""
                        }
                        +"${member.simpleName.asString()}$cast"
                    })
                }


                withParameters("public inline fun ${ifaceName}", buildList {
                    for (member in notNullAbstractMembers) {
                        add("${member.simpleName.asString()}: ${member.type.resolve().render(false)}")
                    }
                    add("initializer: ${builderName}.() -> Unit = {}")
                }, { +it },
                    " : ${ifaceName}"
                ) {
                    withParameters(
                        "return ${builderName}",
                        notNullAbstractMembers,
                        { +it.simpleName.asString() },
                        ".apply { initializer() }.build()"
                    )
                }

                withCodeBlock("public inline fun ${ifaceName}.copy(initializer: ${builderName}.() -> Unit = {}): ${ifaceName}") {
                    ret("${builderName}(this).apply { initializer() }.build()")
                }
            }
        }
        return ret
    }

    private fun MyCodeGenerator.generateConstructor(abstractMembers: List<KSPropertyDeclaration>) {
        withParameters("internal constructor",
            abstractMembers,
            {
                +"${it.simpleName.asString()}: ${it.type.resolve().render(true)}"
            }) {
            for (member in abstractMembers) {
                +"this.${member.simpleName.asString()} = ${member.simpleName.asString()}"
            }
        }
    }
}

class GeneratedBuildersProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) : SymbolProcessor {
        return GeneratedBuildersProcessor(environment.codeGenerator, environment.logger)
    }
}