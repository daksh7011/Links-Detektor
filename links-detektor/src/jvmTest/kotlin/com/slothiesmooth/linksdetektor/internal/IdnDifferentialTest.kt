package com.slothiesmooth.linksdetektor.internal

import java.net.IDN
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * JVM-only: `PureIdn` must agree with `java.net.IDN` (the 2.x implementation) on every host both accept.
 */
class IdnDifferentialTest {

    private fun assertSameOutcome(input: String) {
        val pure = runCatching { PureIdn.toAscii(input) }
        val java = runCatching { IDN.toASCII(input) }
        if (java.isSuccess) {
            assertEquals(java.getOrNull(), pure.getOrNull(), "case: $input")
        } else {
            assertTrue(java.exceptionOrNull() is IllegalArgumentException, "case: $input")
            assertTrue(pure.exceptionOrNull() is IllegalArgumentException, "case: $input")
        }
    }

    @Test
    fun equalCorpusAgrees() {
        for ((input, expected) in IdnCorpus.EQUAL_CORPUS) {
            assertEquals(expected, IDN.toASCII(input), "java: $input")
            assertSameOutcome(input)
        }
    }

    @Test
    fun rejectsAgree() {
        for (input in IdnCorpus.REJECTS) {
            assertSameOutcome(input)
            assertFailsWith<IllegalArgumentException>("case: $input") { IDN.toASCII(input) }
        }
    }

    @Test
    fun knownDivergencesAreRejectedByJava() {
        for ((input, _) in IdnCorpus.KNOWN_DIVERGENCES) {
            assertFailsWith<IllegalArgumentException>("case: $input") { IDN.toASCII(input) }
        }
    }
}
