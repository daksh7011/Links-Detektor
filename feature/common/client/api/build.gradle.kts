plugins {
    id("ld.feature.api")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.presentation)
        }
    }
}
