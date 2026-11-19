package dev.slothiesmooth.linksdetektor.client

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.slothiesmooth.linksdetektor.feature.detektor.api.DetektorFeature
import dev.zacsweers.metro.createGraph
import kotlinx.collections.immutable.toImmutableSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalTestApi::class)
class AppSmokeTest {
    @Test
    fun `the graph builds and every contributed feature has a place in the order`() {
        val graph = createGraph<TestAppGraph>()
        val sorted = sortFeatures(graph.features.toImmutableSet(), graph.featureOrder)
        assertEquals(graph.features.size, sorted.size)
    }

    @Test
    fun `the detektor feature is contributed and ordered`() {
        val graph = createGraph<TestAppGraph>()
        val sorted = sortFeatures(graph.features.toImmutableSet(), graph.featureOrder)
        assertEquals(1, sorted.size)
        assertIs<DetektorFeature>(sorted.single())
    }

    @Test
    fun `typing text with a link shows it in the results`() = runComposeUiTest {
        val graph = createGraph<TestAppGraph>()
        setContent { App(graph) }
        onNodeWithTag("detektor-input").performTextInput("read https://example.com/docs today")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("result-0").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("1 link found").assertExists()
        onNodeWithTag("result-0").assertTextContains("https://example.com/docs")
    }
}
