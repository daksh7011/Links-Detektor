package dev.slothiesmooth.linksdetektor.feature.common.api

/** Where the app lands once the splash is done. Bound once in `:client`, so features never hard-code each other's routes. */
interface AppStartRoute {
    val route: Any
}
