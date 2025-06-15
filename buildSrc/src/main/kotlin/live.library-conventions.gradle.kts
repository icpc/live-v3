@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import java.net.*

plugins {
    id("live.kotlin-conventions")
    id("org.jetbrains.dokka")
    `java-library`
    `maven-publish`
}

kotlin {
    explicitApi()
    abiValidation {
        enabled = true
    }
}

tasks {
    val apiDump by registering {
        dependsOn(updateLegacyAbi)
    }
    val apiCheck by registering {
        dependsOn(checkLegacyAbi)
    }
}

publishing {
    publications {
        val libName = name
        create<MavenPublication>("mavenJava${libName}") {
            pom {
                name = "ICPC live contest data system parser"
                description = "Parser for a various programming competition contest systems"
                url = "https://github.com/icpc/live-v3"
                licenses {
                    license {
                        name = "The MIT License"
                        url = "http://opensource.org/licenses/MIT"
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/icpc/live-v3.git")
                    developerConnection.set("scm:git:ssh://github.com/icpc/live-v3.git")
                    url.set("https://github.com/icpc/live-v3")
                }
            }
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            java {
                withSourcesJar()
            }
            from(components["java"])
            groupId = "com.github.icpc.live-v3"
            version = rootProject.findProperty("build_version")!!.toString()
            artifactId = "org.icpclive.cds.${libName}"
        }
    }
}

dokka {
    dokkaSourceSets.configureEach {
        perPackageOption {
            matchingRegex.set(".*")
            reportUndocumented.set(true)
            sourceLink {
                val projectDir = "https://github.com/icpc/live-v3/tree/main/"
                localDirectory.set(rootProject.projectDir)
                remoteUrl.set(URI(projectDir))
                remoteLineSuffix.set("#L")
            }
        }
    }
}