import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node") version "3.4.0"
}

node {
    version.set("16.14.0")
    npmInstallCommand.set("ci")
    download.set(rootProject.findProperty("npm.download") == "true")
}

tasks {
    named<NpmTask>("npm_run_buildOverlay") {
        environment.set(mapOf("PUBLIC_URL" to "/overlay"))
    }
    named<NpmTask>("npm_run_buildAdmin") {
        environment.set(mapOf("PUBLIC_URL" to "/admin"))
    }
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}