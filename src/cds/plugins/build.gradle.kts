import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.bcv)
}

apiValidation {
    ignoredProjects.add("plugins")
}

subprojects {
    apply<MavenPublishPlugin>()
}

dependencies {
    api(projects.cds.plugins.allcups)
    api(projects.cds.plugins.atcoder)
    api(projects.cds.plugins.cats)
    api(projects.cds.plugins.clics)
    api(projects.cds.plugins.cms)
    api(projects.cds.plugins.codedrills)
    api(projects.cds.plugins.codeforces)
    api(projects.cds.plugins.ejudge)
    api(projects.cds.plugins.eolymp)
    api(projects.cds.plugins.krsu)
    api(projects.cds.plugins.noop)
    api(projects.cds.plugins.nsu)
    api(projects.cds.plugins.pcms)
    api(projects.cds.plugins.testsys)
    api(projects.cds.plugins.yandex)

    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.serialization.json)
}