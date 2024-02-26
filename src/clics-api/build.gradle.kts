plugins {
    id("live.library-conventions")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    implementation(projects.cds.utils)
}