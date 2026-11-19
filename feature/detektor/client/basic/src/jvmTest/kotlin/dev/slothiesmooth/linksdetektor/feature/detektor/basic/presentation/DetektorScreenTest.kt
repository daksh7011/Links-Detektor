package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import com.slothiesmooth.linksdetektor.LinksDetektorOptions.HTML
import com.slothiesmooth.linksdetektor.LinksDetektorOptions.QUOTE_MATCH
import com.slothiesmooth.linksdetektor.LinksDetektorOptions.SINGLE_QUOTE_MATCH
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.LdTheme
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldType
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.DetectLinksUseCase
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.DetectionResult
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.LibraryLinkEngine
import dev.slothiesmooth.linksdetektor.shared.presentation.state.UiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val SAMPLE = "docs at https://example.com/a and HTTP://Sub.Example.ORG/x"

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DetektorScreenTest {
    @Composable
    private fun host(
        state: DetektorUiState,
        width: Dp = 600.dp,
        onInputChange: (String) -> Unit = {},
        onToggleOption: (LinksDetektorOptions) -> Unit = {},
        onSelectLink: (Int?) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        LdTheme {
            Box(Modifier.size(width, 900.dp)) {
                DetektorContent(state, onInputChange, onToggleOption, onSelectLink, onRetry)
            }
        }
    }

    private fun ready(text: String): DetektorUiState {
        val useCase = DetectLinksUseCase(LibraryLinkEngine(), UnconfinedTestDispatcher())
        val result = assertIs<DetectionResult.Success>(runBlocking { useCase(text, persistentSetOf()) })
        return DetektorUiState(input = text, result = UiState.Ready(DetectionUi(result.text, result.links)))
    }

    @Test
    fun `empty input shows the prompt`() = runComposeUiTest {
        setContent { host(DetektorUiState()) }
        onNodeWithText("Nothing to check yet").assertIsDisplayed()
        onNodeWithTag("detektor-results").assertDoesNotExist()
    }

    @Test
    fun `typing reaches the callback`() = runComposeUiTest {
        var typed = ""
        setContent { host(DetektorUiState(), onInputChange = { typed = it }) }
        onNodeWithTag("detektor-input").performTextInput("a.com")
        assertEquals("a.com", typed)
    }

    @Test
    fun `results list every link and the count`() = runComposeUiTest {
        setContent { host(ready(SAMPLE), width = 1000.dp) }
        onNodeWithText("2 links found").assertIsDisplayed()
        onNodeWithTag("result-0").assertTextContains("https://example.com/a")
        onNodeWithTag("result-1").assertIsDisplayed()
        onNodeWithTag("detektor-highlight").assertTextContains(SAMPLE)
        onNodeWithText("Pick a link").assertIsDisplayed()
    }

    @Test
    fun `picking a row asks for that link and wide shows its detail beside the list`() = runComposeUiTest {
        var state by mutableStateOf(ready(SAMPLE))
        setContent { host(state, width = 1000.dp, onSelectLink = { state = state.copy(selectedId = it) }) }
        onNodeWithTag("result-1").performClick()
        onNodeWithTag("detektor-detail").assertIsDisplayed()
        onNodeWithTag("detektor-results").assertIsDisplayed()
        onNodeWithTag("detektor-detail").onChildren().assertAny(hasText("sub.example.org"))
        onNodeWithText("Tidied").assertIsDisplayed()
    }

    @Test
    fun `narrow shows the detail in a sheet that can be closed`() = runComposeUiTest {
        var state by mutableStateOf(ready(SAMPLE).copy(selectedId = 0))
        setContent { host(state, width = 400.dp, onSelectLink = { state = state.copy(selectedId = it) }) }
        onNodeWithTag("detektor-detail").assertIsDisplayed()
        onNodeWithText("Close").performClick()
        waitForIdle()
        onNodeWithTag("detektor-detail").assertDoesNotExist()
    }

    @Test
    fun `option chips reflect the set and report toggles`() = runComposeUiTest {
        var toggled: LinksDetektorOptions? = null
        setContent {
            host(
                DetektorUiState(options = persistentSetOf(QUOTE_MATCH, SINGLE_QUOTE_MATCH)),
                onToggleOption = { toggled = it },
            )
        }
        onNodeWithTag("option-QUOTE_MATCH").assertIsOn()
        onNodeWithTag("option-HTML").assertIsOff().performClick()
        assertEquals(HTML, toggled)
    }

    @Test
    fun `loading, no links, error and the cap notice each show their copy`() = runComposeUiTest {
        var state by mutableStateOf(DetektorUiState(input = "x", detecting = true))
        var retried = 0
        setContent { host(state, onRetry = { retried++ }) }
        onNodeWithText("Checking…").assertIsDisplayed()
        state = DetektorUiState(input = "hello", result = UiState.Ready(DetectionUi("hello", persistentListOf())))
        onNodeWithText("No links found").assertIsDisplayed()
        state = DetektorUiState(input = "x", truncated = true, result = UiState.Failed("That text could not be checked."))
        onNodeWithText("Only the first 20,000 characters are checked.").assertIsDisplayed()
        onNodeWithText("Try again").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `stale ranges never crash the highlight`() = runComposeUiTest {
        val stale = ready(SAMPLE).let { s ->
            s.copy(result = UiState.Ready(DetectionUi("tiny", (s.result as UiState.Ready).value.links)))
        }
        setContent { host(stale, width = 1000.dp) }
        onNodeWithTag("detektor-highlight").assertTextContains("tiny")
    }

    @Test
    fun `detail labels stay on one line on a narrow sheet`() = labelsStayOnOneLine(fontScale = 1f)

    @Test
    fun `detail labels stay on one line at 1_3 times font scale`() = labelsStayOnOneLine(fontScale = 1.3f)

    @Test
    fun `detail labels stay on one line at double font scale`() = labelsStayOnOneLine(fontScale = 2f)

    private fun labelsStayOnOneLine(fontScale: Float) = runComposeUiTest {
        val link = (ready("see http://user:pw@Sub.Example.org:8080/x?q=1#f ok").result as UiState.Ready).value.links.single()
        var oneLine = 0f
        setContent {
            val scaled = Density(LocalDensity.current.density, fontScale)
            CompositionLocalProvider(LocalDensity provides scaled) {
                LdTheme {
                    oneLine = with(scaled) { MaterialTheme.ldType.label.lineHeight.toPx() } * 1.5f
                    // 360 dp wide sheet: 16 dp gutters, 24 dp panel padding. The pane may grow past the test window.
                    Box(Modifier.width(328.dp).wrapContentHeight(unbounded = true)) { DetailPane(link, onClose = {}) }
                }
            }
        }
        persistentListOf("Full URL", "Scheme", "Username", "Password", "Host", "Port", "Path", "Query", "Fragment").forEach { label ->
            val height = onNodeWithText(label).fetchSemanticsNode().size.height
            assertTrue(height > 0, "$label has no height at $fontScale x")
            assertTrue(height <= oneLine, "$label is $height px high at $fontScale x, so it wraps")
        }
        onAllNodesWithText("Original").fetchSemanticsNodes().forEach { node ->
            assertTrue(node.size.height <= oneLine, "a caption wraps at $fontScale x")
        }
        onAllNodesWithText("Tidied").fetchSemanticsNodes().forEach { node ->
            assertTrue(node.size.height <= oneLine, "a caption wraps at $fontScale x")
        }
        onNodeWithTag("detail-stacked").assertExists()
        val minWidth = 100f * density.density
        persistentListOf("Sub.Example.org", "sub.example.org").forEach { value ->
            val width = onNodeWithText(value).fetchSemanticsNode().size.width
            assertTrue(width >= minWidth, "$value cell is only $width px wide at $fontScale x")
        }
    }

    @Test
    fun `a phone sheet stacks the detail rows at 1x font scale`() = runComposeUiTest {
        val link = (ready(SAMPLE).result as UiState.Ready).value.links.first()
        setContent {
            LdTheme {
                // 375 dp phone: 16 dp gutters on both sides.
                Box(Modifier.width(343.dp).wrapContentHeight(unbounded = true)) { DetailPane(link, onClose = {}) }
            }
        }
        onNodeWithTag("detail-stacked").assertExists()
    }

    @Test
    fun `a wide detail pane keeps the table layout`() = runComposeUiTest {
        val link = (ready(SAMPLE).result as UiState.Ready).value.links.first()
        setContent {
            LdTheme {
                Box(Modifier.width(900.dp).wrapContentHeight(unbounded = true)) { DetailPane(link, onClose = {}) }
            }
        }
        onNodeWithTag("detail-stacked").assertDoesNotExist()
    }
}
