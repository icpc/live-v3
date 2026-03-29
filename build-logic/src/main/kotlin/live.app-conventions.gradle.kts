import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.application
import org.gradle.kotlin.dsl.register
import org.icpclive.gradle.tasks.ExtractLicensesTask
import kotlin.io.path.absolute
import kotlin.io.path.name

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

    val extractLicensesTask by tasks.registering(ExtractLicensesTask::class) {
        from(configurations.runtimeClasspath)
        outputDir.set(layout.buildDirectory.dir("extracted-licenses"))
    }

    shadowJar {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        append("META-INF/io.netty.versions.properties")
        exclude(ExtractLicensesTask.FILENAMES)
        from(extractLicensesTask) {
            into("META-INF/licenses")
        }
        archiveClassifier = null
    }
    distZip { enabled = false }
    distTar { enabled = false }
    shadowDistZip { enabled = false }
    shadowDistTar { enabled = false }
}
