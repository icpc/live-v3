import com.github.gradle.node.pnpm.task.PnpmTask
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.provideDelegate
import org.icpclive.gradle.tasks.CheckExportedFiles

plugins {
    id("live.common-conventions")
    id("live.file-sharing")
    base
    alias(libs.plugins.node)
}

node {
    version.set("22.20.0")
    pnpmVersion.set("10.18.3")
    download.set(rootProject.findProperty("npm.download") == "true")
}

val generatedTsLocation = project.projectDir.resolve("generated")

val copyGeneratedTs by tasks.registering(Sync::class) {
    from(configurations.tsInterfacesResolver)
    into(generatedTsLocation)
}

val checkTsExport by tasks.registering(CheckExportedFiles::class) {
    from(configurations.tsInterfacesResolver)
    exportLocation = generatedTsLocation
    fixTask = "gen"
}


fun PnpmTask.setInputs(directory: Directory, publicUrl: String? = null) {
    environment.set(buildMap {
        publicUrl?.let { put("PUBLIC_URL", it) }
        put("BUILD_PATH", "build")
    })
    inputs.dir(layout.projectDirectory.dir("common"))
    inputs.dir(layout.projectDirectory.dir("generated"))
    inputs.file(layout.projectDirectory.file("package.json"))
    inputs.file(layout.projectDirectory.file("pnpm-lock.yaml"))
    inputs.dir(directory.dir("src"))
    inputs.file(directory.file("package.json"))
    mustRunAfter(copyGeneratedTs)
}

fun TaskContainerScope.pnpmBuild(name: String, directory: Directory, publicUrl: String, configure: PnpmTask.(Directory) -> Unit = {}) = named<PnpmTask>(name) {
    outputs.cacheIf { true }
    setInputs(directory, publicUrl)
    outputs.dir(directory.dir("build"))
    configure(directory)
}

dependencies {
    tsInterfaces(projects.backendApi)
}

tasks {
    pnpmInstall {
        inputs.file("package.json")
        inputs.file("admin-overlay/package.json")
        inputs.file("admin-contest-info/package.json")
        inputs.file("admin-router/package.json")
        inputs.file("overlay/package.json")
        inputs.file("locator/package.json")
        pnpmCommand.set(listOf("install", "--frozen-lockfile", "--prefer-offline"))
        nodeModulesOutputFilter {
            // Checking node modules to be unchanged is very slow and not really important.
            exclude("**")
        }
    }
    val buildOverlay = pnpmBuild("pnpm_run_buildOverlay", layout.projectDirectory.dir("overlay"), "/overlay") {
        inputs.file(it.file("index.html"))
    }
    val buildAdminOverlay = pnpmBuild("pnpm_run_buildAdminOverlay", layout.projectDirectory.dir("admin-overlay"), "/admin") {
        inputs.file(it.file("index.html"))
        inputs.dir(layout.projectDirectory.dir("admin-router/src"))
    }
    val buildAdminContestInfo = pnpmBuild("pnpm_run_buildAdminContestInfo", layout.projectDirectory.dir("admin-contest-info"), "/admin-contest-info") {
        inputs.file(it.file("index.html"))
        inputs.dir(layout.projectDirectory.dir("admin-router/src"))
    }
    val buildAdminRouter = pnpmBuild("pnpm_run_buildAdminRouter", layout.projectDirectory.dir("admin-router"), "/") {
        inputs.file(it.file("index.html"))
    }
    val buildLocatorAdmin = pnpmBuild("pnpm_run_buildLocatorAdmin", layout.projectDirectory.dir("locator"), "/locator") {
    }
    val overlayConfigSchema = named<PnpmTask>("pnpm_run_overlayConfigSchema") {
        setInputs(layout.projectDirectory.dir("overlay"))
        outputs.cacheIf { true }
        outputs.files(layout.projectDirectory.file("overlay/schemas/visual-config.schema.json"))
    }
    artifacts {
        jsonSchemasProvider(overlayConfigSchema)
        adminOverlayJsAppProvider(buildAdminOverlay)
        adminContestInfoJsAppProvider(buildAdminContestInfo)
        adminRouterJsAppProvider(buildAdminRouter)
        overlayJsAppProvider(buildOverlay)
        locatorAdminJsAppProvider(buildLocatorAdmin)
    }
    //val installBrowsers = named<NpmTask>("pnpm_run_install-browsers") // probably want to cache it somehow
    val runTests = named<PnpmTask>("pnpm_run_test") {
        //dependsOn(installBrowsers)
        dependsOn(":backend:release")
    }
    check {
        dependsOn(runTests, checkTsExport)
    }
    assemble {
        dependsOn(buildOverlay, buildAdminOverlay, buildAdminContestInfo, buildAdminRouter, buildLocatorAdmin)
    }
}

