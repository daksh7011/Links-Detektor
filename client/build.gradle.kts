plugins {
    id("ld.client")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        commonMain.dependencies {
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            api(projects.shared.presentation)
            api(projects.shared.designLibrary)
            api(projects.feature.common.client.api)
            api(projects.feature.common.client.metro)
            api(projects.feature.detektor.client.api)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}
