plugins {
    kotlin("jvm")

    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")

    // Apply maven-publish to provide library as dependency.
    `maven-publish`
}

val projectVersion: String = (System.getenv("VERSION") ?: "").ifEmpty { "1.0" }

group = "links-detektor"
version = projectVersion

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    detektPlugins(libs.detekt)

    implementation(libs.kotlin.stdlib)

    // Logging dependencies
    implementation(libs.groovy)
    implementation(libs.jansi)
    implementation(libs.logback)
    implementation(libs.logback.groovy)
    implementation(libs.logging)

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("com.google.guava:guava:32.0.1-jre")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.apache.commons:commons-math3:3.6.1")

    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Testing dependencies
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testRuntimeOnly(libs.junit.engine)
}


// Configure test task
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Publishing meta for Jitpack.
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.slothiesmooth"
            artifactId = "link-detektor"
            version = projectVersion

            from(components["java"])
        }
    }
}
