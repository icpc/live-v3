plugins {
    id("com.gradle.develocity") version "3.17.5"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
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
    "cds:tests",
    "cds:ktor",
    "cds:cli",
    "cds:full",
    "cds:utils",
    "ksp",
    "cds-converter",
    "clics-api",
    "frontend",
    "schema-generator",
    "server-shared",
    "oracle-tools",
    "faker",
    "user-archive"
)

val cdsPlugins = listOf(
    "allcups",
    "atcoder",
    "cats",
    "clics",
    "cms",
    "codedrills",
    "codeforces",
    "dmoj",
    "ejudge",
    "eolymp",
    "krsu",
    "merger",
    "noop",
    "nsu",
    "pcms",
    "testsys",
    "yandex",
)

for (projectName in simpleProjects) {
    include(":$projectName")
    project(":$projectName").projectDir = file("src/${projectName.replace(":", "/")}")
}

for (projectName in cdsPlugins) {
    include(":cds:plugins:$projectName")
    project(":cds:plugins:$projectName").projectDir = file("src/cds/plugins/$projectName")
}


develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}
