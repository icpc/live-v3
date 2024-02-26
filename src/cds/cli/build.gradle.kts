plugins {
    id("live.library-conventions")
}

dependencies {
    api(projects.cds.core)
    api(libs.cli)
    implementation(projects.cds.utils)
}