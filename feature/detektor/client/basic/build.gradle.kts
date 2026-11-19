plugins {
    id("ld.feature.basic")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.detektor.client.api)
            api(projects.feature.common.client.api)
            api(projects.feature.common.client.metro)
            api(projects.shared.presentation)
            api(projects.shared.designLibrary)
            implementation(projects.linksDetektor)
        }
    }
}
