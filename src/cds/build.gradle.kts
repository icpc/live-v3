import com.google.protobuf.gradle.*
import java.net.URI

plugins {
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
            // should be moved to another package, as reused by exporter
            perPackageOption {
                matchingRegex.set("org.icpclive.cds.clics.*")
                suppress.set(true)
            }
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

dependencies {
    api(libs.kotlinx.collections.immutable)
    implementation(projects.common)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json5)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.protobuf)
    runtimeOnly(libs.grpc.netty)

    testImplementation(libs.kotlin.junit)
}