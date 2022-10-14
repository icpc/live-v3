java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.icpclive"
version = rootProject.findProperty("build_version")!!
application {
    mainClass.set("org.icpclive.sniper.SniperMover")
}


tasks {
    shadowJar {
        archiveFileName.set("sniper-tools-${project.version}.jar")
    }
    task<Copy>("release") {
        from(shadowJar)
        destinationDir = rootProject.rootDir.resolve("artifacts")
    }
}

repositories {
    mavenCentral()
}

dependencies {
}
