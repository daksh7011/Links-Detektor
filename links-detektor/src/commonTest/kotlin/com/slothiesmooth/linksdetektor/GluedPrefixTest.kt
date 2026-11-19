package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
        "\u0130(http://a.com)",
        "\u0130\u0130,http://a.com",
        "\u0130\u0130\u0130\u0130\u0130\u0130\u0130(http://localhost)",
        "{http://localhost}",
        "<http://localhost>",
        "(http://localhost\"",
        "(\"http://localhost)",
        "*http://localhost*",
        "!http://localhost!",
        "<b>http://a.com</b>",
        "<a href=\"http://localhost\">http://localhost</a>",
    )

    private val wrapperCharacters = "([{<\"'"
    private val closerCharacters = ")]}>\"'"

    @Test
    fun hostAndRangeNeverIncludeTheWrapperUnderAnyOption() {
        for (option in LinksDetektorOptions.entries) {
            for (input in wrapped) {
                for (url in LinksDetektor(input, option).detect()) {
                    val case = "option=$option input=$input host=${url.host} range=${url.range}"
                    val host = url.host.orEmpty()
                    assertFalse(host.any { it in wrapperCharacters || it in closerCharacters }, case)
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

    @Test
    fun capitalDottedIBeforeTheSchemeDoesNotShiftTheCut() {
        for (option in LinksDetektorOptions.entries) {
            for (input in persistentListOf("\u0130(http://a.com)", "\u0130\u0130,http://a.com")) {
                val url = LinksDetektor(input, option).detect().first()

                assertEquals("http", url.scheme, "option=$option input=$input")
                assertEquals("a.com", url.host, "option=$option input=$input")
            }
        }
    }

    @Test
    fun everyWrapperEndsASingleWordHost() {
        val options = LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN
        for (input in persistentListOf(
            "[http://localhost]",
            "{http://localhost}",
            "<http://localhost>",
            "(http://localhost\"",
            "(\"http://localhost)",
            "*http://localhost*",
            "!http://localhost!",
        )) {
            val urls = LinksDetektor(input, options).detect()

            assertEquals(1, urls.size, input)
            assertEquals("localhost", urls.first().host, input)
            assertEquals("http://localhost", input.substring(urls.first().range), input)
        }
    }

    @Test
    fun closingTagAfterAGluedGreaterThanKeepsTheScheme() {
        for (option in LinksDetektorOptions.entries) {
            assertEquals("http://a.com", LinksDetektor("<b>http://a.com</b>", option).detect().first().originalUrl?.take(12), "option=$option")
        }
        val urls = LinksDetektor("<a href=\"http://localhost\">http://localhost</a>", LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect()
        assertEquals(persistentListOf("localhost", "localhost"), urls.map { it.host }.toImmutableList())
        assertEquals(persistentListOf("http://localhost", "http://localhost"), urls.map { it.originalUrl }.toImmutableList())
    }
}
