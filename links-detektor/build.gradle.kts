plugins {
    kotlin("jvm")

    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")

    // Apply Dokka for Kotlin documentation
    id("org.jetbrains.dokka")
    id ("org.danilopianini.publish-on-central")
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

// TODO: Migrate to dokka2 standards
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

/*
 * The plugin comes with defaults that are useful to myself. You should configure it to behave as you please:
 */
publishOnCentral {
    repoOwner.set("daksh7011") // Used to populate the default value for projectUrl and scmConnection
    projectDescription.set("A library for detecting links in text")
    // The following values are the default, if they are ok with you, just omit them
    projectLongName.set("Links Detektor")
    licenseName.set("MIT License")
    licenseUrl.set("https://github.com/daksh7011/links-detektor/LICENSE")
    projectUrl.set("https://github.com/daksh7011/links-detektor")
    scmConnection.set("scm:git:git://github.com/daksh7011/links-detektor.git")

    /*
     * The publications can be sent to other destinations, e.g. GitHub
     * The task name would be 'publishAllPublicationsToGitHubRepository'
     */
   // TODO: Setup this to send publications to github
}

/*
 * Developers and contributors must be added manually
 */
publishing {
    publications {
        withType<MavenPublication> {
            pom {
                developers {
                    developer {
                        name.set("Daksh Desai")
                        email.set("contact@slothiesmooth.com")
                        url.set("https://slothiesmooth.dev")
                    }
                }
            }
        }
    }
}


signing {
    val signingKey = System.getenv("SIGNING_KEY") ?: project.findProperty("signingKey") as String?
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: project.findProperty("signingPassword") as String?

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}
