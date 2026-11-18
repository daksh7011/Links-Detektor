import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    base
    id("io.gitlab.arturbosch.detekt")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "detektPlugins"(libs.findLibrary("detekt-formatting").get())
}

val detektCheck by tasks.registering(Detekt::class) {
    description = "Runs detekt across all module Kotlin sources."
    group = "verification"
    parallel = true
    autoCorrect = providers.gradleProperty("detektAutoCorrect").isPresent
    ignoreFailures = false
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    setSource(files(rootDir))
    include("**/*.kt")
    exclude(
        "**/build/**",
        "**/generated/**",
        "**/resources/**",
        "build-logic/**",
        ".claude/**",
        ".superpowers/**",
        "**/.gradle/**",
        "**/.kotlin/**",
    )
    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
        txt.required.set(false)
        md.required.set(false)
    }
}

// The house rule (immutable collections only) is enforced here: detekt's ForbiddenMethodCall needs type resolution,
// which `detektCheck` does not have, so this regex guard is the enforcing gate.
val bannedCollections by tasks.registering {
    description = "Fails when production sources use mutable or java.util collections."
    group = "verification"
    val sources = fileTree(rootDir) {
        include("**/src/*Main/**/*.kt", "**/src/main/**/*.kt")
        exclude("**/build/**", "build-logic/**")
    }
    inputs.files(sources)
    doLast {
        val banned = Regex(
            """\b(mutableListOf|arrayListOf|mutableSetOf|hashSetOf|linkedSetOf|mutableMapOf|hashMapOf|linkedMapOf|ArrayList|HashSet|HashMap|LinkedHashSet|LinkedHashMap)\s*[<(]|^import java\.util\.""",
            RegexOption.MULTILINE
        )
        val hits = sources.files.filter { banned.containsMatchIn(it.readText()) }
        check(hits.isEmpty()) { "Mutable collections are banned (house rule): $hits" }
    }
}

detektCheck { dependsOn(bannedCollections) }
