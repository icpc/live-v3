plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.ktor)
}

group = "org.icpclive"
version = rootProject.findProperty("build_version")!!
application {
    mainClass.set("org.icpclive.sniper.ApplicationKt")
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

tasks {
    jar {
        archiveFileName.set("sniper-tools-${project.version}-part.jar")
    }
    shadowJar {
        archiveFileName.set("sniper-tools-${project.version}.jar")
    }
    named<JavaExec>("run") {
        this.workingDir(rootDir.resolve("."))
        this.args = listOfNotNull(project.properties["live.overlayUrl"].let { "-P:live.overlayUrl=$it" })

    }
    task<Copy>("release") {
        from(shadowJar)
        destinationDir = rootProject.rootDir.resolve("artifacts")
    }
    val jsBuildPath = project.buildDir.resolve("js")
    val copyJsAdmin = register<Copy>("copyJsAdmin") {
        from(project(":frontend").tasks["npm_run_buildAdmin"])
        destinationDir = jsBuildPath.resolve("admin")
    }
    register("buildJs") {
        dependsOn(copyJsAdmin)
        outputs.dir(jsBuildPath)
    }
}

sourceSets {
    main {
        resources {
            srcDirs(tasks["buildJs"].outputs)
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.autoHeadResponse)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.websockets)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.cds)
    implementation(projects.common)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.ktor.server.tests)
}
