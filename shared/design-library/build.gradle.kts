plugins {
    id("ld.kmp.library")
    id("ld.compose")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        commonMain.dependencies {
            api(projects.shared.presentation)
            implementation(libs.kotlinx.collections.immutable)
        }
    }
}
