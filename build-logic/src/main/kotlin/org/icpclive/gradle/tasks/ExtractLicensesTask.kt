package org.icpclive.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.util.prefixIfNot
import javax.inject.Inject

abstract class ExtractLicensesTask @Inject constructor(
    private val archives: ArchiveOperations
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val runtimeFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    // Internal mapping logic moved inside the task
    @get:Input
    abstract val fileToArtifactName: MapProperty<String, String>

    fun from(configuration: Provider<Configuration>) {
        runtimeFiles.from(configuration)
        fileToArtifactName.set(configuration.map { config ->
            config.resolvedConfiguration.resolvedArtifacts.associate {
                it.file.absolutePath to it.moduleVersion.id.name
            }
        })
    }

    @TaskAction
    fun execute() {
        val destination = outputDir.get().asFile
        destination.deleteRecursively()
        destination.mkdirs()

        logger.info(fileToArtifactName.get().toString())

        runtimeFiles.forEach { jarFile ->
            val artifactName = fileToArtifactName.get()[jarFile.absolutePath] ?: "unknown"

            archives.zipTree(jarFile).matching {
                include(FILENAMES)
                exclude("**/*.class")
            }.visit {
                if (!isDirectory) {
                    val baseName = name.substringBeforeLast(".")
                    val extension = name.substringAfterLast(".", missingDelimiterValue = "")
                        .let { if (it.isEmpty()) "" else ".$it" }

                    val targetName = "${baseName}-${artifactName}${extension}"
                    val targetFile = destination.resolve(targetName)
                    this.copyTo(targetFile)
                    logger.info("Extracted ${path} from $jarFile to ${targetFile.absolutePath}")
                }
            }
        }
    }

    companion object {
        val FILENAMES = listOf("META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/license/*", "AL2.0", "LGPL2.1")
    }
}