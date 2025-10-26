plugins {
    `kotlin-dsl`
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kxs.ts.gen.core)
}
