import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    id("live.common-conventions")
    alias(libs.plugins.node)
}

node {
    version.set("20.11.0")
    pnpmVersion.set("9.5.0")
    download.set(rootProject.findProperty("npm.download") == "true")
}

fun TaskContainerScope.pnpmBuild(name: String, directory: Directory, configure: PnpmTask.(Directory) -> Unit = {}) = named<PnpmTask>(name) {
    outputs.cacheIf { true }
    environment.set(mapOf("PUBLIC_URL" to "/${directory.asFile.name}", "BUILD_PATH" to "build"))
    inputs.dir(layout.projectDirectory.dir("common"))
    inputs.dir(layout.projectDirectory.dir("generated"))
    inputs.file(layout.projectDirectory.file("package.json"))
    inputs.file(layout.projectDirectory.file("pnpm-lock.yaml"))
    inputs.dir(directory.dir("src"))
    inputs.file(directory.file("package.json"))
    outputs.dir(directory.dir("build"))
    configure(directory)
}

tasks {
    pnpmInstall {
        inputs.file("package.json")
        inputs.file("admin/package.json")
        inputs.file("overlay/package.json")
        inputs.file("locator/package.json")
        pnpmCommand.set(listOf("install", "--frozen-lockfile", "--prefer-offline"))
        nodeModulesOutputFilter {
            // Checking node modules to be unchanged is very slow and not really important.
            exclude("**")
        }
    }
    val buildOverlay = pnpmBuild("pnpm_run_buildOverlay", layout.projectDirectory.dir("overlay")) {
        inputs.file(it.file("index.html"))
        mustRunAfter(":schema-generator:exportTs")
    }
    val buildAdmin = pnpmBuild("pnpm_run_buildAdmin", layout.projectDirectory.dir("admin")) {
        inputs.file(it.file("index.html"))
        mustRunAfter(":schema-generator:exportTs")
    }
    val buildLocatorAdmin = pnpmBuild("pnpm_run_buildLocatorAdmin", layout.projectDirectory.dir("locator")) {
        mustRunAfter(":schema-generator:exportTs")
    }
    //val installBrowsers = named<NpmTask>("pnpm_run_install-browsers") // probably want to cache it somehow
    val runTests = named<PnpmTask>("pnpm_run_test") {
        //dependsOn(installBrowsers)
        dependsOn(":backend:release")
    }
    val test = register<Task>("test") {
        dependsOn(runTests)
    }
    val assemble = register<Task>("assemble") {
        dependsOn(buildOverlay, buildAdmin, buildLocatorAdmin)
    }
    register<Task>("build") {
        dependsOn(assemble, test)
    }
}

