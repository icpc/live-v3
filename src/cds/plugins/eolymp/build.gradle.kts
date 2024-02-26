import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer

plugins {
    id("live.cds-plugin-conventions")
    alias(libs.plugins.graphql)
}

val graphQlDirectory = project.projectDir.resolve("src").resolve("main").resolve("graphql")
val graphQlSchemaFile = graphQlDirectory.resolve("eolymp.graphql")

graphql {
    client {
        packageName = "com.eolymp.graphql"
        schemaFile = graphQlSchemaFile
        queryFileDirectory = graphQlDirectory.resolve("queries").absolutePath
        serializer = GraphQLSerializer.KOTLINX
    }
}

tasks {
    graphqlIntrospectSchema {
        endpoint = "https://api.eolymp.com/spaces/00000000-0000-0000-0000-000000000000/graphql"
        outputFile = graphQlSchemaFile
    }
}


dependencies {
    implementation(projects.cds.ktor)
    implementation(projects.cds.utils)
    implementation(libs.graphql.ktor.client)
}