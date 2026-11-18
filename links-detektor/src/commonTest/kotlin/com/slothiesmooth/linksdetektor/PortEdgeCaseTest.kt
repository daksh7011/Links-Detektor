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
