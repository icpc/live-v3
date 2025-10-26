package org.icpclive.gradle.tasks

import gradle.kotlin.dsl.accessors._0efe46a4cec2e9e682da24afc1fcb716.main
import gradle.kotlin.dsl.accessors._0efe46a4cec2e9e682da24afc1fcb716.runtimeClasspath
import gradle.kotlin.dsl.accessors._0efe46a4cec2e9e682da24afc1fcb716.sourceSets
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.icpclive.gradle.tasks.impl.toJsonSchema
import java.net.URLClassLoader
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.functions
import kotlin.reflect.full.starProjectedType


abstract class SchemaGeneratorTask : DefaultTask() {
    init {
        group = "build"
        description = "Generates json schemas from kotlin serial descriptors"
    }

    @get:Classpath
    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    abstract val rootClass: Property<String>

    @get:Input
    abstract val title: Property<String>

    @get:Internal
    abstract val fileName: Property<String>

    @get:OutputFile
    abstract val outputLocation: RegularFileProperty

    init {
        classpath.convention(project.configurations.runtimeClasspath)
        outputLocation.convention(fileName.flatMap {  project.layout.buildDirectory.file("schema/${it}.schema.json") })
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T: Any> KClass<*>.findFunctionByReturnClass(retClass: KClass<T>) = functions.singleOrNull {
        it.parameters.all { it.kind == KParameter.Kind.INSTANCE } && it.returnType.classifier == retClass
    } as? KCallable<T>

    @TaskAction
    fun generate() {
        val taskClassLoader = Thread.currentThread().getContextClassLoader();
        val targetClassUrls = classpath.files.map { it.toURI().toURL() }.toTypedArray();
        URLClassLoader(targetClassUrls, taskClassLoader).use { classLoader ->
            val clazz = classLoader.loadClass(rootClass.get()).kotlin
            val companion = clazz.companionObject
            val moduleMethod = companion?.findFunctionByReturnClass(SerializersModule::class)
            val serializersModule = moduleMethod?.call(companion.objectInstance) ?: EmptySerializersModule()
            val serializer = serializer(clazz.starProjectedType).descriptor
            val json = Json { prettyPrint = true }
            val schema = json.encodeToString(serializer.toJsonSchema(title.get(), serializersModule)) + "\n"
            outputLocation.get().asFile.writeText(schema)
        }
    }
}