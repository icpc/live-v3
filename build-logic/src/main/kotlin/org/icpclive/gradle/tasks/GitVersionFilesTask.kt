package org.icpclive.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class GitVersionFilesTask @Inject constructor(
    private val execOps: ExecOperations
) : DefaultTask() {

    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    // Expose outputs as properties in case a build script wants to wire them individually
    @get:OutputFile
    abstract val branchFile: RegularFileProperty

    @get:OutputFile
    abstract val commitFile: RegularFileProperty

    @get:OutputFile
    abstract val descriptionFile: RegularFileProperty

    init {
        group = "custom"
        description = "Generates git metadata files (branch, commit, description)."

        // Disable up-to-date to reflect that Git state may change outside Gradle's inputs
        outputs.upToDateWhen { false }

        // Defaults mimic the previous inline task
        branchFile.convention(outputDirectory.file("git_branch"))
        commitFile.convention(outputDirectory.file("git_commit"))
        descriptionFile.convention(outputDirectory.file("git_description"))
    }

    @TaskAction
    fun generate() {
        branchFile.writeGitOutputTo("rev-parse", "--abbrev-ref", "HEAD")
        commitFile.writeGitOutputTo("rev-parse", "HEAD")
        descriptionFile.writeGitOutputTo("describe", "--all", "--always", "--dirty", "--match=origin/*", "--match=v*")
    }

    private fun RegularFileProperty.writeGitOutputTo(vararg arguments: String) {
        get().asFile.outputStream().use {
            execOps.exec {
                executable = "git"
                standardOutput = it
                isIgnoreExitValue = true
                args = arguments.toList()
            }
        }
    }
}