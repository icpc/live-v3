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
}
