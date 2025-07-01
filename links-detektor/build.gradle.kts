plugins {
    kotlin("jvm")

    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")

    // Apply Dokka for Kotlin documentation
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    signing
}

val projectVersion: String = System.getenv("VERSION") ?: libs.versions.lib.get()

group = "com.slothiesmooth"
version = projectVersion

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
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

// Task to copy Dokka documentation to docs directory in the root project
tasks.register<Copy>("copyDokkaToDocsDir") {
    dependsOn("dokkaHtml")
    from(layout.buildDirectory.dir("dokka/html"))
    into(rootProject.file("docs"))
    description = "Copies Dokka documentation to docs directory in the root project"
}

// Make build task finalized by copyDokkaToDocsDir task
tasks.named("build") {
    finalizedBy("copyDokkaToDocsDir")
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
            remoteUrl.set(uri("https://github.com/daksh7011/links-detektor/blob/master/lib/src/main/kotlin").toURL())
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

signing {
    val signingKey = System.getenv("SIGNING_KEY") ?: project.findProperty("signingKey") as String?
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: project.findProperty("signingPassword") as String?

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}
