package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the option-set constructor of [LinksDetektor].
 */
class LinksDetektorOptionSetTest {

    /** The detection inputs of [UrlRangeTest], without their per-row options. */
    private val inputs: ImmutableList<String> = persistentListOf(
        "http://example.com",
        "Visit http://example.com and https://example.org for more",
        "x http://a.com\nhttp://b.com/p\ty",
        "<a href=\"http://a.com/b\">http://c.com</a>",
        "{\"u\":\"http://a.com/x\"}",
        "\"http://a.com\" 'http://b.com'",
        "a.com a.com a.com",
        "go to example.com.",
        "http://a.com:hello.world",
        "hello:world.com/x",
        "http://a.com:80a b.com:",
        "x.com\"\"",
        "x.co.. y",
        "http://[::1]] x",
        "ｅｘ。com 例え。jp/x",
        "%%a.com",
        "see http://exa%mple.com/x ok",
        "ab%zz.com rest",
        "a.com a%.com",
    )

    private fun found(detector: LinksDetektor): ImmutableList<Pair<String?, IntRange>> =
        detector.detect().map { it.originalUrl to it.range }.toImmutableList()

    @Test
    fun `quote plus single quote set matches both`() {
        val input = "\"http://a.com\" 'http://b.com'"
        val urls = LinksDetektor(
            input,
            persistentSetOf(LinksDetektorOptions.QUOTE_MATCH, LinksDetektorOptions.SINGLE_QUOTE_MATCH)
        ).detect().map { it.originalUrl }.toImmutableList()

        assertEquals(persistentListOf<String?>("http://a.com", "http://b.com"), urls)
    }

    @Test
    fun `quote plus single quote plus bracket set equals the JAVASCRIPT constant`() {
        val set = persistentSetOf(
            LinksDetektorOptions.QUOTE_MATCH,
            LinksDetektorOptions.SINGLE_QUOTE_MATCH,
            LinksDetektorOptions.BRACKET_MATCH,
        )
        for (input in inputs) {
            assertEquals(
                found(LinksDetektor(input, LinksDetektorOptions.JAVASCRIPT)),
                found(LinksDetektor(input, set)),
                "case: $input"
            )
        }
    }

    @Test
    fun `single-level domain combines with bracket match`() {
        val urls = LinksDetektor(
            "(http://localhost)",
            persistentSetOf(LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, LinksDetektorOptions.BRACKET_MATCH)
        ).detect().map { it.originalUrl }.toImmutableList()

        assertEquals(persistentListOf<String?>("http://localhost"), urls)
    }

    @Test
    fun `single-level domain combines with quote match and keeps later urls`() {
        val urls = LinksDetektor(
            "\"http://localhost\" and (http://intranet) http://a.com",
            persistentSetOf(
                LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN,
                LinksDetektorOptions.QUOTE_MATCH,
                LinksDetektorOptions.BRACKET_MATCH,
            )
        ).detect().map { it.originalUrl }.toImmutableList()

        assertEquals(persistentListOf<String?>("http://localhost", "http://intranet", "http://a.com"), urls)
    }

    @Test
    fun `single option constructor equals singleton set`() {
        for (option in LinksDetektorOptions.entries) {
            for (input in inputs) {
                assertEquals(
                    found(LinksDetektor(input, option)),
                    found(LinksDetektor(input, persistentSetOf(option))),
                    "case: $input / $option"
                )
            }
        }
    }

    @Test
    fun `empty set behaves like Default`() {
        for (input in inputs) {
            assertEquals(
                found(LinksDetektor(input, LinksDetektorOptions.Default)),
                found(LinksDetektor(input, persistentSetOf())),
                "case: $input"
            )
        }
    }

    @Test
    fun `no option set throws on short inputs`() {
        val corpus = persistentListOf(
            "[1:2]", "(a)", "[a.b]", "\"x\"", "'y'", "<z>", "{q}", "a.", ".a", "[::1]",
            "http://[1:2]", "(http://localhost)", "[localhost]",
            "a:b c", "1:2 x",
        )
        val entries = LinksDetektorOptions.entries
        for (bits in 0 until (1 shl entries.size)) {
            var subset = persistentSetOf<LinksDetektorOptions>()
            for (index in entries.indices) {
                if (bits and (1 shl index) != 0) subset = subset.adding(entries[index])
            }
            for (input in corpus) {
                try {
                    LinksDetektor(input, subset).detect()
                } catch (e: Exception) {
                    throw AssertionError("detect() threw for \"$input\" with $subset", e)
                }
            }
        }
    }
}
