import org.icpclive.gradle.tasks.CheckExportedFiles
import org.gradle.util.internal.VersionNumber
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


class DevVersion(val major: Int, val minor: Int, val patch: Int, val buildId: Int) : Comparable<DevVersion> {

    companion object {
        private val REGEX = Regex("""^(\d+)\.(\d+)\.(\d+)-dev-(\d+)$""")

        fun parse(version: String): DevVersion? {
            return REGEX.matchEntire(version)?.destructured?.let { (ma, mi, pa, bi) ->
                DevVersion(ma.toInt(), mi.toInt(), pa.toInt(), bi.toInt())
            }
        }
    }

    override fun compareTo(other: DevVersion): Int = compareBy<DevVersion>(
        { it.major }, { it.minor }, { it.patch }, { it.buildId }
    ).compare(this, other)

    override fun toString(): String = "$major.$minor.$patch-dev-$buildId"
}

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
            val metadataUrl = "https://redirector.kotlinlang.org/maven/dev/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml"
            val metadataXml = URI(metadataUrl).toURL().readText()

            val versionRegex = Regex("<version>(.*?)</version>")
            val allVersions = versionRegex.findAll(metadataXml).map { it.groupValues[1] }.toList()

            // Filter: Must have '-dev-', Must NOT have 'vega'
            val latestDev = allVersions
                .mapNotNull { DevVersion.parse(it) }
                .maxOrNull()

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
