@file:OptIn(ExperimentalAbiValidation::class)

import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.kotlin.dsl.provideDelegate
import org.icpclive.gradle.tasks.SchemaGeneratorTask
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("live.library-conventions")
    id("live.file-sharing")
    id("live.schemas-in-resources")
}

kotlin {
    abiValidation {
        enabled = false
    }
}

dokka {
    dokkaPublications.named("html").configure {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("_site").dir("cds"))
    }
    moduleName.set("ICPC-live contest data parser")
}


tasks {
    val generateAdvancedSchema by registering(SchemaGeneratorTask::class) {
        rootClass = "org.icpclive.cds.tunning.TuningRuleList"
        title = "ICPC live advanced settings"
        fileName = "advanced"
    }
    val generateSettingsSchema by registering(SchemaGeneratorTask::class) {
        rootClass = "org.icpclive.cds.settings.CDSSettings"
        title = "ICPC live settings"
        fileName = "settings"
    }
    artifacts.jsonSchemasProvider(generateAdvancedSchema)
    artifacts.jsonSchemasProvider(generateSettingsSchema)
}

dependencies {
    fun apiAndDokka(dep: DelegatingProjectDependency) {
        api(dep)
        dokka(dep)
    }
    apiAndDokka(projects.cds.plugins.allcups)
    apiAndDokka(projects.cds.plugins.atcoder)
    apiAndDokka(projects.cds.plugins.cats)
    apiAndDokka(projects.cds.plugins.clics)
    apiAndDokka(projects.cds.plugins.cms)
    apiAndDokka(projects.cds.plugins.codedrills)
    apiAndDokka(projects.cds.plugins.codeforces)
    apiAndDokka(projects.cds.plugins.dmoj)
    apiAndDokka(projects.cds.plugins.ejudge)
    apiAndDokka(projects.cds.plugins.eolymp)
    apiAndDokka(projects.cds.plugins.krsu)
    apiAndDokka(projects.cds.plugins.merger)
    apiAndDokka(projects.cds.plugins.noop)
    apiAndDokka(projects.cds.plugins.nsu)
    apiAndDokka(projects.cds.plugins.pcms)
    apiAndDokka(projects.cds.plugins.testsys)
    apiAndDokka(projects.cds.plugins.yandex)
    apiAndDokka(projects.cds.ktor)
    apiAndDokka(projects.cds.cli)
    apiAndDokka(projects.cds.utils)
    apiAndDokka(projects.cds.core)
}