@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("ld.metro")
}

kotlin {
    jvmToolchain(21)

    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.client)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.metro.runtime)
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.coroutines.core)
        }
        wasmJsTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
