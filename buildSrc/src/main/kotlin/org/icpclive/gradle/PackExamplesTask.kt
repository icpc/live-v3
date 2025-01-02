package org.icpclive.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class PackExamplesTask : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val packedDirectory: DirectoryProperty

    init {
        group = "custom"
        description = "Packs examples to make them addable to resources"
    }

    @TaskAction
    fun packExamples() {
        val files = sourceDirectory.get().asFile.listFiles().filter { it.name.endsWith(".example.json") }
        val descriptions = buildMap {
            for (file in files) {
                val content = file.readLines()
                put(file.name, content.first())
                packedDirectory.file(file.name).get().asFile.writeText(content.drop(1).joinToString("\n"))
            }
        }

        packedDirectory.get().asFile.mkdirs()

        packedDirectory.file("descriptions.json").get().asFile.writeText(descriptions.entries.joinToString(separator = ",\n", prefix = "{\n", postfix = "}\n") {
            "\"${it.key}\": \"${it.value.replace("\"", "\\\"").removePrefix("//").trim()}\""
        })
    }
}