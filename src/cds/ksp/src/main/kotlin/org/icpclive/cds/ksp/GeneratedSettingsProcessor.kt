package org.icpclive.cds.ksp

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.PrintWriter


class GeneratedSettingsProcessor(private val generator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    private fun KSType.render() = buildString {
        append(declaration.qualifiedName?.asString() ?: "<ERROR>")
        val typeArgs = arguments
        if (arguments.isNotEmpty()) {
            append("<")
            append(
                typeArgs.map {
                    val type = it.type?.resolve()
                    "${it.variance.label} ${type?.declaration?.qualifiedName?.asString() ?: "ERROR"}" +
                            if (type?.nullability == Nullability.NULLABLE) "?" else ""
                }.joinToString(", ")
            )
            append(">")
        }
        if (isMarkedNullable) append("?")
    }
    private val allGeneratedFiles = mutableListOf<String>()
    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val toGenerate = resolver
            .getSymbolsWithAnnotation("org.icpclive.cds.ksp.GenerateSettings")
        val ret = toGenerate.filter { !it.validate() }.toList()
        val interfacesToImplement = toGenerate
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                if (it.classKind == ClassKind.INTERFACE && it.getAllSuperTypes().any { it.declaration.qualifiedName?.asString() == "org.icpclive.cds.settings.CDSSettings" }) {
                    true
                } else {
                    logger.error("Class annotatated with GenerateSettings must be interface implementing CDSSettings", it)
                    false
                }
            }.toList()
        for (iface in interfacesToImplement) {
            val packageName = iface.packageName.asString()
            val className = iface.simpleName.asString() + "Impl"
            PrintWriter(
                generator.createNewFile(
                    dependencies = Dependencies(true, iface.containingFile!!),
                    packageName = packageName,
                    fileName = className
                )
            ).use {
                it.println("package $packageName")
                it.println()
                it.println("import kotlinx.serialization.*")
                it.println("import org.icpclive.util.*")
                it.println()
                it.println("@Serializable")
                it.println("@SerialName(\"${iface.getAnnotationsByType(GenerateSettings::class).single().name}\")")
                it.println("internal class $className(")
                val postponedMembers = mutableListOf<KSPropertyDeclaration>()
                for (member in iface.getAllProperties()) {
                    val type = member.type.resolve().declaration.qualifiedName?.asString()
                    when (type) {
                        "org.icpclive.cds.settings.Credential", "org.icpclive.cds.settings.UrlOrLocalPath" -> {
                            it.println("  @Contextual")
                        }
                        "kotlin.text.Regex" -> {
                            it.println("  @Serializable(with = RegexSerializer::class)")
                        }
                        "kotlinx.datetime.Instant" -> {
                            it.println("  @Serializable(with = HumanTimeSerializer::class)")
                        }
                        "kotlin.time.Duration" -> {
                            it.println("  @Serializable(with = DurationInSecondsSerializer::class)")
                            it.println("  @SerialName(\"${member.simpleName.asString()}Seconds\")")
                        }
                        "kotlinx.datetime.TimeZone" -> {
                            it.println("  @Serializable(with = TimeZoneSerializer::class)")
                        }
                    }
                    if (member.type.resolve().isMarkedNullable) {
                        it.println("  override val ${member.simpleName.asString()}: ${member.type.resolve().render()} = null,")
                    } else if (member.isAbstract()) {
                        it.println("  override val ${member.simpleName.asString()}: ${member.type.resolve().render()},")
                    } else {
                        it.println("  @SerialName(\"${member.simpleName.asString()}\") val ${member.simpleName.asString()}_: ${member.type.resolve().render()}? = null,")
                        postponedMembers.add(member)
                    }
                }
                it.println(") : ${iface.qualifiedName!!.asString()} {")
                for (member in postponedMembers) {
                    it.println("  override val ${member.simpleName.asString()}: ${member.type.resolve().render()}")
                    it.println("      get() = ${member.simpleName.asString()}_ ?: super.${member.simpleName.asString()}")
                }
                it.println("}")
            }
            allGeneratedFiles.add("$packageName.${className}")
        }
        return ret
    }
}

class GeneratedSettingsProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) : SymbolProcessor {
        return GeneratedSettingsProcessor(environment.codeGenerator, environment.logger)
    }
}