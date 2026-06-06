package org.icpclive.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.icpclive.gradle.tasks.worker.TsInterfaceGeneratorWorkAction
import javax.inject.Inject


@CacheableTask
abstract class TsInterfaceGeneratorTask : DefaultTask() {
    init {
        group = "build"
        description = "Generates ts interface from kotlin serial descriptors"
    }

    @get:Classpath
    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection

    @get:Classpath
    @get:InputFiles
    abstract val generatorClasspath: ConfigurableFileCollection

    @get:Input
    abstract val rootClasses: ListProperty<String>

    @get:Internal
    abstract val fileName: Property<String>

    @get:OutputFile
    abstract val outputLocation: RegularFileProperty

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    init {
        val runtimeClasspathConfig = project.configurations.named("runtimeClasspath")
        val currentClassOutput = project.tasks.named("compileKotlin").map { it.outputs.files }
        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        val defaultGeneratorClasspath = project.configurations.detachedConfiguration(
            libs.findLibrary("kxs-ts-gen-core").get().get(),
            libs.findLibrary("kotlinx-serialization-json").get().get(),
        )

        classpath.convention(project.files(runtimeClasspathConfig, currentClassOutput))
        generatorClasspath.convention(defaultGeneratorClasspath)
        outputLocation.convention(fileName.zip(project.layout.buildDirectory) { file, dir -> dir.file("ts/${file}.ts") })
    }

    @TaskAction
    fun generate() {
        val task = this
        val workerQueue = workerExecutor.classLoaderIsolation {
            classpath.from(task.classpath)
            classpath.from(task.generatorClasspath)
        }
        workerQueue.submit(TsInterfaceGeneratorWorkAction::class.java) {
            rootClasses.set(task.rootClasses)
            outputLocation.set(task.outputLocation)
        }
    }
}
