import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.cli.jvm.main

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}


repositories {
    mavenCentral()
}

fun TaskContainerScope.genTask(classPackage: String, className: String, fileName: String, title: String) =
    register<JavaExec>("gen${className.capitalized()}") {
        dependsOn(build)
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "org.icpclive.generator.schema.GenKt"
        workingDir = rootProject.rootDir
        val file = workingDir.resolve("schemas").resolve("$fileName.schema.json")
        outputs.file(file)
        args = listOf(
            "$classPackage.$className",
            "--output", file.relativeTo(workingDir).path,
            "--title", title
        )
    }


tasks {
    val genTasks = listOf(
        genTask("org.icpclive.api.tunning", "AdvancedProperties", "advanced","ICPC live advanced settings"),
        genTask("org.icpclive.cds.settings", "CDSSettings", "settings","ICPC live settings")
    )
    register("gen") {
        dependsOn(genTasks)
    }
}


dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.cds)
    implementation(libs.cli)
    kotlin("reflect")
    testImplementation(libs.kotlin.junit)
}
