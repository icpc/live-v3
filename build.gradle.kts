tasks {
    register<Task>("doc") {
        dependsOn(project(":cds").tasks.named("dokkaHtmlMultiModule"))
    }
    register<Task>("gen") {
        dependsOn(project("schema-generator").tasks.named("gen"))
    }
}
