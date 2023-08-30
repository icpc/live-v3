import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.cli.jvm.main

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}


repositories {
    mavenCentral()
}

val schemaLocation = rootProject.rootDir.resolve("schemas")

fun TaskContainerScope.genTask(classPackage: String, className: String, fileName: String, title: String): Pair<TaskProvider<out Task>, TaskProvider<out Task>>  {
    val file = buildDir.resolve("schemas").resolve("$fileName.schema.json")
    val genTask = register<JavaExec>("gen${className.capitalized()}") {
        dependsOn(assemble)
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "org.icpclive.generator.schema.GenKt"
        workingDir = buildDir
        outputs.file(file)
        args = listOf(
            "$classPackage.$className",
            "--output", file.relativeTo(workingDir).path,
            "--title", title
        )
    }
    val checkTask = register<Task>("test${className.capitalized()}") {
       dependsOn(genTask)
       doLast {
           fun sanitize(s: String) = s.filter { it.isWhitespace() }
           val newContent = file.readText()
           val oldContent = schemaLocation.resolve(file.name).readText()
           if (newContent != oldContent) {
               throw IllegalStateException("Json schema for ${className} is outdated. Run `./gradlew :${project.name}:gen` to fix it.")
           }
       }
    }
    return genTask to checkTask
}


tasks {
    val genAndCheckTasks = listOf(
        genTask("org.icpclive.api.tunning", "AdvancedProperties", "advanced","ICPC live advanced settings"),
        genTask("org.icpclive.cds.settings", "CDSSettings", "settings","ICPC live settings")
    )
    val genTasks = genAndCheckTasks.map { it.first }
    val checkTasks = genAndCheckTasks.map { it.second }
    register<Copy>("gen") {
        from(genTasks)
        destinationDir = schemaLocation
    }
    check {
        dependsOn(checkTasks)
    }
}


dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.cds)
    implementation(libs.cli)
    kotlin("reflect")
    testImplementation(libs.kotlin.junit)
}
