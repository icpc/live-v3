import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.graphql)
}

kotlin {
    explicitApi()
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
    api(projects.cds.core)
    implementation(projects.cds.utils)
    implementation(projects.cds.ktor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.graphql.ktor.client)
    ksp(projects.ksp)
    compileOnly(projects.ksp)
}