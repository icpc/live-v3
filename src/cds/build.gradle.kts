import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.google.protobuf.gradle.*
import java.net.*

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.graphql)
    alias(libs.plugins.ksp)
    alias(libs.plugins.bcv)
}

apiValidation {
    ignoredPackages.addAll(listOf(
        "com.eolymp.graphql",
        "io.codedrills.proto",
    ))
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
    plugins {
        id("grpc") {
            artifact = libs.grpc.gen.java.get().toString()
        }
        id("grpckt") {
            artifact = libs.grpc.gen.kotlin.get().toString() + ":jdk8@jar"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").configureEach {
            plugins {
                id("grpc")
                id("grpckt")
            }
            builtins {
                id("kotlin")
            }
        }
    }
}

tasks {
    dokkaHtml {
        moduleName.set("ICPC-live contest data parser")
        dokkaSourceSets.configureEach {
            perPackageOption {
                matchingRegex.set(".*")
                reportUndocumented.set(true)
                sourceLink {
                    localDirectory.set(projectDir)
                    remoteUrl.set(URI("https://github.com/icpc/live-v3/tree/main/src/cds").toURL())
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }

    test {
        inputs.dir("testData/")
        inputs.dir("../../config/")
        dependsOn(apiCheck)
    }
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
    api(libs.kotlinx.collections.immutable)
    implementation(projects.common)
    implementation(projects.clicsApi)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json5)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.protobuf)
    implementation(libs.graphql.ktor.client)
    implementation(libs.cli)
    runtimeOnly(libs.grpc.netty)
    ksp(projects.ksp)
    compileOnly(projects.ksp)

    testImplementation(libs.kotlin.junit)
}