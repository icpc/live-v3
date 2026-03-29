import org.icpclive.gradle.tasks.CheckExportedFiles
import java.net.URI
import java.net.URL

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    id("live.file-sharing")
    base
}

dependencies {
    jsonSchemas(projects.frontend)
    jsonSchemas(projects.cds.full)
}

val schemasExportLocation = project.layout.projectDirectory.dir("schemas")


tasks {
    val doc by registering {
        dependsOn(":cds:full:dokkaGenerate")
    }
    val copySchemas by registering(Sync::class) {
        from(configurations.jsonSchemasResolver)
        into(schemasExportLocation)
    }
    val gen by registering {
        dependsOn(copySchemas)
        dependsOn(":frontend:copyGeneratedTs")
    }
    val checkSchemasExport by registering(CheckExportedFiles::class) {
        from(configurations.jsonSchemasResolver)
        exportLocation = schemasExportLocation
        fixTask = gen.name
    }
    check {
        dependsOn(checkSchemasExport)
    }

    val upgradeKotlinDev by registering {
        group = "maintenance"
        description = "Checks JetBrains dev repo and updates libs.versions.toml"

        notCompatibleWithConfigurationCache("This task performs network calls and modifies source files.")
        outputs.upToDateWhen { false }

        doLast {
            val metadataUrl = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml"
            val metadataXml = URI(metadataUrl).toURL().readText()

            // Use Regex to find all <version> tags
            val versionRegex = Regex("<version>(.*?)</version>")
            val allVersions = versionRegex.findAll(metadataXml).map { it.groupValues[1] }.toList()

            // Filter: Must have '-dev-', Must NOT have 'vega'
            val latestDev = allVersions
                .filter { it.contains("-dev-") }
                .maxByOrNull { v ->
                    v.substringAfterLast("-dev-").toInt()
                }

            if (latestDev != null) {
                val tomlFile = file("gradle/libs.versions.toml")
                val content = tomlFile.readText()

                // Replace the kotlin version line
                val updatedContent = content.replace(
                    Regex("(?m)^kotlin\\s*=\\s*[\"']([^\"']+)[\"']"),
                    "kotlin = \"$latestDev\""
                )

                if (content != updatedContent) {
                    tomlFile.writeText(updatedContent)
                    println("🚀 Updated Kotlin to $latestDev in libs.versions.toml")
                } else {
                    println("✅ Already up to date: $latestDev")
                }
            } else {
                throw GradleException("Could not find any valid dev versions at $metadataUrl")
            }
        }
    }
}
