import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    id("live.common-conventions")
    alias(libs.plugins.node)
}

node {
    version.set("20.11.0")
    download.set(rootProject.findProperty("npm.download") == "true")
}

tasks {
    pnpmInstall {
        inputs.file("package.json")
        inputs.file("admin/package.json")
        inputs.file("overlay/package.json")
    }
    val buildOverlay = named<PnpmTask>("pnpm_run_buildOverlay") {
        outputs.cacheIf { true }
        environment.set(mapOf("PUBLIC_URL" to "/overlay", "BUILD_PATH" to "build"))
        inputs.dir("overlay/src")
        inputs.file("overlay/index.html")
        inputs.dir("common")
        inputs.file("package.json")
        inputs.file("pnpm-lock.yaml")
        inputs.file("overlay/package.json")
        outputs.dir("overlay/build")
    }
    val buildAdmin = named<PnpmTask>("pnpm_run_buildAdmin") {
        outputs.cacheIf { true }
        environment.set(mapOf("PUBLIC_URL" to "/admin"))
        inputs.dir("admin/src")
        inputs.dir("admin/public")
        inputs.dir("common")
        inputs.file("package.json")
        inputs.file("pnpm-lock.yaml")
        inputs.file("admin/package.json")
        outputs.dir("admin/build")
    }
    val buildLocatorAdmin = named<PnpmTask>("pnpm_run_buildLocatorAdmin") {
        outputs.cacheIf { true }
        environment.set(mapOf("PUBLIC_URL" to "/locator"))
        inputs.dir("locator/src")
        inputs.dir("locator/public")
        inputs.dir("common")
        inputs.file("package.json")
        inputs.file("pnpm-lock.yaml")
        inputs.file("locator/package.json")
        outputs.dir("locator/build")
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

