import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("ld.kmp.library")
    id("ld.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "commonMainApi"(libs.findLibrary("jetbrains-navigation-compose").get())
    "commonMainApi"(libs.findLibrary("kotlinx-collections-immutable").get())
    "commonMainApi"(libs.findLibrary("kotlinx-serialization-core").get())
}
