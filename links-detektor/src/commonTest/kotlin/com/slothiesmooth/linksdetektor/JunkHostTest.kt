package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
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
    fun noHostEndsWithAColonUnderAnyOptionPair() {
        val colonInputs = persistentListOf(
            "[docs: http://wiki]", "docs: http://wiki", "see docs: now", "key:\"http://a.com\"",
            "{url:\"http://svc\"}", "var u={host:'h'}", "title:<b>", "docs:(x)", "[docs:(http://wiki)]",
        )
        for (option in LinksDetektorOptions.entries) {
            val options = persistentSetOf(LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, option)
            for (input in colonInputs) {
                for (url in LinksDetektor(input, options).detect()) {
                    assertFalse(url.host.orEmpty().endsWith(":"), "options=$options input=$input host=${url.host}")
                }
            }
        }
    }

    @Test
    fun singleWordHostWithAPortKeepsItsPortAndDropsTheCloser() {
        val single = LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN
        val expected = persistentMapOf(
            "http://localhost:8080" to "http://localhost:8080",
            "localhost:8080" to "localhost:8080",
            "http://localhost:8080)" to "http://localhost:8080",
            "(served at http://localhost:8080)" to "http://localhost:8080",
            "http://localhost:8080 x" to "http://localhost:8080",
        )
        for ((input, text) in expected) {
            val urls = LinksDetektor(input, single).detect()

            assertEquals(1, urls.size, input)
            assertEquals("localhost", urls.first().host, input)
            assertEquals(8080, urls.first().port, input)
            assertEquals(text, input.substring(urls.first().range), input)
        }
    }

    @Test
    fun anEmptyHostIsNotEndedByACloserThatFollowsTheScheme() {
        val single = LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN
        for (input in persistentListOf("https://}evil.com", "x http://)evil.com y", "(http://)a.com", "http%3a//}?.o.uk")) {
            for (url in LinksDetektor(input, single).detect()) {
                assertFalse(url.host.orEmpty().any { it in junkCharacters }, "input=$input host=${url.host}")
            }
        }
    }

    @Test
    fun aLeadingWrapperIsNotPartOfADottedHost() {
        val single = LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN
        val expected = persistentMapOf("(a.com)" to "a.com", "'a.com'" to "a.com", "see (a.com) and 'b.org'" to "a.com")
        for ((input, host) in expected) {
            assertEquals(host, LinksDetektor(input, single).detect().first().host, input)
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
