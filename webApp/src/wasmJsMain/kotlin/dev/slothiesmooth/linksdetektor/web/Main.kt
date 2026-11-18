package dev.slothiesmooth.linksdetektor.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.slothiesmooth.linksdetektor.client.App
import dev.zacsweers.metro.createGraph
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val graph = createGraph<WebAppGraph>()
    ComposeViewport {
        App(graph)
    }
    document.getElementById("boot")?.remove()
}
