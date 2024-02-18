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
    register<Sync>("doc") {
        destinationDir = rootDir.resolve("_site/cds/")

        from(project(":cds").tasks.named("dokkaHtml"))
    }

    // If you invoke a gen task, :schema-generator:gen will be invoked. It's defined in :schema-generator project
    // since that project is already aware of global location for schema testing purposes.
}

subprojects {
    group = "org.icpclive"
    version = rootProject.findProperty("build_version")!!

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