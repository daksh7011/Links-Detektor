package com.slothiesmooth.linksdetektor

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [NormalizedUrl] behaviour that the golden tests do not pin down.
 */
class NormalizedUrlTest {

    @Test
    fun `mutating the returned hostBytes does not change later calls`() {
        val url = LinksDetektor("http://1.2.3.4/x", LinksDetektorOptions.Default).detect().first().normalize()
        val first = assertNotNull(url.hostBytes)
        val expected = first.copyOf()
        first.fill(0x7F)

        assertContentEquals(expected, url.hostBytes)
    }

    @Test
    fun `fullUrl falls back to the raw host when the host cannot be normalised`() {
        // The detector accepts this host, but IDN conversion rejects the lone surrogate in it.
        val url = LinksDetektor("http://${LONE_SURROGATE}a.com", LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect().first().normalize()

        assertNull(url.host)
        assertEquals("http://${LONE_SURROGATE}a.com/", url.fullUrl)
        assertEquals("http://${LONE_SURROGATE}a.com/", url.toString())
    }

    private companion object {
        // Not a const: a constant-folded string holding a lone surrogate is rewritten to U+FFFD in klibs.
        val LONE_SURROGATE: Char = Char(0xD800)
    }
}
