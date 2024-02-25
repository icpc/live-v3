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

val simpleProjects = listOf(
    "backend",
    "backend-api",
    "cds",
    "cds:core",
    "cds:plugins",
    "cds:tests",
    "ksp",
    "cds-converter",
    "clics-api",
    "common",
    "frontend",
    "reactions-bot",
    "schema-generator",
    "sniper-tools",
    "faker",
    "user-archive",
)

val cdsPlugins = listOf(
    "allcups",
    "atcoder",
    "cats",
    "clics",
    "cms",
    "codedrills",
    "codeforces",
    "ejudge",
    "eolymp",
    "krsu",
    "noop",
    "nsu",
    "pcms",
    "testsys",
    "yandex"
)

for (projectName in simpleProjects) {
    include(":$projectName")
    project(":$projectName").projectDir = file("src/${projectName.replace(":", "/")}")
}

for (projectName in cdsPlugins) {
    include(":cds:plugins:$projectName")
    project(":cds:plugins:$projectName").projectDir = file("src/cds/plugins/$projectName")
}


gradleEnterprise.buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}