package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
            "a:b c", "1:2 x", "[", "[]", "a [ b", "x:@[]", "http://a:@[]", "[::%2",
            "//:@.", "//:@[", "//:@\u3002", "HTTP://:@.",
        )
        val entries = LinksDetektorOptions.entries.toImmutableList()
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

    @Test
    fun `short garbage with an empty username does not throw`() {
        for (input in persistentListOf("//:@.", "//:@[", "//:@\u3002", "HTTP://:@.")) {
            LinksDetektor(input, LinksDetektorOptions.Default).detect()
            LinksDetektor(input, LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect()
        }
    }

    @Test
    fun `urls whose domain reader rewrote the buffer have consistent accessors`() {
        for (input in persistentListOf("http://%3A.\u30A4\u30F3", "http%3a//}?.o.uk")) {
            for (url in LinksDetektor(input, LinksDetektorOptions.Default).detect()) {
                touchEverything(url)
                assertEquals(url.fullUrl, url.toString())
                assertFalse(url.host.orEmpty().contains('?'), "case: $input host: ${url.host}")
            }
        }
    }

    @Test
    fun `detect is idempotent`() {
        for (input in inputs) {
            val detector = LinksDetektor(input, LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN)
            assertEquals(found(detector), found(detector), "case: $input")
        }
    }

    // The fuzz is split into several tests of 2_000 strings each so that every one stays well under the
    // 2 s per-test timeout of the JS test runner; together they cover 10_000 strings x every option.
    @Test
    fun `seeded fuzz 1`() = fuzz(20261118)

    @Test
    fun `seeded fuzz 2`() = fuzz(20261119)

    @Test
    fun `seeded fuzz 3`() = fuzz(20261120)

    @Test
    fun `seeded fuzz 4`() = fuzz(20261121)

    @Test
    fun `seeded fuzz 5`() = fuzz(20261122)

    /** Never throws, and every accessor of every result works, for [FUZZ_ROUNDS] random strings x every option. */
    private fun fuzz(seed: Int) {
        val alphabet = "aAzZ09:/@.[]()%?#'\"<>{}\u3002\uFF0E\u30A4\u00E9\u00DF -_=&~+;,"
        val random = Random(seed)
        val entries = LinksDetektorOptions.entries.toImmutableList()
        repeat(FUZZ_ROUNDS) { round ->
            val length = random.nextInt(0, 41)
            val text = buildString { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
            // Bias a third of the inputs towards URL-ish prefixes so the deeper states are reached.
            val input = if (round % 3 == 0) PREFIXES[random.nextInt(PREFIXES.size)] + text else text
            for (option in entries) {
                val urls = try {
                    LinksDetektor(input, option).detect()
                } catch (e: Exception) {
                    throw AssertionError("detect() threw for \"$input\" with $option", e)
                }
                for (url in urls) {
                    try {
                        touchEverything(url)
                    } catch (e: Exception) {
                        throw AssertionError("accessor threw for \"$input\" with $option on ${url.originalUrl}", e)
                    }
                }
            }
        }
    }

    private fun touchEverything(url: Url) {
        url.toString()
        url.fullUrl
        url.host
        url.path
        url.query
        url.fragment
        url.range
        url.scheme
        url.username
        url.password
        url.port
        val normalized = url.normalize()
        normalized.fullUrl
        normalized.host
        normalized.path
        normalized.hostBytes
    }

    private companion object {
        const val FUZZ_ROUNDS = 2_000

        val PREFIXES: ImmutableList<String> = persistentListOf(
            "http://", "https://", "//", "HTTP://:@", "http://a:b@", "ftp://[", "http%3a//", "x:",
        )
    }
}
