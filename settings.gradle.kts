rootProject.name = "sparrow-metadata"
include(":core")
include(":paper-plugin")
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}