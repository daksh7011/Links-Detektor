package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the low-byte truncation quirk of [UrlUtil.encode]: each UTF-16 unit is reduced to its low byte.
 */
class UrlUtilEncodeGoldenTest {

    @Test
    fun encodeGoldens() {
        persistentListOf(
            "a b" to "a%20b",
            "caf\u00E9" to "caf%E9",
            "\u0100" to "%00",
            "\u4F8B" to "%8B",
            "\uD83D\uDE00" to "%3D%00",
            "#%" to "%23%25",
            "\u007F" to "%7F",
            "~" to "~",
            "\u0000\u001F" to "%00%1F"
        ).forEach { (input, expected) ->
            assertEquals(expected, UrlUtil.encode(input), "case: $input")
        }
    }
}
