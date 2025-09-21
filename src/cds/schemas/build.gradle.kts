plugins {
    id("live.library-conventions")
}

tasks {
    processResources {
        from(project(":schema-generator").tasks.named("generateAllSchemas")) {
            into("schemas")
        }
    }
}