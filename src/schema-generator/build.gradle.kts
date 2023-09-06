plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val tmpLocation = buildDir.resolve("tmp")
val schemasExportLocation = rootProject.rootDir.resolve("schemas")
val schemasGenerationLocation = tmpLocation.resolve("schemas")
val schemasGatherLocation = buildDir.resolve("schemas")

val tsExportLocation = rootProject.rootDir.resolve("src").resolve("frontend").resolve("common")
val tsGenerationLocation = tmpLocation.resolve("ts")
val tsGatherLocation = buildDir.resolve("ts")


fun String.capitalize(): String = replaceFirstChar { it.uppercaseChar() }

fun TaskContainerScope.genTask(
    command: String,
    classFqNames: List<String>,
    taskSuffix: String,
    fullFileName: String,
    title: String?,
    exportLocation: File,
    generationLocation: File,
): Pair<TaskProvider<out Task>, TaskProvider<out Task>>  {
    val generatedSchemaFile = generationLocation.resolve(fullFileName)
    val repositorySchemaFile = exportLocation.resolve(fullFileName)

    val genTask = register<JavaExec>("generateSchema${taskSuffix}") {
        dependsOn(assemble)
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "org.icpclive.generator.schema.GenKt"
        workingDir = tmpLocation
        outputs.file(generatedSchemaFile)
        args = buildList {
            add(command)
            classFqNames.forEach {
                add("--class-name"); add(it)
            }
            add("--output"); add(generatedSchemaFile.relativeTo(workingDir).path)
            if (title != null) {
                add("--title"); add(title)
            }
        }

    }
    val checkTask = register<Task>("testSchema${taskSuffix}") {
        group = "verification"
        dependsOn(genTask)
        inputs.files(generatedSchemaFile, repositorySchemaFile)
        doLast {
            val newContent = generatedSchemaFile.readText()
            val oldContent = repositorySchemaFile.readText()
            if (newContent != oldContent) {
                throw IllegalStateException("File $fullFileName is outdated. Run `./gradlew :${project.name}:gen` to fix it.")
            }
        }
    }
    return genTask to checkTask
}

fun TaskContainerScope.genJsonTask(classFqName: String, fileName: String, title: String) =
    genTask(
        "json", listOf(classFqName), fileName.capitalize(),
        "${fileName}.schema.json", title,
        schemasExportLocation, schemasGenerationLocation)

fun TaskContainerScope.genTsTask(classFqNames: List<String>, fileName: String) =
    genTask("type-script", classFqNames, fileName.capitalize(),
        "${fileName}.ts", null,
        tsExportLocation, tsGenerationLocation,
        )


tasks {
    val schemaAllTasks = listOf(
        genJsonTask(
            "org.icpclive.api.tunning.AdvancedProperties",
            "advanced",
            "ICPC live advanced settings"
        ),
        genJsonTask(
            "org.icpclive.cds.settings.CDSSettings",
            "settings",
            "ICPC live settings"
        )
    )
    val tsAllTasks = listOf(
        genTsTask(
            listOf(
                "org.icpclive.api.ContestInfo",
                "org.icpclive.api.RunInfo",
                "org.icpclive.api.Scoreboard",
                "org.icpclive.api.LegacyScoreboard",
                "org.icpclive.api.MainScreenEvent",
                "org.icpclive.api.QueueEvent",
                "org.icpclive.api.AnalyticsEvent",
                "org.icpclive.api.TickerEvent"
            ),
            "api",
        ),
    )
    val schemasGenTasks = schemaAllTasks.map { it.first }
    val tsGenTasks = tsAllTasks.map { it.first }
    val checkTasks = schemaAllTasks.map { it.second } + tsAllTasks.map { it.second }

    // Gradle for inter-project dependencies uses outgoing variants. Those are a bit hard to properly set up, so this
    // project just uses cross-project tasks dependencies (that from the looks of configuration cache aren't welcome,
    // but they do work and IMHO they aren't going to be deprecated anytime soon). However, I've not found a way to
    // create a pseudo-task that just combines the output of two other tasks, so let's just copy those two files one
    // more time.
    val generateSchemas = register<Sync>("generateAllSchemas") {
        group = "build"
        destinationDir = schemasGatherLocation

        from(schemasGenTasks)
    }
    val generateTs = register<Sync>("generateAllTs") {
        group = "build"
        destinationDir = tsGatherLocation

        from(tsGenTasks)
    }

    val exportSchemas = register<Sync>("exportSchemas") {
        destinationDir = schemasExportLocation

        from(generateSchemas)
    }
    val exportTs = register<Sync>("exportTs") {
        destinationDir = tsExportLocation

        from(generateTs)
    }
    register("gen") {
        dependsOn(exportSchemas, exportTs)
    }

    check {
        dependsOn(checkTasks)
    }
}


dependencies {
    implementation(projects.common)
    implementation(libs.cli)
    implementation(libs.kxs.ts.gen.core)
    runtimeOnly(projects.cds)
    runtimeOnly(projects.backendApi)

    testImplementation(libs.kotlin.junit)
}