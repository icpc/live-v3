package org.icpclive.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class CheckExportedFiles : DefaultTask() {

    init {
        group = "verification"
        description = "Checks that exported files are up-to-date by comparing content."
    }

    @get:Internal
    abstract val exportLocation: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val exportedFiles = generatedFiles.elements.zip(exportLocation) { fileSet, exportLocation ->
        fileSet.map { exportLocation.file(it.asFile.name).asFile }
    }

    @get:Input
    abstract val fixTask: Property<String>

    fun from(configuration: Provider<Configuration>) {
        generatedFiles.setFrom(configuration)
    }

    @TaskAction
    fun check() {
        val generated = generatedFiles.files.associateBy { it.name }
        val exported = exportedFiles.get().associateBy { it.name }

        for (name in generated.keys + exported.keys) {
            val generatedFile = generated[name]
            val exportFile = exported[name]

            requireNotNull(generatedFile) { "File '$name' found in export location but not in generated sources. Skipping check." }
            requireNotNull(exportFile) { "File '$name' is missing from the export location." }
            require(generatedFile.readText() == exportFile.readText()) {
                "File '${exportFile.name}' is outdated. Run `./gradlew ${fixTask.get()}` to fix it."
            }
        }
    }
}