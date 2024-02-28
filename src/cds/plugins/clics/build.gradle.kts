plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

dependencies {
    api(projects.cds.core)
    implementation(projects.cds.ktor)
    implementation(projects.cds.utils)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(projects.clicsApi)
    ksp(projects.ksp)
    compileOnly(projects.ksp)
    testImplementation(projects.cds.tests)
}