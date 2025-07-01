plugins {
    // Apply Kotlin plugin at the root level with version
    kotlin("jvm") version "2.2.0" apply false

    // Other plugins that might be needed at the root level with versions
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("com.github.jakemarsden.git-hooks") version "0.0.2" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
    id("com.vanniktech.maven.publish") version "0.33.0" apply false
}

// Root project configuration
repositories {
    mavenCentral()
    gradlePluginPortal()
}
