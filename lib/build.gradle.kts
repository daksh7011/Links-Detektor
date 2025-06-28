plugins {
    kotlin("jvm")

    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")

    // Apply maven-publish to provide library as dependency.
    `maven-publish`

    // Apply signing plugin for Maven Central requirements
    signing
}

val projectVersion: String =  libs.versions.lib.get()

group = "com.slothiesmooth"
version = projectVersion

repositories {
    mavenCentral()
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

    api(libs.commonsMath3)

    implementation(libs.commonsLang3)

    // Testing dependencies
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.engine)
}

detekt {
    buildUponDefaultConfig = true
    autoCorrect = true
    config.from(rootProject.files("detekt.yml"))
}


tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Publishing configuration for Maven Central
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.slothiesmooth"
            artifactId = "links-detektor"
            version = projectVersion

            from(components["java"])

            // Required metadata for Maven Central
            pom {
                name.set("Links Detektor")
                description.set("A library for detecting links in text")
                url.set("https://github.com/slothiesmooth/links-detektor")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/slothiesmooth/links-detektor/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("slothiesmooth")
                        name.set("Daksh Desai")
                        email.set("contact@slothiesmooth.com")
                        url.set("https://slothiesmooth.dev")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/slothiesmooth/links-detektor.git")
                    developerConnection.set("scm:git:ssh://github.com/slothiesmooth/links-detektor.git")
                    url.set("https://github.com/slothiesmooth/links-detektor")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrhUsername") as String?
                password = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrhPassword") as String?
            }
        }
    }
}

// Signing configuration
signing {
    val signingKey = System.getenv("SIGNING_KEY") ?: project.findProperty("signingKey") as String?
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: project.findProperty("signingPassword") as String?

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications["maven"])
}
