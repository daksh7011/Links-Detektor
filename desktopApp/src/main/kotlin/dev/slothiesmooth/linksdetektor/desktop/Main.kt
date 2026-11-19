package dev.slothiesmooth.linksdetektor.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.slothiesmooth.linksdetektor.client.APP_TITLE
import dev.slothiesmooth.linksdetektor.client.App
import dev.zacsweers.metro.createGraph

fun main() {
    val graph = createGraph<DesktopAppGraph>()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = APP_TITLE,
            state = rememberWindowState(size = DpSize(1280.dp, 800.dp)),
        ) {
            App(graph)
        }
    }
}
