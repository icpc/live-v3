package org.icpclive.gradle.tasks

import dev.adamko.kxstsgen.KxsTsGenerator
import gradle.kotlin.dsl.accessors._0efe46a4cec2e9e682da24afc1fcb716.main
import gradle.kotlin.dsl.accessors._0efe46a4cec2e9e682da24afc1fcb716.runtimeClasspath
import gradle.kotlin.dsl.accessors._0efe46a4cec2e9e682da24afc1fcb716.sourceSets
import kotlinx.serialization.serializer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.net.URLClassLoader


abstract class TsInterfaceGeneratorTask : DefaultTask() {
    init {
        group = "build"
        description = "Generates ts interface from kotlin serial descriptors"
    }

    @get:Classpath
    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    abstract val rootClasses: ListProperty<String>

    @get:Internal
    abstract val fileName: Property<String>

    @get:OutputFile
    abstract val outputLocation: RegularFileProperty

    init {
        classpath.convention(project.configurations.runtimeClasspath)
        outputLocation.convention(fileName.flatMap { project.layout.buildDirectory.file("ts/${it}.ts") })
    }

    @TaskAction
    fun generate() {
        val taskClassLoader = Thread.currentThread().getContextClassLoader();
        val targetClassUrls = classpath.files.map { it.toURI().toURL() }.toTypedArray();
        URLClassLoader(targetClassUrls, taskClassLoader).use { classLoader ->
            val tsGenerator = KxsTsGenerator()
            val descriptors = rootClasses.get().map { serializer(classLoader.loadClass(it)) }
            val interfaceText = tsGenerator.generate(*descriptors.toTypedArray()) + "\n"
            outputLocation.get().asFile.writeText(interfaceText)
        }
    }
}