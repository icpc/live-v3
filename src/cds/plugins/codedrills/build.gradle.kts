import com.google.protobuf.gradle.*

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
}

kotlin {
    explicitApi()
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
    api(projects.cds.core)
    implementation(projects.cds.utils)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    ksp(projects.ksp)
    implementation(libs.protobuf)
    runtimeOnly(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    compileOnly(projects.ksp)
}