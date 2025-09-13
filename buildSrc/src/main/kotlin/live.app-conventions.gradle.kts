import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.application
import org.gradle.kotlin.dsl.register

plugins {
    application
    id("live.kotlin-conventions")
    id("com.gradleup.shadow")
}

tasks.named<Jar>("jar") {
    archiveClassifier = "just"
}

tasks {
    register<Sync>("release") {
        destinationDir = rootDir.resolve("artifacts/")
        preserve { include("*") }
        from(tasks.named("shadowJar"))
    }

    shadowJar {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        archiveClassifier = null
    }
}