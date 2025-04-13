plugins {
    id("live.common-conventions")
}

tasks {
    val emptyJson by registering {
        val file = project.layout.buildDirectory.file("empty.json")
        outputs.file(file)
        doLast {
            file.get().asFile.writeText("{\n}\n")
        }
    }
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
        from(configDir) {
            into("examples")
            include("creds.json.example")
            include("visual-config*.json")
            include("analytics*.json")
            include("widget-positions*.json")
            rename { it.removeSuffix(".example") }
        }
        from(project(":backend").tasks.named("shadowJar")) {
            rename { "live-v3.jar" }
        }
        from(project(":schema-generator").tasks.named("generateAllSchemas")) {
            into(".vscode/schemas")
        }
        fun emptyJson(dir:String, name: String) = from(emptyJson) {
            into(dir)
            rename { "$name.json"}
        }
        emptyJson("config", "settings")
        emptyJson("config", "advanced")
        emptyJson("", "creds")
        emptyJson("", "widget-positions")
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