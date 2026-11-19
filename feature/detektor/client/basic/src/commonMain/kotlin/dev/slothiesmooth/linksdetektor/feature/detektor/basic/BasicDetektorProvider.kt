package dev.slothiesmooth.linksdetektor.feature.detektor.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.slothiesmooth.linksdetektor.feature.common.api.FeatureContext
import dev.slothiesmooth.linksdetektor.feature.common.api.FeatureProvider
import dev.slothiesmooth.linksdetektor.feature.common.metro.MetroFeatureProvider
import dev.slothiesmooth.linksdetektor.feature.common.metro.metroFeatureViewModel
import dev.slothiesmooth.linksdetektor.feature.detektor.api.DetektorFeature
import dev.slothiesmooth.linksdetektor.feature.detektor.api.DetektorRoute
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation.DetektorScreen
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation.DetektorViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

/** The link detector: contributes its one route to the shell. */
@Inject
@ContributesIntoSet(AppScope::class, binding = binding<FeatureProvider>())
class BasicDetektorProvider : MetroFeatureProvider(), DetektorFeature {
    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<DetektorRoute> { DetektorScreen(viewModel = metroFeatureViewModel<DetektorViewModel>()) }
    }
}
