package com.slothiesmooth.linksdetektor.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * Normalising a very long path stays linear. With quadratic in-place edits the decode case took about 7 s on the JVM
 * and the others seconds (all far longer on JS); with linear edits they take milliseconds.
 */
class NormalizeScalingTest {

    @Test
    fun decodesALongRunOfEncodedCharactersInLinearTime() {
        val (decoded, elapsed) = measureTimedValue { UrlUtil.decode("%41".repeat(REPEATS)) }

        assertEquals("A".repeat(REPEATS), decoded)
        assertTrue(elapsed < LIMIT, "decode took $elapsed")
    }

    @Test
    fun collapsesALongRunOfSlashesInLinearTime() {
        val (normalized, elapsed) = measureTimedValue { PathNormalizer().normalizePath("/".repeat(REPEATS) + "a") }

        assertEquals("/a", normalized)
        assertTrue(elapsed < LIMIT, "normalizePath took $elapsed")
    }

    @Test
    fun resolvesALongRunOfDotSegmentsInLinearTime() {
        val (normalized, elapsed) = measureTimedValue { PathNormalizer().normalizePath("/a/..".repeat(REPEATS / 5) + "/b") }

        assertEquals("/b", normalized)
        assertTrue(elapsed < LIMIT, "normalizePath took $elapsed")
    }

    private companion object {
        const val REPEATS = 300_000
        val LIMIT = 3.seconds
    }
}
