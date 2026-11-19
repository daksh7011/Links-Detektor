package dev.slothiesmooth.linksdetektor.feature.common.metro

import dev.slothiesmooth.linksdetektor.feature.common.api.BaseFeatureProvider

/**
 * Base class of every `BasicXProvider`. A provider is `@Inject`ed and contributed with
 * `@ContributesIntoSet(AppScope::class, binding = binding<FeatureProvider>())`; it owns no container of its own.
 */
abstract class MetroFeatureProvider : BaseFeatureProvider()
