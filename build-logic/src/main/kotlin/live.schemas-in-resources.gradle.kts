plugins {
    id("live.kotlin-conventions")
    id("live.file-sharing")
}

tasks {
    processResources {
        from(configurations["jsonSchemasResolver"]) {
            into("schemas")
        }
    }
}