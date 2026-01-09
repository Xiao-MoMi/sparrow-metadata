plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://repo.momirealms.net/releases")
}

dependencies {
    compileOnly("org.jetbrains:annotations:${rootProject.properties["jetbrains_annotations_version"]}")
    compileOnly("com.google.guava:guava:${rootProject.properties["guava_version"]}")
    compileOnly("org.slf4j:slf4j-api:${rootProject.properties["slf4j_version"]}")
    implementation("io.lettuce:lettuce-core:${rootProject.properties["lettuce_version"]}")
    implementation("org.mongodb:mongodb-driver-sync:${rootProject.properties["mongodb_version"]}")
    implementation("com.github.ben-manes.caffeine:caffeine:${rootProject.properties["caffeine_version"]}")
    implementation("net.momirealms:sparrow-redis-message-broker:${rootProject.properties["message_broker_version"]}")
    implementation("org.yaml:snakeyaml:${rootProject.properties["snake_yaml_version"]}")
}