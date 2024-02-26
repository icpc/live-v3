import org.gradle.api.artifacts.VersionCatalogsExtension

val catalogs = extensions.getByType<VersionCatalogsExtension>()


plugins {
    id("live.library-conventions")
    id("live.ksp-conventions")
}

dependencies {
    api(project(":cds:core"))
    implementation(catalogs.named("libs").findLibrary("kotlinx-datetime").get())
    implementation(catalogs.named("libs").findLibrary("kotlinx-serialization-json").get())
    testImplementation(project(":cds:tests"))
}