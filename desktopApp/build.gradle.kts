plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("ld.metro")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.client)
    implementation(compose.desktop.currentOs)
    implementation(libs.metro.runtime)
    // JVM Main dispatcher (Swing EDT): view models use Dispatchers.Main, which throws without it.
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(libs.kotlin.test)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "dev.slothiesmooth.linksdetektor.desktop.MainKt"
        // Run with the JDK 21 the module compiles with, not the JDK running Gradle.
        javaHome = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(21)
        }.get().metadata.installationPath.asFile.absolutePath
    }
}
