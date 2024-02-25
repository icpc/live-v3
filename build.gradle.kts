import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // versions are set in dependencies block for build.gradle.kts
    // apply false brings the plugins into the Gradle script classpath (see import above)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
}

tasks {
    register<Task>("doc") {
        dependsOn(project(":cds").tasks.named("dokkaHtmlMultiModule"))
    }

    // If you invoke a gen task, :schema-generator:gen will be invoked. It's defined in :schema-generator project
    // since that project is already aware of global location for schema testing purposes.
}

subprojects {
    group = "org.icpclive"
    version = rootProject.findProperty("build_version")!!


    plugins.withType<MavenPublishPlugin> {
        afterEvaluate {
            configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/icpc/live-v3")
                        credentials {
                            username = System.getenv("GITHUB_ACTOR")
                            password = System.getenv("GITHUB_TOKEN")
                        }
                    }
                }
                publications {
                    create<MavenPublication>("mavenJava${this@subprojects.name}") {
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
                        from(components["java"])
                        groupId = "org.icpclive"
                        version = rootProject.findProperty("build_version")!!.toString()
                        artifactId = "org.icpclive.cds.${this@subprojects.name}"
                    }
                }
            }
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(17)
            }
        }

        tasks.named<Jar>("jar") {
            archiveClassifier = "just"
        }

        tasks.named<Test>("test") {
            useJUnitPlatform()
        }
    }

    // Technically, Ktor pulls this too, but reconfigures...
    plugins.withType<ShadowJavaPlugin> {
        tasks.register<Sync>("release") {
            destinationDir = rootDir.resolve("artifacts/")
            preserve { include("*") }
            from(tasks.named("shadowJar"))
        }
        tasks.named<ShadowJar>("shadowJar") {
            mergeServiceFiles()

            archiveClassifier = null
        }
    }

    tasks {
        withType<KotlinCompile>().configureEach {
            kotlinOptions {
                compilerOptions {
                    freeCompilerArgs.add("-Xjvm-default=all")
                    optIn = listOf("kotlinx.serialization.ExperimentalSerializationApi")
                }
                allWarningsAsErrors = true
            }
        }
    }
}