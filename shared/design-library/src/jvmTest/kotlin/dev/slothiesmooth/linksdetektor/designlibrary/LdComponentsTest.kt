package dev.slothiesmooth.linksdetektor.designlibrary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdChip
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdText
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdTextField
import dev.slothiesmooth.linksdetektor.designlibrary.molecules.LdErrorState
import dev.slothiesmooth.linksdetektor.designlibrary.molecules.LdMasterDetail
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.LdTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LdComponentsTest {
    @Test
    fun `a chip reports its state and toggles on click`() = runComposeUiTest {
        var on by mutableStateOf(false)
        setContent { LdTheme { LdChip("HTML", selected = on, onToggle = { on = !on }) } }
        onNodeWithText("HTML").assertIsOff().performClick()
        onNodeWithText("HTML").assertIsOn()
    }

    @Test
    fun `a chip claims a 48 dp touch target in layout`() = runComposeUiTest {
        setContent {
            LdTheme { Box(Modifier.testTag("slot")) { LdChip("HTML", selected = false, onToggle = {}) } }
        }
        val slot = onNodeWithTag("slot").fetchSemanticsNode().boundsInRoot.height
        assertTrue(slot >= 48f * density.density, "the chip's slot is $slot px high")
        val touch = onNodeWithText("HTML").fetchSemanticsNode().touchBoundsInRoot.height
        assertTrue(touch >= 48f * density.density, "touch target is $touch px high")
    }

    @Test
    fun `a text field announces its label once`() = runComposeUiTest {
        setContent { LdTheme { LdTextField("", {}, label = "Link text") } }
        onAllNodes(hasText("Link text") or hasContentDescription("Link text")).assertCountEquals(1)
    }

    @Test
    fun `the error state offers a retry`() = runComposeUiTest {
        var retried = 0
        setContent { LdTheme { LdErrorState("Could not check that text", "Try again.", onRetry = { retried++ }) } }
        onNodeWithText("Try again").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `wide shows master and detail side by side`() = runComposeUiTest {
        setContent { LdTheme { Box(Modifier.size(1000.dp, 600.dp)) { sample(chosen = true) } } }
        onNodeWithText("MASTER").assertIsDisplayed()
        onNodeWithText("DETAIL").assertIsDisplayed()
    }

    @Test
    fun `wide with nothing chosen shows the placeholder`() = runComposeUiTest {
        setContent { LdTheme { Box(Modifier.size(1000.dp, 600.dp)) { sample(chosen = false) } } }
        onNodeWithText("Pick a link").assertIsDisplayed()
        onNodeWithText("DETAIL").assertDoesNotExist()
    }

    @Test
    fun `narrow shows the detail only when chosen`() = runComposeUiTest {
        var chosen by mutableStateOf(false)
        setContent { LdTheme { Box(Modifier.size(400.dp, 700.dp)) { sample(chosen) } } }
        onNodeWithText("DETAIL").assertDoesNotExist()
        chosen = true
        waitForIdle()
        onNodeWithText("DETAIL").assertIsDisplayed()
    }
}

@Composable
private fun sample(chosen: Boolean) = LdMasterDetail(
    chosen = chosen,
    onClose = {},
    master = { LdText("MASTER", it) },
    detail = { LdText("DETAIL") },
    placeholderTitle = "Pick a link",
    placeholderMessage = "Its parts show here.",
)
