package org.icpclive.cds.ksp

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.PrintWriter

class SerializerProvidersProcessor(private val generator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    private val allGeneratedFiles = mutableMapOf<String, MutableList<String>>()
    private val interestingClasses = mutableMapOf<String, String>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allSerializable = resolver
            .getSymbolsWithAnnotation("kotlinx.serialization.Serializable")
        val ret = allSerializable.filter { !it.validate()  }.toList()

        val subTypesOfInteresting = allSerializable
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                it.getAllSuperTypes().any { superClass ->
                    val ann = superClass.declaration.getAnnotationsByType(SerializerProviders::class).singleOrNull()
                    if (ann != null) {
                        interestingClasses[superClass.declaration.qualifiedName!!.asString()] = ann.providerClassName
                        true
                    } else {
                        false
                    }
                }
            }

        subTypesOfInteresting.forEach {
            val packageName = it.packageName.asString()
            val className = it.simpleName.asString()
            val supers = it.getAllSuperTypes()
                .map { it.declaration }
                .filterIsInstance<KSClassDeclaration>()
                .filter { superClass -> superClass.qualifiedName?.asString() in interestingClasses }
                .toList()
            PrintWriter(
                generator.createNewFile(
                    dependencies = Dependencies(true, it.containingFile!!),
                    packageName = packageName,
                    fileName = "${className}Provider"
                )
            ).use {
                val providers = supers
                    .map { "${interestingClasses[it.qualifiedName?.asString()]!!}<$className>" }
                it.println("package $packageName")
                it.println()
                it.println("import kotlinx.serialization.*")
                it.println()
                it.println("internal class ${className}Provider : ${providers.joinToString(",")} {")
                it.println("  override val clazz = ${className}::class")
                it.println("  override val serializer = serializer<$className>()")
                it.println("}")
            }
            for (superClass in supers) {
                allGeneratedFiles
                    .getOrPut(interestingClasses[superClass.qualifiedName!!.asString()]!!) { mutableListOf() }
                    .add("$packageName.${className}Provider")
            }
        }
        return ret
    }

    override fun finish() {
        super.finish()
        for ((fileName, content) in allGeneratedFiles)
        PrintWriter(generator.createNewFile(
            Dependencies(
                true,
                sources = emptyArray()
            ),
            "META-INF.services",
            fileName = fileName,
            extensionName = ""
        )).use {
            it.println(content.joinToString("\n"))
        }
    }

}

class SerializerProvidersProvider() : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) : SymbolProcessor {
        return SerializerProvidersProcessor(environment.codeGenerator, environment.logger)
    }
}