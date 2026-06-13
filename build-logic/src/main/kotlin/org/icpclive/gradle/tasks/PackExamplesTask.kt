package org.icpclive.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class PackExamplesTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val packedDirectory: DirectoryProperty

    init {
        group = "custom"
        description = "Packs examples to make them addable to resources"
    }

    @TaskAction
    fun packExamples() {
        val destination = packedDirectory.get().asFile
        destination.deleteRecursively()
        destination.mkdirs()

        val files = sourceDirectory.get().asFile.listFiles().filter { it.name.endsWith(".example.json") }
        val descriptions = buildMap {
            for (file in files) {
                val content = file.readLines()
                put(file.name, content.first())
                destination.resolve(file.name).writeText(content.drop(1).joinToString("\n"))
            }
        }

        destination.resolve("descriptions.json").writeText(descriptions.entries.joinToString(separator = ",\n", prefix = "{\n", postfix = "}\n") {
            "\"${it.key}\": \"${it.value.replace("\"", "\\\"").removePrefix("//").trim()}\""
        })
    }
}
