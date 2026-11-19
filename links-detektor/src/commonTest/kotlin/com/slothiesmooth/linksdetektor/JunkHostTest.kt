package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
}
