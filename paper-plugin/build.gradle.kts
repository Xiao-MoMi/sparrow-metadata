import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
    id("maven-publish")
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    implementation(project(":core"))
    compileOnly("io.papermc.paper:paper-api:${rootProject.properties["paper_version"]}-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:${rootProject.properties["placeholder_api_version"]}")
    implementation("net.momirealms:sparrow-redis-message-broker:${rootProject.properties["message_broker_version"]}")
    implementation("io.lettuce:lettuce-core:${rootProject.properties["lettuce_version"]}")
    implementation("org.mongodb:mongodb-driver-sync:${rootProject.properties["mongodb_version"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    dependsOn(tasks.clean)
}

artifacts {
    implementation(tasks.shadowJar)
}

tasks {
    shadowJar {
        archiveClassifier = ""
        archiveFileName = "sparrow-metadata-${rootProject.properties["project_version"]}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }
}

paper {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "net.momirealms.sparrow.metadata.SparrowMetaDataPlugin"
    bootstrapper = "net.momirealms.sparrow.metadata.SparrowMetaDataBootstrap"
    version = rootProject.properties["project_version"] as String
    name = "SparrowMetaData"
    apiVersion = "1.21"
    authors = listOf("XiaoMoMi")
    foliaSupported = true
    serverDependencies {
        register("PlaceholderAPI") { required = false }
    }
}

tasks.register("startDevServer", RunServer::class) {
    group = "run paper"
    minecraftVersion("1.21.11")
    pluginJars.from(tasks.shadowJar.flatMap { it.archiveFile })
    runDirectory = rootProject.layout.projectDirectory.dir("runPaper")
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    systemProperties["com.mojang.eula.agree"] = true
    jvmArgs("-Dsun.stdout.encoding=UTF-8")
    jvmArgs("-Dsun.stderr.encoding=UTF-8")
    jvmArgs("-Ddisable.watchdog=true")
    jvmArgs("-Xlog:redefine+class*=info")
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}