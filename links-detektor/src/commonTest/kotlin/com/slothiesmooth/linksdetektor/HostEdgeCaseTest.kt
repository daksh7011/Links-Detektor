package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Hosts written in an unusual way.
 */
class HostEdgeCaseTest {

    @Test
    fun ipv6WithoutClosingBracketIsNotAHost() {
        for (input in persistentListOf("http://[::1", "http://[1::2", "http://[::1 x", "see [fe80::1 there")) {
            val hosts = LinksDetektor(input, LinksDetektorOptions.Default).detect().map { it.host }.toImmutableList()

            assertEquals(persistentListOf(), hosts, "case: $input")
        }
    }

    @Test
    fun closedIpv6IsStillAHost() {
        val url = LinksDetektor("http://[::1]:8080/a", LinksDetektorOptions.Default).detect().first()

        assertEquals("[::1]", url.host)
        assertEquals(8080, url.port)
    }

    @Test
    fun upperCaseEncodedDotSeparatesLabels() {
        val name = LinksDetektor("http://example%2Ecom/a", LinksDetektorOptions.Default).detect().first()
        assertEquals("http://example%2Ecom/a", name.originalUrl)
        assertEquals("example.com", name.normalize().host)

        val address = LinksDetektor("http://1%2E2%2E3%2E4/a", LinksDetektorOptions.Default).detect().first().normalize()
        assertEquals("1.2.3.4", address.host)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), address.hostBytes?.copyOfRange(12, 16))
    }

    /** A decoded host must not gain a character that would end the host when the normalised URL is parsed again. */
    @Test
    fun normalisedHostKeepsUrlDelimitersEncoded() {
        val cases = persistentListOf(
            "http://a.com%40b.com/" to "a.com%40b.com",
            "http://a.com%2Fb.com/" to "a.com%2Fb.com",
            "http://a.com%3Fb.com/" to "a.com%3Fb.com",
            "http://a.com%5Cb.com/" to "a.com%5Cb.com",
            "http://a.com%3A80/" to "a.com%3A80",
            "http://a\uFF20b.com/" to "a%40b.com",
        )
        for ((input, expectedHost) in cases) {
            val url = LinksDetektor(input, LinksDetektorOptions.Default).detect().first().normalize()

            assertEquals(expectedHost, url.host, "case: $input")
            assertEquals("http://$expectedHost/", url.fullUrl, "case: $input")
        }
    }

    @Test
    fun normalisedIpv6HostKeepsItsColons() {
        val url = LinksDetektor("http://[2001:db8::1]/", LinksDetektorOptions.Default).detect().first().normalize()

        assertEquals("[2001:db8:0:0:0:0:0:1]", url.host)
    }
}
