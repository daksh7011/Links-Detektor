package dev.slothiesmooth.linksdetektor.client

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import dev.zacsweers.metro.createGraph
import kotlinx.collections.immutable.toImmutableSet
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppSmokeTest {
    @Test
    fun `the graph builds and every contributed feature has a place in the order`() {
        val graph = createGraph<TestAppGraph>()
        val sorted = sortFeatures(graph.features.toImmutableSet(), graph.featureOrder)
        assertEquals(graph.features.size, sorted.size)
    }

    @Test
    fun `the app root composes with no feature installed`() = runComposeUiTest {
        val graph = createGraph<TestAppGraph>()
        setContent { App(graph) }
        onNodeWithContentDescription("Links Detektor").assertExists()
    }
}
