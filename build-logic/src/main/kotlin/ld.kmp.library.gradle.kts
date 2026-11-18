@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

val ldAndroidCompileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
val ldAndroidMinSdk = libs.findVersion("android-app-minSdk").get().requiredVersion.toInt()
val ldJvmToolchain = 21

// A metadata (commonMain) klib's `unique_name` is the Kotlin module name, which KGP derives as
// `archivesName.orElse(project.name)` + "_<sourceSet>" — WITHOUT the "$group:" prefix that native,
// JS and JVM klibs get. Every ":feature:*:client:api" module is named "api" and every
// ":feature:*:client:basic" is named "basic", so they all emitted `unique_name=api_commonMain` /
// `basic_commonMain` and the KLIB loader warned about the collision. Project paths are unique, so
// deriving the archive base name from the path makes every metadata klib unique.
// Only local archive FILE names change: nothing here is published to Maven, and consumers use
// `projects.*` dependencies, which Gradle resolves by variant metadata rather than by file name.
base {
    archivesName.set(project.path.removePrefix(":").replace(":", "-"))
}

fun defaultAndroidNamespace(projectPath: String): String {
    val segments = projectPath
        .removePrefix(":")
        .split(":")
        .filter { it != "client" }
        .map { it.replace("-", "") }
    // ":client" filters down to no segments; it keeps the "client" leaf.
    val suffix = if (segments.isEmpty()) "client" else segments.joinToString(".")
    return "dev.slothiesmooth.linksdetektor.$suffix"
}

kotlin {
    android {
        namespace = defaultAndroidNamespace(project.path)
        compileSdk = ldAndroidCompileSdk
        minSdk = ldAndroidMinSdk
        androidResources.enable = true
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm {
        // JUnit Platform for the jvm tests (kotlin-test-junit5 below).
        testRuns.named("test") {
            executionTask.configure { useJUnitPlatform() }
        }
    }

    // iosX64 (Intel-Mac iOS simulator) is intentionally omitted: Compose Multiplatform no longer publishes an iosX64
    // variant. Apple Silicon simulators use iosSimulatorArm64; real devices use iosArm64.
    iosArm64()
    iosSimulatorArm64()

    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    jvmToolchain(ldJvmToolchain)

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
            implementation(libs.findLibrary("turbine").get())
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.findLibrary("junit-bom").get()))
            implementation(libs.findLibrary("kotlin-test-junit5").get())
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.findLibrary("androidx-test-runner").get())
            implementation(libs.findLibrary("androidx-test-ext-junit").get())
            implementation(libs.findLibrary("junit4").get())
        }
    }
}
