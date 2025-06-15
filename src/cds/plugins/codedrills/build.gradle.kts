@file:OptIn(ExperimentalAbiValidation::class)

import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("live.cds-plugin-conventions")
    alias(libs.plugins.protobuf)
}

kotlin {
    abiValidation {
        filters {
            excluded {
                byNames.add("io.codedrills.proto.**")
            }
        }
    }
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

dependencies {
    implementation(projects.cds.utils)
    implementation(libs.protobuf)
    runtimeOnly(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
}