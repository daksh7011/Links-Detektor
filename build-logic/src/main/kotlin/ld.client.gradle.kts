import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("ld.kmp.library")
    id("ld.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ld.metro")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "commonMainImplementation"(libs.findLibrary("metro-runtime").get())
    "commonMainApi"(libs.findLibrary("metrox-viewmodel").get())
    "commonMainImplementation"(libs.findLibrary("metrox-viewmodel-compose").get())
    "commonMainImplementation"(libs.findLibrary("jetbrains-navigation-compose").get())
    "commonMainImplementation"(libs.findLibrary("kotlinx-collections-immutable").get())
    "commonMainImplementation"(libs.findLibrary("kotlinx-coroutines-core").get())
}
