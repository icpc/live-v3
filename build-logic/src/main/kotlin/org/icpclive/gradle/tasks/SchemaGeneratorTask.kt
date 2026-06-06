package org.icpclive.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.icpclive.gradle.tasks.worker.SchemaGeneratorWorkAction
import javax.inject.Inject


@CacheableTask
abstract class SchemaGeneratorTask : DefaultTask() {
    init {
        group = "build"
        description = "Generates json schemas from kotlin serial descriptors"
    }

    @get:Classpath
    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection

    @get:Classpath
    @get:InputFiles
    abstract val generatorClasspath: ConfigurableFileCollection

    @get:Input
    abstract val rootClass: Property<String>

    @get:Input
    abstract val title: Property<String>

    @get:Internal
    abstract val fileName: Property<String>

    @get:OutputFile
    abstract val outputLocation: RegularFileProperty

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    init {
        val runtimeClassPath = project.configurations.named("runtimeClasspath")
        val currentClassModules = project.tasks.named("compileKotlin").map { it.outputs.files }
        val merged = project.files(runtimeClassPath, currentClassModules)
        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        val defaultGeneratorClasspath = project.configurations.detachedConfiguration(
            libs.findLibrary("kotlinx-serialization-json").get().get(),
        )
        classpath.convention(merged)
        generatorClasspath.convention(defaultGeneratorClasspath)
        outputLocation.convention(fileName.flatMap {  project.layout.buildDirectory.file("schema/${it}.schema.json") })
    }

    @TaskAction
    fun generate() {
        val task = this
        val workerQueue = workerExecutor.classLoaderIsolation {
            classpath.from(task.classpath)
            classpath.from(task.generatorClasspath)
        }
        workerQueue.submit(SchemaGeneratorWorkAction::class.java) {
            rootClass.set(task.rootClass)
            title.set(task.title)
            outputLocation.set(task.outputLocation)
        }
    }
}
