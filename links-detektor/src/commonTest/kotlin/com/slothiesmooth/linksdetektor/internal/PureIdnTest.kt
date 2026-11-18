package com.slothiesmooth.linksdetektor.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PureIdnTest {

    @Test
    fun matchesJavaIdn() {
        for ((input, expected) in IdnCorpus.EQUAL_CORPUS) {
            assertEquals(expected, PureIdn.toAscii(input), "case: $input")
        }
    }

    @Test
    fun rejects() {
        for (input in IdnCorpus.REJECTS) {
            assertFailsWith<IllegalArgumentException>("case: $input") { PureIdn.toAscii(input) }
        }
    }

    @Test
    fun knownDivergenceFromJavaIdn() {
        for ((input, expected) in IdnCorpus.KNOWN_DIVERGENCES) {
            assertEquals(expected, PureIdn.toAscii(input), "case: $input")
        }
    }

    @Test
    fun loneSeparatorIsTheRootLabel() {
        assertEquals(".", PureIdn.toAscii("。"))
        assertEquals(".", PureIdn.toAscii("．"))
        assertEquals(".", PureIdn.toAscii("｡"))
    }
}
