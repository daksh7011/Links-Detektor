package dev.slothiesmooth.linksdetektor.client

import dev.slothiesmooth.linksdetektor.feature.common.api.AppStartRoute
import dev.slothiesmooth.linksdetektor.feature.common.api.Feature
import dev.slothiesmooth.linksdetektor.feature.common.api.FeatureProvider
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import kotlinx.collections.immutable.ImmutableList
import kotlin.reflect.KClass

/**
 * The part of the app graph shared by every platform. It is not annotated: a graph in commonMain cannot see
 * platform contributions, so each entry app declares its own `@DependencyGraph(AppScope::class)` extending this.
 */
interface BaseAppGraph : ViewModelGraph {
    /** Every contributed provider, unordered; [sortFeatures] orders them by [featureOrder]. Metro's multibinding type: convert at once. */
    @Multibinds(allowEmpty = true)
    val features: Set<FeatureProvider>

    val featureOrder: ImmutableList<KClass<out Feature>>

    /** Where the app lands first. */
    val appStartRoute: AppStartRoute

    @Provides
    fun provideFeatureOrder(): ImmutableList<KClass<out Feature>> = DefaultFeatureOrder
}
