plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

tasks {
    register<Task>("doc") {
        dependsOn(project(":cds:full").tasks.named("dokkaGenerate"))
    }
    register<Task>("gen") {
        dependsOn(project("schema-generator").tasks.named("gen"))
    }
}
