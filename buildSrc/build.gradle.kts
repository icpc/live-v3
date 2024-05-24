
plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(libs.gradleplugin.kotlin.jvm)
    implementation(libs.gradleplugin.kotlin.serialization)
    implementation(libs.gradleplugin.shadow)
    implementation(libs.gradleplugin.dokka)
    implementation(libs.gradleplugin.bcv)
    implementation(libs.gradleplugin.ksp)
}