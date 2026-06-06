package org.icpclive.gradle.tasks.worker

import dev.adamko.kxstsgen.KxsTsGenerator
import kotlinx.serialization.serializer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

interface TsInterfaceGeneratorWorkParameters : WorkParameters {
    val rootClasses: ListProperty<String>
    val outputLocation: RegularFileProperty
}

abstract class TsInterfaceGeneratorWorkAction : WorkAction<TsInterfaceGeneratorWorkParameters> {
    override fun execute() {
        val tsGenerator = KxsTsGenerator()
        val classLoader = Thread.currentThread().contextClassLoader
        val descriptors = parameters.rootClasses.get().map { serializer(classLoader.loadClass(it)) }
        val interfaceText = tsGenerator.generate(*descriptors.toTypedArray()) + "\n"
        parameters.outputLocation.get().asFile.writeText(interfaceText)
    }
}
