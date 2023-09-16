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
    command: String,
    classFqNames: List<String>,
    taskSuffix: String,
    fullFileName: String,
    title: String?
): Pair<TaskProvider<out Task>, TaskProvider<out Task>>  {
    val generatedSchemaFile = schemasGenerationLocation.resolve(fullFileName)
    val repositorySchemaFile = schemasExportLocation.resolve(fullFileName)

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
    genTask("json", listOf(classFqName), fileName.capitalize(), "${fileName}.schema.json", title)

fun TaskContainerScope.genTsTask(classFqNames: List<String>, fileName: String) =
    genTask("type-script", classFqNames, fileName.capitalize(), "${fileName}.ts", null)


tasks {
    val genAndCheckTasks = listOf(
        genJsonTask(
            "org.icpclive.api.tunning.AdvancedProperties",
            "advanced",
            "ICPC live advanced settings"
        ),
        genJsonTask(
            "org.icpclive.cds.settings.CDSSettings",
            "settings",
            "ICPC live settings"
        ),
        genTsTask(
            listOf(
                "org.icpclive.api.ContestInfo",
                "org.icpclive.api.RunInfo",
                "org.icpclive.api.Scoreboard",
                "org.icpclive.api.MainScreenEvent",
                "org.icpclive.api.QueueEvent",
                "org.icpclive.api.AnalyticsEvent",
                "org.icpclive.api.TickerEvent"
            ),
            "api",
        ),
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
    implementation(libs.kxs.ts.gen.core)
    runtimeOnly(projects.cds)
    runtimeOnly(projects.backendApi)

    testImplementation(libs.kotlin.junit)
}