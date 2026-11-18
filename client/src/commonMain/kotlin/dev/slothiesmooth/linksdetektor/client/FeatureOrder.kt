package dev.slothiesmooth.linksdetektor.client

import dev.slothiesmooth.linksdetektor.feature.common.api.Feature
import dev.slothiesmooth.linksdetektor.feature.common.api.FeatureProvider
import dev.slothiesmooth.linksdetektor.feature.detektor.api.DetektorFeature
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.reflect.KClass

/** Nesting order of the feature wrappers, outermost first. */
val DefaultFeatureOrder: ImmutableList<KClass<out Feature>> = persistentListOf(
    DetektorFeature::class,
)

/**
 * Orders the contributed [features] by [order], since a graph set has no order of its own.
 * A provider that matches no entry of [order], or more than one, fails fast: a silent position would nest wrappers wrongly.
 */
fun sortFeatures(
    features: ImmutableSet<FeatureProvider>,
    order: ImmutableList<KClass<out Feature>>,
): ImmutableList<FeatureProvider> {
    val ranked = features.map { provider ->
        val matches = order.indices.filter { index -> order[index].isInstance(provider) }.toImmutableList()
        val name = provider::class.simpleName
        check(matches.isNotEmpty()) { "Feature $name is not in the feature order" }
        check(matches.size == 1) { "Feature $name matches several entries of the feature order" }
        matches.single() to provider
    }.toImmutableList()
    return ranked
        .sortedBy { (rank, _) -> rank }
        .map { (_, provider) -> provider }
        .toImmutableList()
}
