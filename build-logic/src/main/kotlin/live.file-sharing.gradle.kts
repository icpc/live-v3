import org.gradle.kotlin.dsl.provideDelegate

fun ConfigurationContainer.allThree(name: String, desc: String) {
        val filesUsageAttribute = objects.named<Usage>("live.files.$name")
        val declarator by dependencyScope(name) {
            description = "Declare dependencies on $desc from other subprojects"
            dependencies.add(project.dependencies.project(project.path))
        }

        consumable("${name}Provider") {
            description = "Provides $desc to other subprojects"
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, filesUsageAttribute)
            }
        }

        resolvable("${name}Resolver") {
            description = "Resolves $desc from other subprojects"

            extendsFrom(declarator)

            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, filesUsageAttribute)
            }
        }
}

configurations {
    allThree("jsonSchemas", "json schemas")
    allThree("tsInterfaces", "type script interfaces")
    allThree("adminJsApp", "admin js app")
    allThree("overlayJsApp", "overlay js app")
    allThree("locatorAdminJsApp", "locator admin js app")
    allThree("applicationJar", "application jar")
}