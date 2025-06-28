pluginManagement {

    repositories {
        gradlePluginPortal()
        mavenCentral()

        maven("https://snapshots-repo.kordex.dev")
        maven("https://releases-repo.kordex.dev")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

rootProject.name = "links-detektor"
include("lib")
include("sample")
