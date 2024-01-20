package org.icpclive.cds.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.PrintWriter
import kotlin.random.Random

class SettingsServiceProcessor(private val generator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    private val allGeneratedFiles = mutableListOf<String>()
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allSerializable = resolver
            .getSymbolsWithAnnotation("kotlinx.serialization.Serializable")
        val ret = allSerializable.filter { !it.validate() }.toList()
        val subTypesOfSettings = allSerializable
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .filter { it.superTypes.any { it.resolve().declaration.qualifiedName?.asString() == "org.icpclive.cds.settings.CDSSettings" } }
        subTypesOfSettings.forEach {
            val packageName = it.packageName.asString()
            val className = it.simpleName.asString()
            PrintWriter(
                generator.createNewFile(
                    dependencies = Dependencies(true, it.containingFile!!),
                    packageName = packageName,
                    fileName = "${className}Provider"
                )
            ).use {
                it.println("package $packageName")
                it.println()
                it.println("import kotlinx.serialization.*")
                it.println("import org.icpclive.cds.settings.CDSSettingsProvider")
                it.println()
                it.println("internal class ${className}Provider : CDSSettingsProvider<$className> {")
                it.println("  override val clazz = ${className}::class")
                it.println("  override val serializer = serializer<$className>()")
                it.println("}")
            }
            allGeneratedFiles.add("$packageName.${className}Provider")
        }
        return ret
    }

    override fun finish() {
        super.finish()
        PrintWriter(generator.createNewFile(
            Dependencies(
                true,
                sources = emptyArray()
            ),
            "META-INF.services",
            fileName = "org.icpclive.cds.settings.CDSSettingsProvider",
            extensionName = ""
        )).use {
            for (i in allGeneratedFiles) {
                it.println(i)
            }
        }
    }

}

class SettingsServiceProvider() : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) : SymbolProcessor {
        return SettingsServiceProcessor(environment.codeGenerator, environment.logger)
    }
}