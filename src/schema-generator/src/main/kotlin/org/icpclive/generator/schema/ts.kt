package org.icpclive.generator.schema

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import dev.adamko.kxstsgen.KxsTsGenerator
import dev.adamko.kxstsgen.core.TsDeclaration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.io.File

// This is a copy-pasted generating method to work around a bug.
fun KxsTsGenerator.patchedGenerate(vararg serializers: KSerializer<*>) : String {
    return serializers
        .toSet()
        .flatMap { serializer -> descriptorsExtractor(serializer) }
        .toSet()
        .flatMap { descriptor -> elementConverter(descriptor) }
        .toSet()
        .groupBy { element -> sourceCodeGenerator.groupElementsBy(element) }
        .mapValues { (_, elements) ->
            elements
                .filterIsInstance<TsDeclaration>()
                .map { element -> sourceCodeGenerator.generateDeclaration(element) }
                .filter { it.isNotBlank() }
                .toSet() // workaround for https://github.com/adamko-dev/kotlinx-serialization-typescript-generator/issues/110
                .joinToString(config.declarationSeparator)
        }
        .values
        .joinToString(config.declarationSeparator)
}

class TSCommand : CliktCommand(name = "type-script") {
    private val className by option(help = "Class name for which schema should be generated").multiple()
    private val output by option("--output", "-o", help = "File to print output").required()

    override fun run() {
        val tsGenerator = KxsTsGenerator()
        val things = className.map { serializer(Class.forName(it)) }
        File(output).printWriter().use {
            it.println(tsGenerator.patchedGenerate(*things.toTypedArray()))
        }
    }
}
