fun createConfiguration(name: String, desc: String) {
    val filesUsageAttribute = objects.named<Usage>("live.files.$name")

    val declarator = configurations.create(name) {
        description = "Declare dependencies on $desc from other subprojects"

        isCanBeResolved = false
        isCanBeConsumed = false
        isCanBeDeclared = true
        dependencies.add(project.dependencies.project(project.path))
    }

    val provider = configurations.create("${name}Provider") {
        description = "Provides $desc to other subprojects"

        isCanBeResolved = false
        isCanBeConsumed = true
        isCanBeDeclared = false

        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, filesUsageAttribute)
        }
    }

    configurations.create("${name}Resolver") {
        description = "Resolves $desc from other subprojects"

        isCanBeResolved = true
        isCanBeConsumed = false
        isCanBeDeclared = false

        extendsFrom(declarator)

        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, filesUsageAttribute)
        }
    }

}

createConfiguration("jsonSchemas", "json schemas")
createConfiguration("tsInterfaces", "type script interfaces")
createConfiguration("adminJsApp", "admin js app")
createConfiguration("overlayJsApp", "overlay js app")
createConfiguration("locatorAdminJsApp", "locator admin js app")
createConfiguration("applicationJar", "application jar")