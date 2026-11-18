package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.DetectedLink
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.LinkParts
import dev.slothiesmooth.linksdetektor.shared.presentation.state.UiState
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class DetektorScreenStateTest {
    private val parts = LinkParts("a.com", "http", "", "", "a.com", 80, "", "", "")
    private val link = DetectedLink(0, 0, 5, parts, parts)

    @Test
    fun `screen state table`() {
        assertEquals(DetektorScreenState.Empty, screenStateOf(DetektorUiState()))
        assertEquals(DetektorScreenState.Loading, screenStateOf(DetektorUiState(input = "x", detecting = true)))
        assertEquals(DetektorScreenState.Loading, screenStateOf(DetektorUiState(result = UiState.Loading)))
        assertEquals(
            DetektorScreenState.NoLinks,
            screenStateOf(DetektorUiState(result = UiState.Ready(DetectionUi("hello", persistentListOf())))),
        )
        assertEquals(
            DetektorScreenState.Content,
            screenStateOf(DetektorUiState(result = UiState.Ready(DetectionUi("a.com", persistentListOf(link))))),
        )
        assertEquals(DetektorScreenState.Error, screenStateOf(DetektorUiState(result = UiState.Failed("m"))))
    }
}
