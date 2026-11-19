plugins {
    id("ld.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.common.client.api)
            api(projects.shared.presentation)
        }
    }
}
