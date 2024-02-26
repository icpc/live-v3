plugins {
    id("org.jetbrains.dokka")
}

tasks {
    dokkaHtmlMultiModule {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("_site").dir("cds"))
        moduleName.set("ICPC-live contest data parser")
    }
}
