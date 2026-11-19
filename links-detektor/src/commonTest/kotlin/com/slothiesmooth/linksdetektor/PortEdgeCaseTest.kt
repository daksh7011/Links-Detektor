package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ports that are followed by more text.
 */
class PortEdgeCaseTest {

    @Test
    fun portFollowedByTextStaysOutOfTheHost() {
        val inputs = persistentListOf(
            "see http://example.com:8080 now",
            "http://example.com:8080, and more",
            "(http://example.com:8080)",
            "\"http://example.com:8080\"",
            "http://example.com:8080x",
        )
        for (input in inputs) {
            val url = LinksDetektor(input, LinksDetektorOptions.Default).detect().first()

            assertEquals("example.com", url.host, "case: $input")
            assertEquals(8080, url.port, "case: $input")
            assertEquals("http://example.com:8080/", url.fullUrl, "case: $input")
            assertEquals("example.com", url.normalize().host, "case: $input")
        }
    }

    @Test
    fun portIsKeptWhenAMatchedBracketOrQuoteEndsTheUrl() {
        val html = LinksDetektor("<a href=\"https://example.com:8443\">x</a>", LinksDetektorOptions.HTML).detect().first()
        assertEquals("example.com", html.host)
        assertEquals(8443, html.port)

        val bracket = LinksDetektor("[http://example.com:8080]", LinksDetektorOptions.BRACKET_MATCH).detect().first()
        assertEquals("example.com", bracket.host)
        assertEquals(8080, bracket.port)
    }

    @Test
    fun colonWithoutDigitsIsNotAPort() {
        val url = LinksDetektor("http://example.com:hello", LinksDetektorOptions.Default).detect().first()

        assertEquals("http://example.com", url.originalUrl)
        assertEquals("example.com", url.host)
        assertEquals(80, url.port)
    }
}

class PortRangeTest {

    private fun detect(input: String): Url = LinksDetektor(input, LinksDetektorOptions.Default).detect().first()

    @Test
    fun portAboveTheValidRangeMeansNoPort() {
        for (written in persistentListOf("65536", "2147483647", "99999999999")) {
            val url = detect("http://example.com:$written/x")

            assertEquals("example.com", url.host, "case: $written")
            assertEquals(-1, url.port, "case: $written")
            assertEquals("http://example.com/x", url.fullUrl, "case: $written")
        }
    }

    @Test
    fun highestValidPortIsKept() {
        val url = detect("http://example.com:65535/x")

        assertEquals(65535, url.port)
        assertEquals("http://example.com:65535/x", url.fullUrl)
    }

    @Test
    fun portZeroIsKeptAsWrittenButNotRendered() {
        val url = detect("http://example.com:0/x")

        assertEquals(0, url.port)
        assertEquals("http://example.com/x", url.fullUrl)
    }
}
