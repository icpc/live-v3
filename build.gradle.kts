import org.icpclive.gradle.tasks.CheckExportedFiles

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
    register("doc") {
        dependsOn(":cds:full:dokkaGenerate")
    }
    val copySchemas = register<Sync>("copySchemas") {
        from(configurations.jsonSchemasResolver)
        into(schemasExportLocation)
    }
    val gen = register("gen") {
        dependsOn(copySchemas)
        dependsOn(":frontend:copyGeneratedTs")
    }
    val checkSchemasExport = register<CheckExportedFiles>("checkSchemasExport") {
        from(configurations.jsonSchemasResolver)
        exportLocation = schemasExportLocation
        fixTask = gen.name
    }
    check {
        dependsOn(checkSchemasExport)
    }
}
