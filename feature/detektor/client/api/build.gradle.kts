plugins {
    id("ld.feature.api")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.common.client.api)
        }
    }
}
