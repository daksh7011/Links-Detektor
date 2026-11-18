package dev.slothiesmooth.linksdetektor.feature.common.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow

/**
 * The navigation surface handed to every feature. Destinations are matched by serialized route name (a String),
 * which behaves the same on every target; `route::class.serializer()` is internal API.
 */
@Suppress("TooManyFunctions") // the whole navigation surface of the shell, one verb each
@Stable
interface FeatureContext {
    /** Emits the current destination's serialized route name on every back-stack change. */
    fun getCurrentDestinationChanges(): Flow<String?>

    /** Emits the id of the shown back-stack entry on every change, even when the route name stays the same (the same screen pushed twice). */
    fun getCurrentEntryChanges(): Flow<String>

    /**
     * True when the shown screen is a root: the first screen, or one chosen with [setDestination] (a bar entry). False for a
     * screen that was pushed over another with [pushDestination] (a home tile, a row): those get a back arrow.
     */
    fun isRootDestination(): Boolean

    /**
     * Like [isRootDestination], but for the page this composition belongs to, not the shown one: a page fading out during a
     * transition keeps its own answer. Call it from inside a destination's content.
     */
    @Composable
    fun isRootPage(): Boolean

    /** The current destination's serialized route name, or null before the graph is ready. */
    fun getCurrentDestination(): String?

    /** Suspends until the navigation graph is set and the first destination is shown; navigate only after it returns. */
    suspend fun awaitReady()

    /** Navigates to a new instance of [route]. */
    fun pushDestination(route: Any)

    /** Switches to [route] as a root destination: clears back to the root and navigates (tab switch). */
    fun setDestination(route: Any)

    /** Navigates to [route] and removes the current destination from the back stack. */
    fun replaceDestination(route: Any)

    /** Navigates to [route] and drops the whole back stack under it (a reset to a fresh start). */
    fun resetStack(route: Any)

    /** Pops the back stack if there is a previous entry. */
    fun popDestination()
}
