plugins {
    id("live.kotlin-conventions")
    id("com.google.devtools.ksp")
}

dependencies {
    compileOnly(project(":ksp"))
    ksp(project(":ksp"))
}
