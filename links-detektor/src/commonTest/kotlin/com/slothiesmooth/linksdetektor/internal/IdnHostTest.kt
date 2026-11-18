package com.slothiesmooth.linksdetektor.internal

import kotlin.test.Test
import kotlin.test.assertEquals

/** Hosts that `java.net.IDN` rejected in 2.x on the JVM. Every platform now encodes them the same way. */
class IdnHostTest {

    @Test
    fun emojiHostIsEncoded() {
        assertEquals("xn--e28h.com", HostNormalizer("😀.com").normalizedHost)
    }

    @Test
    fun capitalSharpSHostIsFolded() {
        assertEquals("ss.de", HostNormalizer("ẞ.de").normalizedHost)
    }
}
