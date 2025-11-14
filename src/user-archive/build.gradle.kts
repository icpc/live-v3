plugins {
    id("live.common-conventions")
    id("live.file-sharing")
}

dependencies {
    jsonSchemas(projects.backendApi)
    jsonSchemas(projects.cds.full)
    jsonSchemas(projects.frontend)
    applicationJar(projects.backend)
}

tasks {
    val emptyJson by registering {
        val file = project.layout.buildDirectory.file("empty.json")
        outputs.file(file)
        doLast {
            file.get().asFile.writeText("{\n}\n")
        }
    }
    val emptyJsonArray by registering {
        val file = project.layout.buildDirectory.file("empty-array.json")
        outputs.file(file)
        doLast {
            file.get().asFile.writeText("[\n]\n")
        }
    }
    val version = project.version
    val userArchive = register<Sync>("userArchive") {
        destinationDir = project.layout.buildDirectory.dir("archive").get().asFile
        val configDir = rootProject.layout.projectDirectory.dir("config")
        val projectDir = project.layout.projectDirectory
        from(configDir.dir("_examples")) {
            includeEmptyDirs = false
            include("codeforces/**")
            include("clics/**")
            include("cms/**")
            include("pcms/**")
            include("yandex/**")
            into("examples/config")
        }
        from(configDir.dir("_examples/_advnaced")) {
            into("examples/advanced")
        }
        from(configDir.dir("_examples/_visual-config")) {
            into("examples/visual-config")
        }
        from(configDir) {
            into("examples")
            include("creds.json.example")
            include("visual-config*.json")
            include("analytics*.json")
            rename { it.removeSuffix(".example") }
        }
        from(configurations.applicationJarResolver) {
            rename { it.replace("-${version}", "") }
        }
        from(configurations.jsonSchemasResolver) {
            into(".vscode/schemas")
        }
        fun emptyJson(dir:String, name: String, task: TaskProvider<Task> = emptyJson) = from(task) {
            into(dir)
            rename { "$name.json"}
        }
        emptyJson("config", "settings")
        emptyJson("config", "advanced", emptyJsonArray)
        emptyJson("", "creds")
        emptyJson("", "visual-config")
        from(projectDir.dir("scripts"))
        from(configDir) {
            include("analytics-en.json")
        }
        from(projectDir.dir("vscode")) {
            into(".vscode")
        }
    }
    register<Zip>("release") {
        from(userArchive)
        archiveFileName = "live-v3-${version}.zip"
        destinationDirectory = rootProject.layout.projectDirectory.dir("artifacts")
    }
}