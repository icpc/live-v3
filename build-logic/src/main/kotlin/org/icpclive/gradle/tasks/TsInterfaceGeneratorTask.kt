package org.icpclive.gradle.tasks

import dev.adamko.kxstsgen.KxsTsGenerator
import dev.adamko.kxstsgen.core.SerializerDescriptorsExtractor
import dev.adamko.kxstsgen.core.util.MutableMapWithDefaultPut
import gradle.kotlin.dsl.accessors._0efe46a4cec2e9e682da24afc1fcb716.runtimeClasspath
import kotlinx.serialization.serializer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.net.URLClassLoader
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


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

    // TODO: this is dirty hack
    // correct way is to run the task via Worker API, with loading this library only for a task.
    private fun clearKxsTsGeneratorCaches() {
        val cacheProperty = SerializerDescriptorsExtractor.Default::class.memberProperties.single { it.name == "elementDescriptors" }
        cacheProperty.isAccessible = true
        val delegate = cacheProperty.getDelegate(SerializerDescriptorsExtractor.Default) as MutableMapWithDefaultPut<*, *>
        cacheProperty.isAccessible = false
        val backingMapProperty = delegate::class.memberProperties.single { it.name == "map" }
        backingMapProperty.isAccessible = true
        val backingMap = backingMapProperty.call(delegate) as MutableMap<*, *>
        backingMapProperty.isAccessible = false
        backingMap.clear()
    }

    @TaskAction
    fun generate() {
        clearKxsTsGeneratorCaches()
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