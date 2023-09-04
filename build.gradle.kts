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
    register<Sync>("release") {
        destinationDir = rootDir.resolve("artifacts/")

        listOf(
            ":backend",
            ":cds-converter",
            ":reactions-bot",
            ":sniper-tools",
        ).forEach { projectId ->
            from(project(projectId).tasks.named("shadowJar"))
        }
    }

    register<Sync>("doc") {
        destinationDir = rootDir.resolve("_site/cds/")

        from(project(":cds").tasks.named("dokkaHtml"))
    }
}

subprojects {
    group = "org.icpclive"
    version = rootProject.findProperty("build_version")!!

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(11)
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
        tasks.named<ShadowJar>("shadowJar") {
            mergeServiceFiles()

            archiveClassifier = null
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
        }
    }
}