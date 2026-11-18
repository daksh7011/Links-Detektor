plugins {
    alias(libs.plugins.kotlin.jvm)
    // Versionless: build-logic already puts both plugins on the classpath.
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

val projectVersion: String = System.getenv("VERSION") ?: libs.versions.lib.get()

group = "com.slothiesmooth"
version = projectVersion

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.kotlinx.collections.immutable)

    testImplementation(kotlin("test"))
}

// Configure all JAR names
tasks.withType<Jar> {
    archiveBaseName.set("links-detektor")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dokka {
    moduleName.set("Links Detektor")
}

mavenPublishing {
    coordinates("com.slothiesmooth", "links-detektor", projectVersion)
    pom {
        name.set("Links Detektor")
        description.set("A robust library for detecting and extracting URLs from text content. It provides a powerful URL detection engine that can identify various URL formats within arbitrary text.")
        inceptionYear.set("2025")
        url.set("https://github.com/daksh7011/links-detektor")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/daksh7011/links-detektor/LICENSE")
                distribution.set("https://github.com/daksh7011/links-detektor/LICENSE")
            }
        }
        developers {
            developer {
                id.set("slothiesmooth")
                name.set("Daksh Desai")
                url.set("https://slothiesmooth.dev")
                email.set("contact@slothiesmooth.com")
            }
        }
        scm {
            url.set("https://github.com/daksh7011/links-detektor")
            connection.set("scm:git:git://github.com/daksh7011/links-detektor.git")
            developerConnection.set("scm:git:git://github.com/daksh7011/links-detektor.git")
        }
    }

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
