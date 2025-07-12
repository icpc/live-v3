plugins {
    id("live.kotlin-conventions")
}

tasks {
    test {
        inputs.dir(rootProject.layout.projectDirectory.dir("config"))
    }
}

dependencies {
    implementation(projects.cds.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(projects.cds.full)
    implementation(libs.kotlin.junit)
}