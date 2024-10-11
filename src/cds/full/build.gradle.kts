import org.gradle.api.internal.catalog.DelegatingProjectDependency

plugins {
    id("live.library-conventions")
}

apiValidation {
    validationDisabled = true
}

dokka {
    dokkaPublications.named("html").configure {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("_site").dir("cds"))
    }
    moduleName.set("ICPC-live contest data parser")
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