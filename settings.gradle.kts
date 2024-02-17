plugins {
    `gradle-enterprise`
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

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

include(
    ":backend",
    ":backend-api",
    ":cds",
    ":cds:ksp",
    ":cds-converter",
    ":clics-api",
    ":common",
    ":frontend",
    ":reactions-bot",
    ":schema-generator",
    ":sniper-tools",
    ":faker",
    ":user-archive"
)
project(":backend").projectDir = file("src/backend")
project(":backend-api").projectDir = file("src/backend-api")
project(":cds").projectDir = file("src/cds")
project(":cds:ksp").projectDir = file("src/cds/ksp")
project(":cds-converter").projectDir = file("src/cds-converter")
project(":clics-api").projectDir = file("src/clics-api")
project(":common").projectDir = file("src/common")
project(":frontend").projectDir = file("src/frontend")
project(":reactions-bot").projectDir = file("src/reactions-bot")
project(":schema-generator").projectDir = file("src/schema-generator")
project(":sniper-tools").projectDir = file("src/sniper-tools")
project(":faker").projectDir = file("src/faker")
project(":user-archive").projectDir = file("src/user-archive")

gradleEnterprise.buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}