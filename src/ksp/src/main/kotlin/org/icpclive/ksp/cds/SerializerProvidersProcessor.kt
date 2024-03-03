package org.icpclive.ksp.cds

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import org.icpclive.ksp.common.*
import java.io.PrintWriter

class SerializerProvidersProcessor(private val generator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    class GeneratedFile(
        val names: MutableList<String> = mutableListOf(),
        val files: MutableSet<KSFile> = mutableSetOf()
    )
    private val allGeneratedFiles = mutableMapOf<String, GeneratedFile>()
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
            generator.generateFile(
                dependencies = Dependencies(true, it.containingFile!!),
                packageName = packageName,
                fileName = "${className}Provider"
            ) {
                val providers = supers.map { "${interestingClasses[it.qualifiedName?.asString()]!!}<$className>" }
                imports("kotlinx.serialization.*")
                withCodeBlock("internal class ${className}Provider : ${providers.joinToString(",")}") {
                    property(listOf(Modifier.OVERRIDE), "clazz", null, "${className}::class")
                    property(listOf(Modifier.OVERRIDE), "serializer", null, "serializer<$className>()")
                }
            }
            for (superClass in supers) {
                allGeneratedFiles
                    .getOrPut(interestingClasses[superClass.qualifiedName!!.asString()]!!) { GeneratedFile() }
                    .let { file ->
                        file.names.add("$packageName.${className}Provider")
                        file.files.add(it.containingFile!!)
                    }
            }
        }
        return ret
    }

    override fun finish() {
        super.finish()
        for ((fileName, content) in allGeneratedFiles) {
            PrintWriter(
                generator.createNewFile(
                    Dependencies(
                        true,
                        sources = content.files.toTypedArray()
                    ),
                    "META-INF.services",
                    fileName = fileName,
                    extensionName = ""
                )
            ).use {
                it.println(content.names.joinToString("\n"))
            }
        }
    }
}

class SerializerProvidersProvider() : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) : SymbolProcessor {
        return SerializerProvidersProcessor(environment.codeGenerator, environment.logger)
    }
}