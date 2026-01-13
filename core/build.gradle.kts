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
    compileOnly("io.lettuce:lettuce-core:${rootProject.properties["lettuce_version"]}")
    compileOnly("org.mongodb:mongodb-driver-sync:${rootProject.properties["mongodb_version"]}")
    compileOnly("com.github.ben-manes.caffeine:caffeine:${rootProject.properties["caffeine_version"]}")
    compileOnly("net.momirealms:sparrow-redis-message-broker:${rootProject.properties["message_broker_version"]}")
    compileOnly("org.yaml:snakeyaml:${rootProject.properties["snake_yaml_version"]}")
}

tasks {
    shadowJar {
        archiveClassifier = ""
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    dependsOn(tasks.clean)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "releases"
            url = uri("https://repo.momirealms.net/releases")
            credentials(PasswordCredentials::class) {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("core") {
            groupId = "net.momirealms"
            artifactId = "sparrow-metadata"
            version = rootProject.properties["project_version"].toString()
            artifact(tasks["sourcesJar"])
            from(components["shadow"])
        }
    }
}

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publishes to the release repository"
    dependsOn("publishBukkitPublicationToReleasesRepository")
}
