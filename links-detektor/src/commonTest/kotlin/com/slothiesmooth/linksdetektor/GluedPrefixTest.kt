package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Punctuation glued before a scheme is not part of the URL: it starts at the scheme.
 */
class GluedPrefixTest {

    private val wrapped: ImmutableList<String> = persistentListOf(
        "then (http://localhost) x",
        "(http://a.com)",
        "(http://a.com/p)",
        "'http://a.com'",
        "\"http://a.com\" x",
        "'http://localhost'",
        "\"http://localhost\"",
        "[http://a.com]",
        "[http://localhost]",
        "{https://a.com}",
        "<http://a.com>",
        "x,http://a.com;",
        "x;http://a.com",
        "*http://a.com",
        "!http://a.com",
        ":http://a.com",
        "(HTTP://A.COM)",
        "(http%3a//a.com)",
        "(ftp://a.com)",
        "((http://a.com))",
    )

    private val wrapperCharacters = "([{<\"'"

    @Test
    fun hostAndRangeNeverIncludeTheWrapperUnderAnyOption() {
        for (option in LinksDetektorOptions.entries) {
            for (input in wrapped) {
                for (url in LinksDetektor(input, option).detect()) {
                    val case = "option=$option input=$input host=${url.host} range=${url.range}"
                    val host = url.host.orEmpty()
                    assertFalse(host.any { it in wrapperCharacters }, case)
                    assertFalse(host.contains(":/"), case)
                    val text = input.substring(url.range)
                    assertFalse(text.first() in wrapperCharacters, case)
                }
            }
        }
    }

    @Test
    fun singleWordHostInBracketsStartsAtTheScheme() {
        val input = "then (http://localhost) x"
        for (option in LinksDetektorOptions.entries) {
            val urls = LinksDetektor(input, option).detect()
            if (option == LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) {
                assertEquals(1, urls.size, "option=$option")
                assertEquals("http://localhost", urls.first().originalUrl, "option=$option")
                assertEquals("localhost", urls.first().host, "option=$option")
                assertEquals("http://localhost", input.substring(urls.first().range), "option=$option")
            } else {
                assertTrue(urls.isEmpty(), "option=$option")
            }
        }
    }

    @Test
    fun quotedSingleWordHostStartsAtTheScheme() {
        for (input in persistentListOf("'http://localhost'", "\"http://localhost\"")) {
            val url = LinksDetektor(input, LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect().first()

            assertEquals("localhost", url.host, input)
            assertEquals("http://localhost", input.substring(url.range), input)
        }
    }

    @Test
    fun everySchemeCarryingInputKeepsItsSchemeUnderAnyOption() {
        for (option in LinksDetektorOptions.entries) {
            for (input in persistentListOf("(http://a.com)", "'http://a.com'", "x,http://a.com;", "[http://a.com]")) {
                val urls = LinksDetektor(input, option).detect()

                assertEquals(1, urls.size, "option=$option input=$input")
                assertTrue(urls.first().originalUrl?.startsWith("http://a.com") == true, "option=$option input=$input")
                assertEquals("http", urls.first().scheme, "option=$option input=$input")
                assertTrue(input.substring(urls.first().range).startsWith("http://a.com"), "option=$option input=$input")
            }
        }
    }
}
