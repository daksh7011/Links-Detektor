plugins {
    `kotlin-dsl`
}

// Plugin versions come from the root version catalog so build-logic never drifts from the versions
// the rest of the project resolves.
dependencies {
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.compose:compose-gradle-plugin:${libs.versions.compose.multiplatform.get()}")
    implementation("dev.zacsweers.metro:gradle-plugin:${libs.versions.metro.get()}")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${libs.versions.detekt.get()}")
    implementation("com.vanniktech:gradle-maven-publish-plugin:${libs.versions.vanniktech.get()}")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:${libs.versions.dokka.get()}")
}
