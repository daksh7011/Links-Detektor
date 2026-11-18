package com.slothiesmooth.linksdetektor.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class NfkcTest {
    @Test
    fun `nfkc folds compatibility characters`() {
        assertEquals("ex", nfkc("\uFF45\uFF58"))
        assertEquals("IX", nfkc("\u2168"))
        assertEquals("caf\u00E9", nfkc("caf\u00E9"))
        assertEquals("\u30AC", nfkc("\uFF76\uFF9E"))
        assertEquals("fi", nfkc("\uFB01"))
        assertEquals("abc", nfkc("abc"))
        assertEquals("", nfkc(""))
    }
}
