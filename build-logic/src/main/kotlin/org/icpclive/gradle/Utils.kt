package org.icpclive.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec

inline fun JavaExec.argPropertyList(name: String, crossinline transform: (String) -> List<String>) {
    val provider = project.providers.gradleProperty(name).map { transform(it) }
    argumentProviders.add {
        provider.getOrElse(emptyList())
    }
}

inline fun JavaExec.argProperty(name: String, crossinline transform: (String) -> String) {
    argPropertyList(name) { listOf(transform(it)) }
}

fun <T: Any> T.enabledIf(enabled: Provider<Boolean>) : Provider<T> =
    enabled.map { if (it) this@enabledIf else null }

fun Project.booleanGradleProperty(name: String) : Provider<Boolean> =
    providers.gradleProperty(name).map { it.toBoolean() }