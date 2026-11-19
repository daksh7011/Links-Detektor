import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// The compose.* dependency accessors are deprecated in Compose Multiplatform 1.11
// ("Specify dependency directly"), so we resolve the artifacts from the version catalog instead.
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "commonMainImplementation"(libs.findLibrary("compose-runtime").get())
    "commonMainImplementation"(libs.findLibrary("compose-foundation").get())
    "commonMainImplementation"(libs.findLibrary("compose-material3").get())
    "commonMainImplementation"(libs.findLibrary("compose-ui").get())
    "commonMainImplementation"(libs.findLibrary("compose-components-resources").get())
    "commonMainImplementation"(libs.findLibrary("compose-ui-tooling-preview").get())
    // Desktop Compose UI tests (jvmTest): runComposeUiTest + the skiko runtime for the host OS.
    "jvmTestImplementation"(libs.findLibrary("compose-ui-test").get())
    "jvmTestImplementation"(compose.desktop.currentOs)
    // ComposeViewAdapter, which Android Studio's preview renderer loads; android-only.
    "androidMainImplementation"(libs.findLibrary("compose-ui-tooling").get())
    // On-device Compose UI tests (androidDeviceTest): runComposeUiTest + the test-manifest Activity host.
    "androidDeviceTestImplementation"(libs.findLibrary("compose-ui-test").get())
    "androidDeviceTestImplementation"(libs.findLibrary("compose-ui-test-manifest").get())
    // Force espresso 3.7.0 (getSystemService, not the removed InputManager.getInstance) so Compose
    // device tests run on API 35+ devices/emulators; compose-ui-test pulls an older espresso otherwise.
    "androidDeviceTestImplementation"(libs.findLibrary("androidx-test-espresso-core").get())
}

// Generated Res class lives in a package derived from the module path, e.g. :shared:design-library
// -> dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.
fun defaultResourcePackage(projectPath: String): String {
    val segments = projectPath
        .removePrefix(":")
        .split(":")
        .filter { it != "client" }
        .map { it.replace("-", "") }
    val suffix = if (segments.isEmpty()) "client" else segments.joinToString(".")
    return "dev.slothiesmooth.linksdetektor.$suffix.resources"
}

compose.resources {
    publicResClass = true
    generateResClass = always
    packageOfResClass = defaultResourcePackage(project.path)
}

// Test files opt in to androidx.compose.ui.test.ExperimentalTestApi with @OptIn (runComposeUiTest).

// Material 3 components the shell uses (bottom sheet, dialogs) are still marked experimental.
extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    sourceSets.all { languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api") }
}

// Compose UI tests here run on the desktop jvm target only. The wasmJs check below only guards Compose UI tests on wasm,
// which these modules do not have, and it fails for every library module because only :webApp declares an executable.
tasks.matching { it.name == "checkComposeUiTestConfigurationForWasmJs" }.configureEach { enabled = false }
