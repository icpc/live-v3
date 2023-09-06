plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val schemasExportLocation = rootProject.rootDir.resolve("schemas/")
val tmpLocation = buildDir.resolve("tmp/")
val schemasGenerationLocation = tmpLocation.resolve("schemas/")
val schemasGatherLocation = buildDir.resolve("schemas/")

fun String.capitalize(): String = replaceFirstChar { it.uppercaseChar() }

fun TaskContainerScope.genTask(
    classPackage: String,
    className: String,
    fileName: String,
    title: String
): Pair<TaskProvider<out Task>, TaskProvider<out Task>>  {
    val fullFileName = "$fileName.schema.json"
    val generatedSchemaFile = schemasGenerationLocation.resolve(fullFileName)
    val repositorySchemaFile = schemasExportLocation.resolve(fullFileName)

    val genTask = register<JavaExec>("generateSchema${className.capitalize()}") {
        dependsOn(assemble)
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "org.icpclive.generator.schema.GenKt"
        workingDir = tmpLocation
        outputs.file(generatedSchemaFile)
        args = listOf(
            "$classPackage.$className",
            "--output", generatedSchemaFile.relativeTo(workingDir).path,
            "--title", title
        )
    }
    val checkTask = register<Task>("testSchema${className.capitalize()}") {
        group = "verification"
        dependsOn(genTask)
        inputs.files(generatedSchemaFile, repositorySchemaFile)
        doLast {
            val newContent = generatedSchemaFile.readText()
            val oldContent = repositorySchemaFile.readText()
            if (newContent != oldContent) {
                throw IllegalStateException("Json schema for $className is outdated. Run `./gradlew :${project.name}:gen` to fix it.")
            }
        }
    }
    return genTask to checkTask
}


tasks {
    val genAndCheckTasks = listOf(
        genTask("org.icpclive.api.tunning", "AdvancedProperties", "advanced", "ICPC live advanced settings"),
        genTask("org.icpclive.cds.settings", "CDSSettings", "settings", "ICPC live settings"),
    )
    val genTasks = genAndCheckTasks.map { it.first }
    val checkTasks = genAndCheckTasks.map { it.second }

    // Gradle for inter-project dependencies uses outgoing variants. Those are a bit hard to properly set up, so this
    // project just uses cross-project tasks dependencies (that from the looks of configuration cache aren't welcome,
    // but they do work and IMHO they aren't going to be deprecated anytime soon). However, I've not found a way to
    // create a pseudo-task that just combines the output of two other tasks, so let's just copy those two files one
    // more time.
    val generateSchemas = register<Sync>("generateAllSchemas") {
        group = "build"
        destinationDir = schemasGatherLocation

        from(genTasks)
    }

    register<Sync>("gen") {
        destinationDir = schemasExportLocation

        from(generateSchemas)
    }

    check {
        dependsOn(checkTasks)
    }
}


dependencies {
    implementation(projects.common)
    implementation(libs.cli)
    runtimeOnly(projects.cds)

    testImplementation(libs.kotlin.junit)
}