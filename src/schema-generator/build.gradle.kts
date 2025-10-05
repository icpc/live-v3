plugins {
    id("live.kotlin-conventions")
}

val tmpLocation = layout.buildDirectory.dir("tmp")
val schemasExportLocation = providers.provider { rootProject.layout.projectDirectory.dir("schemas") }
val schemasGenerationLocation = tmpLocation.map { it.dir("schemas") }
val schemasGatherLocation = layout.buildDirectory.dir("schemas")

val tsExportLocation = providers.provider { rootProject.layout.projectDirectory.dir("src").dir("frontend").dir("generated") }
val tsGenerationLocation = tmpLocation.map { it.dir("ts") }
val tsGatherLocation = layout.buildDirectory.dir("ts")


fun String.capitalize(): String = replaceFirstChar { it.uppercaseChar() }

fun TaskContainerScope.checkTask(
    taskSuffix: String,
    exportLocation: Provider<Directory>,
    genTask: TaskProvider<out Task>,
) = register<Task>("testSchema${taskSuffix}") {
    group = "verification"
    mustRunAfter(named("exportSchemas"))
    mustRunAfter(named("exportTs"))

    val generatedFiles = genTask.map { it.outputs.files }
    val exportFile = generatedFiles.flatMap { generated -> exportLocation.map { it.file(generated.singleFile.name)} }

    inputs.files(generatedFiles, exportFile)

    doLast {
        val newContent = generatedFiles.get().singleFile.readText()
        val oldContent = exportFile.get().asFile.readText()
        if (newContent != oldContent) {
            throw IllegalStateException("File ${exportFile.get().asFile.name} is outdated. Run `./gradlew :schema-generator:gen` to fix it.")
        }
    }
}

fun TaskContainerScope.genTask(
    command: String,
    classFqNames: List<String>,
    taskSuffix: String,
    title: String?,
    generationLocation: Provider<RegularFile>,
    exportLocation: Provider<Directory>,
): Pair<TaskProvider<out Task>, TaskProvider<out Task>>  {
    val genTask = register<JavaExec>("generateSchema${taskSuffix}") {
        dependsOn(assemble)
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "org.icpclive.generator.schema.GenKt"
        workingDir = tmpLocation.get().asFile
        outputs.file(generationLocation)
        args = buildList {
            add(command)
            classFqNames.forEach {
                add("--class-name"); add(it)
            }
            add("--output"); add(generationLocation.get().asFile.relativeTo(workingDir).path)
            if (title != null) {
                add("--title"); add(title)
            }
        }

    }
    val checkTask = checkTask(taskSuffix, exportLocation, genTask)
    return genTask to checkTask
}

fun TaskContainerScope.genJsonTask(classFqName: String, fileName: String, title: String) =
    genTask(
        command = "json",
        classFqNames = listOf(classFqName),
        taskSuffix = fileName.capitalize(),
        title = title,
        generationLocation = schemasGenerationLocation.map { it.file("${fileName}.schema.json") },
        exportLocation = schemasExportLocation
    )

fun TaskContainerScope.genTsTask(classFqNames: List<String>, fileName: String) =
    genTask(
        command = "type-script",
        classFqNames = classFqNames,
        taskSuffix = fileName.capitalize(),
        title = null,
        generationLocation = tsGenerationLocation.map { it.file("${fileName}.ts") },
        exportLocation = tsExportLocation,
    )


tasks {
    val schemaAllTasks = listOf(
        genJsonTask(
            "org.icpclive.cds.tunning.TuningRuleList",
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
                "org.icpclive.cds.api.ContestInfo",
                "org.icpclive.cds.api.RunInfo",
                "org.icpclive.cds.api.ScoreboardDiff",
                "org.icpclive.api.MainScreenEvent",
                "org.icpclive.api.QueueEvent",
                "org.icpclive.api.AnalyticsEvent",
                "org.icpclive.api.TickerEvent",
                "org.icpclive.api.SolutionsStatistic",
                "org.icpclive.api.ExternalTeamViewSettings",
                "org.icpclive.api.ObjectSettings",
                "org.icpclive.api.WidgetUsageStatistics",
                "org.icpclive.api.TimeLineRunInfo",
            ),
            "api",
        ),
    )
    val widgetPositionsGenTask = project(":frontend").tasks.named("pnpm_run_overlayConfigSchema")
    val schemasGenTasks = schemaAllTasks.map { it.first } + widgetPositionsGenTask
    val tsGenTasks = tsAllTasks.map { it.first }
    val checkTasks = schemaAllTasks.map { it.second } +
            tsAllTasks.map { it.second } +
            checkTask(
                "WidgetPositions",
                schemasExportLocation,
                widgetPositionsGenTask
            )

    // Gradle for inter-project dependencies uses outgoing variants. Those are a bit hard to properly set up, so this
    // project just uses cross-project tasks dependencies (that from the looks of configuration cache aren't welcome,
    // but they do work and IMHO they aren't going to be deprecated anytime soon). However, I've not found a way to
    // create a pseudo-task that just combines the output of two other tasks, so let's just copy those two files one
    // more time.
    val generateSchemas = register<Sync>("generateAllSchemas") {
        group = "build"
        into(schemasGatherLocation)
        from(schemasGenTasks)
    }
    val generateTs = register<Sync>("generateAllTs") {
        group = "build"
        into(tsGatherLocation)
        from(tsGenTasks)
    }

    val exportSchemas = register<Sync>("exportSchemas") {
        into(schemasExportLocation)
        from(generateSchemas)
    }
    val exportTs = register<Sync>("exportTs") {
        into(tsExportLocation)
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.cli)
    implementation(libs.kxs.ts.gen.core)
    implementation(libs.kotlin.reflect)
    runtimeOnly(projects.cds.full)
    runtimeOnly(projects.backendApi)
}
