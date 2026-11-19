package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class PunycodeTest {

    @Test
    fun encodesLabels() {
        persistentListOf(
            "bücher" to "bcher-kva",
            "例え" to "r8jz45g",
            "αβγ" to "mxacd",
            "café" to "caf-dma",
            "münchen" to "mnchen-3ya",
            "한국" to "3e0b707e",
            "ไทย" to "o3cw4h",
            "испытание" to "80akhbyknj4f",
            "рф" to "p1ai",
            // A surrogate pair is one code point.
            "😀" to "e28h",
            "üüü" to "tdaaa",
        ).forEach { (label, expected) ->
            assertEquals(expected, Punycode.encode(label), "case: $label")
        }
    }
}
