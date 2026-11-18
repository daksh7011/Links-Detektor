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
// which `detektCheck` does not have, so this regex guard is the enforcing gate. It covers main and test sources.
// It cannot see inferred plain results (for example of `.map {}` or `.filter {}`); reviewers enforce those.
val bannedCollections by tasks.registering {
    description = "Fails when sources use mutable, plain or java.util collections."
    group = "verification"
    // Captured here so the task action does not reach back into the script (configuration cache).
    val root = rootDir
    val sources = fileTree(root) {
        include(
            "**/src/*Main/**/*.kt",
            "**/src/main/**/*.kt",
            "**/src/*Test/**/*.kt",
            "**/src/test/**/*.kt",
        )
        exclude("**/build/**", "build-logic/**", ".claude/**", ".superpowers/**")
    }
    inputs.files(sources)
    doLast {
        val banned = Regex(
            listOf(
                """\b(mutableListOf|arrayListOf|mutableSetOf|hashSetOf|linkedSetOf|mutableMapOf|hashMapOf|linkedMapOf|ArrayList|HashSet|HashMap|LinkedHashSet|LinkedHashMap)\s*[<(]""",
                """^import java\.util\.(?!Locale\b)""",
                """\b(List|Collection|MutableList|MutableSet|MutableMap|MutableCollection)\s*<""",
                """\b(listOf|setOf|mapOf|emptyList|emptySet|emptyMap|listOfNotNull|buildList|buildSet|buildMap|toMutableList|toMutableSet|toMutableMap)\s*[(<{]""",
                """\.(toList|toSet|toMap)\s*\(""",
            ).joinToString("|"),
            RegexOption.MULTILINE
        )
        // Metro's multibinding and view-model factory APIs are typed with plain Set and Map. These two files are the only
        // place they appear, each converts at once, and only the plain Set/Map pattern is waived there.
        val plainSetOrMap = Regex("""\b(Set|Map)\s*<""")
        val metroTypedFiles = setOf(
            "client/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/client/BaseAppGraph.kt",
            "client/src/commonMain/kotlin/dev/slothiesmooth/linksdetektor/client/LdViewModelFactory.kt",
        )
        val hits = sources.files.flatMap { file ->
            val metroTyped = file.relativeTo(root).invariantSeparatorsPath in metroTypedFiles
            file.readLines().withIndex()
                .filter { (_, line) ->
                    banned.containsMatchIn(line) || (!metroTyped && plainSetOrMap.containsMatchIn(line))
                }
                .map { (index, line) -> "${file.relativeTo(root)}:${index + 1}: ${line.trim()}" }
        }
        check(hits.isEmpty()) { "Mutable or plain collections are banned (house rule):\n" + hits.joinToString("\n") }
    }
}

detektCheck { dependsOn(bannedCollections) }
