plugins {
    // versions are set in dependencies block for build.gradle.kts
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        kotlinOptions {
            kotlinOptions.allWarningsAsErrors = true
        }
    }
}