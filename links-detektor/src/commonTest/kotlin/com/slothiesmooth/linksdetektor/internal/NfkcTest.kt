package com.slothiesmooth.linksdetektor.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class NfkcTest {
    @Test
    fun `nfkc folds compatibility characters`() {
        assertEquals("ex", nfkc("ｅｘ"))
        assertEquals("IX", nfkc("Ⅸ"))
        assertEquals("café", nfkc("café"))
        assertEquals("ガ", nfkc("ｶﾞ"))
        assertEquals("fi", nfkc("ﬁ"))
        assertEquals("abc", nfkc("abc"))
        assertEquals("", nfkc(""))
    }
}
