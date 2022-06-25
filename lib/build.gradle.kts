plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.31"

    // Apply kotlin-linter for enforcing code style.
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"

    // Apply shadowJar plugin to generate fatJar for easy execution.
    id("com.github.johnrengelman.shadow") version "7.0.0"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // Apply maven-publish to provide library as dependency.
    `maven-publish`
}

group = "in.technowolf"
version = "1.0.0"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("com.google.guava:guava:30.1.1-jre")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.apache.commons:commons-math3:3.6.1")

    implementation("org.apache.commons:commons-lang3:3.12.0")
}

// Adding compiler jvm version for all kotlin compile and kotlin tests compile tasks.
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}

// Kotlin linter configuration.
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    debug.set(true)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
//    enableExperimentalRules.set(true)
    disabledRules.set(setOf("indent"))
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    kotlinScriptAdditionalPaths {
        include(fileTree("scripts/"))
    }
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

// Publishing meta for Jitpack.
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "in.technowolf"
            artifactId = "link-detektor"
            version = "1.0.0"

            from(components["java"])
        }
    }
}
