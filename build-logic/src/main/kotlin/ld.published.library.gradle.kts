@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    jvm {
        testRuns.named("test") {
            executionTask.configure { useJUnitPlatform() }
        }
    }

    android {
        namespace = "com.slothiesmooth.linksdetektor"
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
        withHostTestBuilder {}.configure {}
    }

    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()
    js(IR) {
        nodejs()
        browser { testTask { enabled = false } }
    }
    wasmJs {
        nodejs()
        browser { testTask { enabled = false } }
    }

    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                withCompilations { it.target is KotlinMultiplatformAndroidLibraryTarget }
            }
            group("nonJvm") { group("native"); group("web") }
            group("native") { group("nativeNonApple") { withLinux(); withMingw() } }
        }
    }

    explicitApi()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            api(libs.findLibrary("kotlinx-collections-immutable").get())
        }
        getByName("nativeNonAppleMain").dependencies {
            implementation(libs.findLibrary("doist-normalize").get())
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.findLibrary("junit-bom").get()))
            implementation(libs.findLibrary("kotlin-test-junit5").get())
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}

// Library bytecode stays at Java 17 (toolchain 21 only builds it).
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
