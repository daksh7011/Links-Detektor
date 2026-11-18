plugins {
    id("ld.kmp.library")
    id("ld.compose")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }
        commonMain.dependencies {
            // api: BaseViewModel publicly extends androidx.lifecycle.ViewModel.
            api(libs.jetbrains.lifecycle.viewmodel.compose)
            api(libs.jetbrains.navigation.compose)
            api(libs.kotlinx.collections.immutable)
            // LocalLifecycleOwner (androidx.lifecycle.compose) used inside BaseViewModel.bind().
            implementation(libs.jetbrains.lifecycle.runtime.compose)
        }
    }
}
