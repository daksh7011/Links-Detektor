package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.nfkc.TableNfkc
import kotlin.test.Test
import kotlin.test.assertEquals

/** Runs the table-driven NFKC on every target, including those that normally use a platform normalizer. */
class TableNfkcTest {
    @Test
    fun `compatibility folding and composition`() {
        assertEquals("ex", TableNfkc.normalize("\uFF45\uFF58"))
        assertEquals("IX", TableNfkc.normalize("\u2168"))
        assertEquals("caf\u00E9", TableNfkc.normalize("cafe\u0301"))
        assertEquals("\u30AC", TableNfkc.normalize("\uFF76\uFF9E"))
        assertEquals("fi", TableNfkc.normalize("\uFB01"))
        assertEquals("", TableNfkc.normalize(""))
    }

    @Test
    fun `hangul is composed and decomposed algorithmically`() {
        assertEquals("\uD55C", TableNfkc.normalize("\u1112\u1161\u11AB"))
        assertEquals("\uAC01", TableNfkc.normalize("\uAC00\u11A8"))
    }

    @Test
    fun `combining marks are put in canonical order`() {
        assertEquals("\u1EA1\u0301", TableNfkc.normalize("a\u0301\u0323"))
    }

    @Test
    fun `supplementary code points are handled as pairs`() {
        assertEquals("A", TableNfkc.normalize("\uD835\uDC00"))
        assertEquals("aA", TableNfkc.normalize("a\uD835\uDC00"))
    }
}
