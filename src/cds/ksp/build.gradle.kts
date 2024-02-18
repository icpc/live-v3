plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ksp)
    implementation(libs.kotlinx.serialization.json)
}