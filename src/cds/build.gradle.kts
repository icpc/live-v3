import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.*
plugins {
    alias(libs.plugins.dokka)
}

subprojects {
    if (name != "tests" && name != "plugins") {
        apply(plugin = "org.jetbrains.dokka")

        tasks.withType<DokkaTaskPartial>().configureEach {
            dokkaSourceSets.configureEach {
                perPackageOption {
                    matchingRegex.set(".*")
                    reportUndocumented.set(true)
                    sourceLink {
                        val projectDir = "https://github.com/icpc/live-v3/tree/main/"
                        localDirectory.set(rootProject.projectDir)
                        remoteUrl.set(URI(projectDir).toURL())
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}

tasks {
    dokkaHtmlMultiModule {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("_site").dir("cds"))
        moduleName.set("ICPC-live contest data parser")
    }
}
