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
            freeCompilerArgs.add("-Xjvm-default=all")
            optIn.addAll(listOf("kotlinx.serialization.ExperimentalSerializationApi"))
        }
    }
}

dependencies {
    testImplementation(catalogs.named("libs").findLibrary("kotlin-junit").get())
}