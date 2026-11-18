package dev.slothiesmooth.linksdetektor.designlibrary

import androidx.compose.ui.graphics.Color
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.LdColorsDark
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.contrastRatio
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LdContrastTest {
    private val c = LdColorsDark

    @Test
    fun `the tokens are the DESIGN md values`() {
        assertEquals(Color(0xFF0D0C0A), c.stage)
        assertEquals(Color(0xFF161411), c.surface)
        assertEquals(Color(0xFF1E1B17), c.surface2)
        assertEquals(Color(0xFF2A2722), c.line)
        assertEquals(Color(0xFFF5F1EA), c.fg)
        assertEquals(Color(0xFFA8A296), c.dim)
        assertEquals(Color(0xFF8BD7A2), c.ok)
        assertEquals(Color(0xFFFFB547), c.amber)
        assertEquals(Color(0xFFFFC670), c.amberHover)
        assertEquals(Color(0xFF1A1206), c.amberInk)
    }

    @Test
    fun `every text pair clears 4_5 to 1`() {
        persistentListOf(
            c.fg to c.stage, c.fg to c.surface, c.fg to c.surface2,
            c.dim to c.stage, c.dim to c.surface, c.dim to c.surface2,
            c.amber to c.stage, c.amber to c.surface, c.ok to c.surface,
            c.amberInk to c.amber, c.amberInk to c.amberHover,
        ).forEach { (fg, bg) -> assertTrue(contrastRatio(fg, bg) >= 4.5, "$fg on $bg") }
    }

    @Test
    fun `contrast of white on black is 21`() {
        assertEquals(21.0, contrastRatio(Color.White, Color.Black), 0.01)
    }
}
