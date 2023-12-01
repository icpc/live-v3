import com.google.protobuf.gradle.*
import java.net.URI

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.protobuf)
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
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.kotlinx.collections.immutable)
    implementation(projects.common)
    implementation(projects.clicsApi)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json5)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.protobuf)
    runtimeOnly(libs.grpc.netty)

    testImplementation(libs.kotlin.junit)
}