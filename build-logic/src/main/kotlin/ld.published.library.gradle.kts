@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class, ExperimentalAbiValidation::class)

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
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
    js {
        // Mocha's default limit is 2 s per test, which grows tight on a slow runner (the fuzz and scaling tests).
        nodejs { testTask { useMocha { timeout = "30s" } } }
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
        }
    }

    explicitApi()

    abiValidation {}

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            api(libs.findLibrary("kotlinx-collections-immutable").get())
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

dokka {
    moduleName.set("Links Detektor")
    dokkaSourceSets.configureEach {
        includes.from("packages.md")
        sourceLink {
            localDirectory.set(file("src/commonMain/kotlin"))
            remoteUrl("https://github.com/daksh7011/links-detektor/blob/master/links-detektor/src/commonMain/kotlin")
            remoteLineSuffix.set("#L")
        }
        perPackageOption {
            matchingRegex.set("""com\.slothiesmooth\.linksdetektor\.internal.*""")
            suppress.set(true)
        }
    }
    pluginsConfiguration.html {
        footerMessage.set("© 2026 Slothie Smooth - Links Detektor")
    }
}
