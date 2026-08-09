package org.icpclive.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class ExtractLicensesTask @Inject constructor(
    private val archives: ArchiveOperations
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected abstract val runtimeFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    protected abstract val artifactIdentities: SetProperty<String>

    @get:Internal
    protected abstract val artifactNameByFilePath: MapProperty<String, String>

    fun from(configuration: Provider<Configuration>) {
        val artifacts: Provider<ArtifactCollection> = configuration.map {
            it.incoming.artifacts
        }

        runtimeFiles.from(artifacts.map { it.artifactFiles })

        artifactIdentities.addAll(artifacts.flatMap { artifactCollection ->
            artifactCollection.resolvedArtifacts.map { resolvedArtifacts ->
                resolvedArtifacts.map { it.stableIdentity() }
            }
        })

        artifactNameByFilePath.putAll(artifacts.flatMap { artifactCollection ->
            artifactCollection.resolvedArtifacts.map { resolvedArtifacts ->
                resolvedArtifacts.associate {
                    it.file.absolutePath to it.artifactName()
                }
            }
        })
    }

    private fun ResolvedArtifactResult.stableIdentity(): String =
        "${id.componentIdentifier.displayName}:${id.displayName}:${variant.displayName}"

    private fun ResolvedArtifactResult.artifactName(): String =
        id.componentIdentifier.displayName

    @TaskAction
    fun execute() {
        val destination = outputDir.get().asFile

        runtimeFiles.forEach { jarFile ->
            val artifactRawName = artifactNameByFilePath.get()[jarFile.absolutePath] ?: "unknown"
            val artifactPath = artifactRawName.split(':').dropLast(1)
            val targetDir = artifactPath.fold(destination) { acc, name -> acc.resolve(name) }

            archives.zipTree(jarFile).matching {
                include(FILENAMES)
                exclude("**/*.class")
            }.visit {
                if (!isDirectory) {
                    val targetFile = targetDir.resolve(name)
                    this.copyTo(targetFile)
                    logger.info("Extracted $path from $jarFile to ${targetFile.absolutePath}")
                }
            }
        }
    }

    companion object {
        val FILENAMES = listOf("META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/license/*", "AL2.0", "LGPL2.1")
    }
}
