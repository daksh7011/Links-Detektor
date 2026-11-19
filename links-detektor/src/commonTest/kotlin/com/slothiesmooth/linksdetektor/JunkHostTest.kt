package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * A host never holds a bracket or a quote; text such as `>x<` or `{"u"` is not a single-level domain.
 */
class JunkHostTest {

    private val junkCharacters = "<>\"'()[]{}"

    private val inputs: ImmutableList<String> = persistentListOf(
        "<a href=\"http://localhost\">x</a>",
        "{\"u\":\"http://a.com\"}",
        "<a href=\"http://localhost\">x</a> and {\"u\":\"http://a.com\"}",
    )

    @Test
    fun noHostHoldsABracketOrQuoteUnderAnyOption() {
        for (option in LinksDetektorOptions.entries) {
            for (input in inputs) {
                for (url in LinksDetektor(input, option).detect()) {
                    val host = url.host.orEmpty()
                    assertFalse(host.any { it in junkCharacters }, "option=$option input=$input host=$host")
                }
            }
        }
    }

    @Test
    fun theRealUrlsAreStillFound() {
        val single = LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN
        assertEquals(
            persistentListOf<String?>("localhost"),
            LinksDetektor(inputs[0], single).detect().map { it.host }.toImmutableList(),
        )
        assertEquals("a.com", LinksDetektor(inputs[1], single).detect().first { it.host == "a.com" }.host)
    }

    @Test
    fun bracketedIpv6IsStillAHost() {
        val url = LinksDetektor("http://[2001:db8::1]/x", LinksDetektorOptions.Default).detect().first()

        assertEquals("[2001:db8::1]", url.host)
    }

    @Test
    fun anUnmatchedCloserAfterASingleWordHostIsNotPartOfTheUrl() {
        val expected = persistentMapOf(
            "(served at http://localhost)" to "localhost",
            "http://localhost)" to "localhost",
            "open \"the app at http://localhost\"" to "localhost",
            "x http://localhost' y" to "localhost",
            "{url: http://svc}" to "svc",
            "[docs: http://wiki]" to "wiki",
            "see http://localhost> now" to "localhost",
        )
        val single = LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN
        for ((input, host) in expected) {
            val urls = LinksDetektor(input, single).detect()

            assertEquals(persistentListOf<String?>(host), urls.map { it.host }.toImmutableList(), input)
            val text = input.substring(urls.first().range)
            assertEquals("http://$host", text, input)
        }
    }

    @Test
    fun noHostEndsWithAColonUnderAnyOption() {
        for (option in LinksDetektorOptions.entries) {
            for (input in persistentListOf("[docs: http://wiki]", "docs: http://wiki", "see docs: now")) {
                for (url in LinksDetektor(input, option).detect()) {
                    assertFalse(url.host.orEmpty().endsWith(":"), "option=$option input=$input host=${url.host}")
                }
            }
        }
    }

    @Test
    fun junkInsideASingleWordHostStaysRejected() {
        val single = LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN
        for (input in persistentListOf("http://lo(cal", "http://lo\"cal", "http://a>b")) {
            for (url in LinksDetektor(input, single).detect()) {
                assertFalse(url.host.orEmpty().any { it in junkCharacters }, "input=$input host=${url.host}")
            }
        }
    }
}
