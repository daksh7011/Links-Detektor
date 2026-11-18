package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.nfkc.TableNfkc
import java.text.Normalizer
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks [TableNfkc] against `java.text.Normalizer` (Unicode 15.0.0 on JDK 21) for every code point. */
class TableNfkcDifferentialTest {

    private fun assertMatchesJdk(input: String, label: String) {
        val expected = Normalizer.normalize(input, Normalizer.Form.NFKC)
        val actual = TableNfkc.normalize(input)
        assertEquals(expected, actual, "$label: ${input.toCharArray().joinToString(" ") { it.code.toString(16) }}")
    }

    private fun hex(cp: Int) = "U+" + cp.toString(16).uppercase().padStart(4, '0')

    @Test
    fun `every code point matches the JDK`() {
        for (cp in 0..Character.MAX_CODE_POINT) {
            if (cp in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code) continue
            val s = String(Character.toChars(cp))
            assertMatchesJdk(s, hex(cp))
            // Canonical and compatibility decompositions exercise composition, exclusions and reordering.
            assertMatchesJdk(Normalizer.normalize(s, Normalizer.Form.NFD), "${hex(cp)} NFD")
            assertMatchesJdk(Normalizer.normalize(s, Normalizer.Form.NFKD), "${hex(cp)} NFKD")
            assertMatchesJdk("a$s\u0301", "${hex(cp)} between a and acute")
            assertMatchesJdk("$s\u0323\u0301\u0316", "${hex(cp)} followed by unordered marks")
        }
    }

    @Test
    fun `lone surrogates pass through like the JDK`() {
        for (c in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code) {
            assertMatchesJdk("\u00E9" + c.toChar() + "a\u0301", "lone surrogate ${hex(c)}")
        }
    }

    @Test
    fun `hangul jamo sequences match the JDK`() {
        for (l in 0x1100..0x1112) {
            for (v in 0x1161..0x1175) {
                assertMatchesJdk("${l.toChar()}${v.toChar()}", "L+V ${hex(l)} ${hex(v)}")
                for (t in 0x11A8..0x11C2) {
                    assertMatchesJdk("${l.toChar()}${v.toChar()}${t.toChar()}", "L+V+T ${hex(l)} ${hex(v)} ${hex(t)}")
                }
            }
        }
        for (s in 0xAC00..0xD7A3 step 7) {
            assertMatchesJdk("${s.toChar()}\u11A8", "LV+T ${hex(s)}")
            assertMatchesJdk("${s.toChar()}\u1161", "S+V ${hex(s)}")
        }
    }

    @Test
    fun `kana with voiced marks match the JDK`() {
        val marks = "\u3099\u309A\uFF9E\uFF9F\u309B\u309C"
        for (range in arrayOf(0x3040..0x30FF, 0xFF66..0xFF9F)) {
            for (base in range) {
                for (mark in marks) {
                    assertMatchesJdk("${base.toChar()}$mark", "kana ${hex(base)} ${hex(mark.code)}")
                }
            }
        }
    }

    @Test
    fun `latin letters with several combining marks in any order match the JDK`() {
        val marks = "\u0300\u0301\u0302\u0303\u0308\u0323\u0327\u0328\u0331\u0345\u0316\u035C"
        for (base in "AaEeIiOoUuCcNnSsZzgGyY\u00C5") {
            for (m1 in marks) {
                assertMatchesJdk("$base$m1", "$base+${hex(m1.code)}")
                for (m2 in marks) {
                    assertMatchesJdk("$base$m1$m2", "$base+${hex(m1.code)}+${hex(m2.code)}")
                    assertMatchesJdk("$base$m1$m2$m1", "$base+${hex(m1.code)}+${hex(m2.code)}+again")
                }
            }
        }
    }

    @Test
    fun `idn corpus inputs match the JDK`() {
        for ((input, _) in IdnCorpus.EQUAL_CORPUS) assertMatchesJdk(input, "idn corpus")
    }
}
