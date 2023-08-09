rootProject.name = "live-v3"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {}
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":sniper-tools", ":reactions-bot", ":common", ":cds", ":frontend", ":backend", ":cds-converter", ":schema-generator")
project(":sniper-tools").projectDir = file("src/sniper-tools")
project(":common").projectDir = file("src/common")
project(":reactions-bot").projectDir = file("src/reactions-bot")
project(":cds").projectDir = file("src/cds")
project(":frontend").projectDir = file("src/frontend")
project(":backend").projectDir = file("src/backend")
project(":cds-converter").projectDir = file("src/cds-converter")
project(":schema-generator").projectDir = file("src/schema-generator")
