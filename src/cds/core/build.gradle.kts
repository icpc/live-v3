plugins {
    id("live.library-conventions")
    id("live.ksp-conventions")
}

dependencies {
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.coroutines.core)
    implementation(projects.cds.utils)
    implementation(libs.kotlin.reflect)
    implementation(libs.ktor.http)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    ksp(projects.ksp)
    compileOnly(projects.ksp)
}