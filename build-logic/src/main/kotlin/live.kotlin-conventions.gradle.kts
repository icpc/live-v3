import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    java
    kotlin("jvm")
    id("live.common-conventions")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val catalogs = extensions.getByType<VersionCatalogsExtension>()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        compilerOptions {
            jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
            freeCompilerArgs.add("-Xcontext-parameters")
            freeCompilerArgs.add("-Xreturn-value-checker=full")
            progressiveMode = true
            optIn.addAll(listOf(
                "kotlinx.serialization.ExperimentalSerializationApi",
                "kotlin.time.ExperimentalTime",
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.concurrent.atomics.ExperimentalAtomicApi"
            ))
        }
    }
}

dependencies {
    testImplementation(catalogs.named("libs").findLibrary("kotlin-junit").get())
}
