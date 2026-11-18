package dev.slothiesmooth.linksdetektor.feature.common.api

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val nextFeatureIndex = staticCompositionLocalOf { 0 }

/**
 * Nests each provider's content wrapper recursively, in list order (the first is the outermost). Once every provider
 * has wrapped, one shell [Scaffold] hosts the single NavHost built from all providers' navigation contributions.
 */
@Composable
fun FeatureHost(context: FeatureHostContext, startDestinationProvider: () -> Any?) {
    val index = nextFeatureIndex.current
    val feature = remember(index, context.features) { context.features.getOrNull(index) }
    if (feature != null) {
        CompositionLocalProvider(nextFeatureIndex provides index + 1) {
            feature.provideContent(context) {
                FeatureHost(context, startDestinationProvider)
            }
        }
    } else {
        FeatureHostContent(context, startDestinationProvider)
    }
}

@Composable
private fun FeatureHostContent(context: FeatureHostContext, startDestinationProvider: () -> Any?) {
    val startDestination = startDestinationProvider() ?: return
    // The shell and the screens place themselves inside the system bars; the host adds no insets of its own.
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0, 0, 0, 0)) { paddings ->
        NavHost(
            modifier = Modifier.fillMaxSize().padding(paddings).consumeWindowInsets(paddings),
            startDestination = startDestination,
            contentAlignment = Alignment.Center,
            navController = context.navController,
            enterTransition = { if (context.isPush(this)) slideIn(forward = true) else fadeThroughIn() },
            exitTransition = { if (context.isPush(this)) slideOut(forward = true) else fadeThroughOut() },
            popEnterTransition = { slideIn(forward = false) },
            popExitTransition = { slideOut(forward = false) },
        ) {
            context.features.forEach { registered -> registered.provideNavigation(context, this) }
        }
    }
}

// Material fade-through and shared-axis timings: the old screen is gone before the new one fades in, so the two never
// show through each other (the default 700 ms crossfade did).
private const val OUT_MILLIS = 90
private const val IN_MILLIS = 210
private const val SLIDE_FRACTION = 10

private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(durationMillis = IN_MILLIS, delayMillis = OUT_MILLIS, easing = LinearOutSlowInEasing))

private fun fadeThroughOut(): ExitTransition = fadeOut(tween(OUT_MILLIS))

private fun slideIn(forward: Boolean): EnterTransition = fadeThroughIn() +
    slideInHorizontally(tween(IN_MILLIS + OUT_MILLIS, easing = LinearOutSlowInEasing)) { width ->
        if (forward) width / SLIDE_FRACTION else -width / SLIDE_FRACTION
    }

private fun slideOut(forward: Boolean): ExitTransition = fadeThroughOut() +
    slideOutHorizontally(tween(IN_MILLIS + OUT_MILLIS, easing = LinearOutSlowInEasing)) { width ->
        if (forward) -width / SLIDE_FRACTION else width / SLIDE_FRACTION
    }

/** The concrete [FeatureContext], backed by the shell's [NavHostController]. */
@Suppress("TooManyFunctions")
class FeatureHostContext(
    internal val features: ImmutableList<FeatureProvider>,
    internal val navController: NavHostController,
) : FeatureContext {
    // Entries chosen as roots with setDestination. A screen pushed over another is never in here, even when it has the same route.
    private var rootEntries: PersistentSet<String> = persistentSetOf()

    override fun getCurrentDestinationChanges(): Flow<String?> = navController.currentBackStackEntryFlow
        .map { entry -> entry.destination.route }
        .distinctUntilChanged()

    override fun getCurrentEntryChanges(): Flow<String> = navController.currentBackStackEntryFlow.map { entry -> entry.id }

    override fun isRootDestination(): Boolean {
        val entry = navController.currentBackStackEntry ?: return true
        val shown = navController.currentBackStack.value.count { it.destination.route != null }
        return shown <= 1 || entry.id in rootEntries
    }

    @Composable
    override fun isRootPage(): Boolean {
        val entry = LocalLifecycleOwner.current as? NavBackStackEntry ?: return true
        val shown = navController.currentBackStack.value.count { it.destination.route != null }
        return shown <= 1 || entry.id in rootEntries
    }

    /** A push lands on a page over the one it starts from; a root switch, a replace of a root or a reset lands on a root. */
    internal fun isPush(scope: AnimatedContentTransitionScope<NavBackStackEntry>): Boolean =
        scope.targetState.id !in rootEntries &&
            navController.currentBackStack.value.any { entry -> entry.id == scope.initialState.id }

    override fun getCurrentDestination(): String? = navController.currentBackStackEntry?.destination?.route

    override suspend fun awaitReady() {
        navController.currentBackStackEntryFlow.first()
    }

    override fun pushDestination(route: Any) = navController.navigate(route)

    // Clears back to the current root, not the graph's declared start: the splash is declared first yet popped.
    override fun setDestination(route: Any) {
        val rootRoute = navController.currentBackStack.value
            .firstOrNull { entry -> entry.destination.route != null }
            ?.destination?.route
        navController.navigate(
            route,
            navOptions {
                rootRoute?.let { root -> popUpTo(root) { inclusive = false } }
                launchSingleTop = true
            },
        )
        navController.currentBackStackEntry?.id?.let { id -> rootEntries = rootEntries.adding(id) }
    }

    // The current destination's route is a String, so use the String overload of popUpTo.
    override fun replaceDestination(route: Any) {
        val wasRoot = isRootDestination()
        navController.navigate(route) {
            navController.currentDestination?.route?.let { current -> popUpTo(current) { inclusive = true } }
            launchSingleTop = true
        }
        if (wasRoot) navController.currentBackStackEntry?.id?.let { id -> rootEntries = rootEntries.adding(id) }
    }

    override fun resetStack(route: Any) {
        rootEntries = persistentSetOf()
        navController.navigate(route) {
            // The whole stack goes, whatever its entries are: the start destination may be long gone.
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
        navController.currentBackStackEntry?.id?.let { id -> rootEntries = rootEntries.adding(id) }
    }

    override fun popDestination() {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }
}
