// Applies the Metro compiler plugin once the module has a Kotlin plugin (Metro needs the kotlin extension at apply time), so the
// choice of plugin lives in one place. Modules take it with id("ld.metro") wherever they use Metro.
listOf("org.jetbrains.kotlin.multiplatform", "org.jetbrains.kotlin.jvm").forEach { kotlinPlugin ->
    pluginManager.withPlugin(kotlinPlugin) { pluginManager.apply("dev.zacsweers.metro") }
}
