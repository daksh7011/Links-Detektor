package dev.slothiesmooth.linksdetektor.designlibrary

import dev.slothiesmooth.linksdetektor.designlibrary.atoms.focusRingInset
import kotlin.test.Test
import kotlin.test.assertEquals

class FocusRingTest {
    @Test
    fun `an outside ring is centred past the gap and half its width`() {
        assertEquals(-3f, focusRingInset(outside = true, gap = 2f, stroke = 2f))
        assertEquals(-6f, focusRingInset(outside = true, gap = 4f, stroke = 4f))
    }

    @Test
    fun `an inside ring is centred half its width in from the edge`() {
        assertEquals(1f, focusRingInset(outside = false, gap = 2f, stroke = 2f))
    }
}
