package com.slothiesmooth.linksdetektor.internal

import kotlin.test.Test
import kotlin.test.assertNull

/** Expectations that hold only where `java.net.IDN` performs the conversion. */
class JvmIdnHostTest {

    @Test
    fun emojiHostIsRejected() {
        assertNull(HostNormalizer("\uD83D\uDE00.com").normalizedHost)
    }

    @Test
    fun capitalSharpSHostIsRejected() {
        assertNull(HostNormalizer("\u1E9E.de").normalizedHost)
    }
}
