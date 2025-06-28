plugins {
    kotlin("jvm")

    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")

    // Apply Dokka for Kotlin documentation
    id("org.jetbrains.dokka")

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

// Configure Dokka to generate KDoc documentation
tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))

    // Set custom Dokka plugin options
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
                {
                  "customStyleSheets": [],
                  "customAssets": [],
                  "footerMessage": "© 2025 Slothie Smooth - Links Detektor",
                  "separateInheritedMembers": true
                }
            """
        )
    )

    // Configure Dokka options
    dokkaSourceSets.configureEach {
        // Set project name and description
        moduleName.set("Links Detektor")
        moduleVersion.set(projectVersion)

        // Documentation settings
        includeNonPublic.set(false)
        skipEmptyPackages.set(true)
        reportUndocumented.set(true)
        jdkVersion.set(21)

        // Add source links to GitHub
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl.set(uri("https://github.com/slothiesmooth/links-detektor/blob/main/lib/src/main/kotlin").toURL())
            remoteLineSuffix.set("#L")
        }

        // Customize documentation appearance
        perPackageOption {
            matchingRegex.set("com\\.slothiesmooth\\.linksdetektor\\.internal.*")
            suppress.set(true)
        }

        // Add custom documentation links
        externalDocumentationLink {
            url.set(uri("https://kotlinlang.org/api/latest/jvm/stdlib/").toURL())
        }

        // Include module documentation
        includes.from("packages.md")
    }
}

// Create javadoc JAR with Dokka-generated documentation
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
}

// Create sources JAR
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Publishing configuration for Maven Central
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.slothiesmooth"
            artifactId = "links-detektor"
            version = projectVersion

            from(components["java"])

            // Include javadoc and sources JARs
            artifact(javadocJar)
            artifact(sourcesJar)

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

// Ensure documentation is generated before publishing
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.dokkaHtml)
}

tasks.withType<PublishToMavenLocal>().configureEach {
    dependsOn(tasks.dokkaHtml)
}
