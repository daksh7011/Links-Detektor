package dev.slothiesmooth.linksdetektor.web

import dev.slothiesmooth.linksdetektor.client.DefaultFeatureOrder
import dev.slothiesmooth.linksdetektor.feature.detektor.api.DetektorRoute
import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertEquals

class WebAppGraphTest {
    @Test
    fun `the web graph builds on wasm`() {
        val graph = createGraph<WebAppGraph>()
        assertEquals(DefaultFeatureOrder, graph.featureOrder)
        assertEquals(DetektorRoute, graph.appStartRoute.route)
        assertEquals(1, graph.features.size)
    }
}
