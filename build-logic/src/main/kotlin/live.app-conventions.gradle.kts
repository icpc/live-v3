import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.application
import org.gradle.kotlin.dsl.register

plugins {
    application
    id("live.kotlin-conventions")
    id("live.schemas-in-resources")
    id("com.gradleup.shadow")
}

tasks.named<Jar>("jar") {
    archiveClassifier = "just"
}

artifacts {
    add("applicationJarProvider", tasks.shadowJar)
}

tasks {
    register<Sync>("release") {
        destinationDir = rootDir.resolve("artifacts/")
        preserve { include("*") }
        from(shadowJar)
    }

    shadowJar {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        archiveClassifier = null
    }
}
