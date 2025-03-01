plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.base"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
intellij {
    version.set("2024.3.3")
    type.set("GO") // Specifies this is a GoLand plugin, not IDEA
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    // Patch the plugin XML
    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("243.*")
    }

    // Sign the plugin (only if needed)
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    // Publish the plugin (only if needed)
    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // Build task to create the zip file of the plugin
    buildPlugin {
        // This task will create the zip file of the plugin
        dependsOn("build")
        archiveFileName.set("my-plugin.zip")
        destinationDirectory.set(file("${project}/libs"))
    }
}
