plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.31"

    // Apply kotlin-linter for enforcing code style.
    id("org.jmailen.kotlinter") version "3.4.5"

    // Apply shadowJar plugin to generate fatJar for easy execution.
    id("com.github.johnrengelman.shadow") version "7.0.0"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

group = "in.technowolf"
version = "1.0.0"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
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

// Adding task for kotlinter pre-push hook
tasks.check {
    dependsOn("installKotlinterPrePushHook")
}

kotlinter {
    ignoreFailures = false
    indentSize = 4
    reporters = arrayOf("checkstyle", "plain")
    experimentalRules = false
    disabledRules = arrayOf("import-ordering", "indent")
}
