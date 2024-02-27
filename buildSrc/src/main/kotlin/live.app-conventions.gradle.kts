import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.application
import org.gradle.kotlin.dsl.register

plugins {
    application
    id("live.kotlin-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    register<Sync>("release") {
        destinationDir = rootDir.resolve("artifacts/")
        preserve { include("*") }
        from(tasks.named("shadowJar"))
    }

    shadowJar {
        mergeServiceFiles()

        archiveClassifier = null
    }
}