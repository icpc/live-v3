import com.github.gradle.node.npm.task.NpmTask

plugins {
    alias(libs.plugins.node)
}

node {
    version.set("16.14.0")
    npmInstallCommand.set("ci")
    download.set(rootProject.findProperty("npm.download") == "true")
}

tasks {
    npmInstall {
        inputs.file("admin/package.json")
        inputs.file("overlay/package.json")
    }
    named<NpmTask>("npm_run_buildOverlay") {
        environment.set(mapOf("PUBLIC_URL" to "/overlay"))
        inputs.dir("overlay/src")
        inputs.dir("overlay/public")
        inputs.file("overlay/package.json")
        outputs.dir("overlay/build")
    }
    named<NpmTask>("npm_run_buildAdmin") {
        environment.set(mapOf("PUBLIC_URL" to "/admin"))
        inputs.dir("admin/src")
        inputs.dir("admin/public")
        inputs.file("admin/package.json")
        outputs.dir("admin/build")
    }
}
