plugins {
    `kotlin-dsl`
}

val workerJobs = sourceSets.create("workerJobs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.gradleplugin.kotlin.jvm)
    implementation(libs.gradleplugin.kotlin.serialization)
    implementation(libs.gradleplugin.dokka)
    implementation(libs.gradleplugin.ksp)
    implementation(libs.gradleplugin.shadow)

    implementation(workerJobs.output)

    add(workerJobs.compileOnlyConfigurationName, gradleApi())
    add(workerJobs.compileOnlyConfigurationName, libs.kotlinx.serialization.json)
    add(workerJobs.compileOnlyConfigurationName, libs.kxs.ts.gen.core)
}
