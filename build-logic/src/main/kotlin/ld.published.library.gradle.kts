import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    jvm {
        testRuns.named("test") {
            executionTask.configure { useJUnitPlatform() }
        }
    }

    applyDefaultHierarchyTemplate()

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
