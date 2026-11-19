package dev.slothiesmooth.linksdetektor.feature.common.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder

/** A feature's entry in the app: contributed into the app graph as a set and ordered by the client. */
interface FeatureProvider : Feature {
    /** Wraps the recursive host content; the outermost providers decorate the whole shell. */
    @Composable
    fun provideContent(context: FeatureContext, content: @Composable () -> Unit)

    /** Contributes this feature's type-safe routes to the single shell NavHost. */
    fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder)
}
