package dev.slothiesmooth.linksdetektor.client

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import dev.slothiesmooth.linksdetektor.feature.common.api.Feature
import dev.slothiesmooth.linksdetektor.feature.common.api.FeatureContext
import dev.slothiesmooth.linksdetektor.feature.common.api.FeatureProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private interface FirstFeature : Feature

private interface SecondFeature : Feature

private class FirstProvider : FeatureProvider, FirstFeature {
    @Composable
    override fun provideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder) = Unit
}

private class SecondProvider : FeatureProvider, SecondFeature {
    @Composable
    override fun provideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder) = Unit
}

class FeatureOrderTest {
    private val first = FirstProvider()
    private val second = SecondProvider()

    @Test
    fun `providers are ordered by the feature order, not by set order`() {
        val order = persistentListOf(FirstFeature::class, SecondFeature::class)
        val sorted = sortFeatures(persistentSetOf(second, first), order)
        assertEquals(persistentListOf<FeatureProvider>(first, second), sorted)
    }

    @Test
    fun `a provider outside the order fails fast`() {
        assertFailsWith<IllegalStateException> {
            sortFeatures(persistentSetOf(first, second), persistentListOf(FirstFeature::class))
        }
    }

    @Test
    fun `a provider matching two entries fails fast`() {
        assertFailsWith<IllegalStateException> {
            sortFeatures(persistentSetOf(first), persistentListOf(FirstFeature::class, Feature::class))
        }
    }
}
