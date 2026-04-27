plugins {
    application
}

application {
    mainClass = "io.gridkit.playground.MainKt"
}

dependencies {
    implementation(project(":gridkit-core"))
}
