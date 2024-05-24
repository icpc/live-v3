plugins {
    id("live.library-conventions")
}

apiValidation {
    validationDisabled = true
}

dependencies {
    api(projects.cds.plugins.allcups)
    api(projects.cds.plugins.atcoder)
    api(projects.cds.plugins.cats)
    api(projects.cds.plugins.clics)
    api(projects.cds.plugins.cms)
    api(projects.cds.plugins.codedrills)
    api(projects.cds.plugins.codeforces)
    api(projects.cds.plugins.dmoj)
    api(projects.cds.plugins.ejudge)
    api(projects.cds.plugins.eolymp)
    api(projects.cds.plugins.krsu)
    api(projects.cds.plugins.noop)
    api(projects.cds.plugins.nsu)
    api(projects.cds.plugins.pcms)
    api(projects.cds.plugins.testsys)
    api(projects.cds.plugins.yandex)
    api(projects.cds.ktor)
    api(projects.cds.cli)
    api(projects.cds.utils)
}