package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [Url.range].
 */
class UrlRangeTest {

    private data class RangeCase(
        val input: String,
        val options: LinksDetektorOptions,
        val expected: ImmutableList<IntRange>,
        /** True when the detector drops a stray `%`, so the range is longer than `originalUrl`. */
        val dropsStrayPercent: Boolean = false,
    )

    private fun ranges(s: String, o: LinksDetektorOptions): ImmutableList<IntRange> =
        LinksDetektor(s, o).detect().map { it.range }.toImmutableList()

    private val cases: ImmutableList<RangeCase> = persistentListOf(
        RangeCase("http://example.com", LinksDetektorOptions.Default, persistentListOf(0 until 18)),
        RangeCase(
            "Visit http://example.com and https://example.org for more",
            LinksDetektorOptions.Default,
            persistentListOf(6 until 24, 29 until 48)
        ),
        RangeCase(
            "x http://a.com\nhttp://b.com/p\ty",
            LinksDetektorOptions.Default,
            persistentListOf(2 until 14, 15 until 29)
        ),
        RangeCase(
            "<a href=\"http://a.com/b\">http://c.com</a>",
            LinksDetektorOptions.HTML,
            persistentListOf(9 until 23, 25 until 37)
        ),
        RangeCase("{\"u\":\"http://a.com/x\"}", LinksDetektorOptions.JSON, persistentListOf(6 until 20)),
        RangeCase(
            "\"http://a.com\" 'http://b.com'",
            LinksDetektorOptions.Default,
            persistentListOf(8 until 13, 23 until 28)
        ),
        RangeCase(
            "a.com a.com a.com",
            LinksDetektorOptions.Default,
            persistentListOf(0 until 5, 6 until 11, 12 until 17)
        ),
        RangeCase("go to example.com.", LinksDetektorOptions.Default, persistentListOf(6 until 18)),
        RangeCase(
            "http://a.com:hello.world",
            LinksDetektorOptions.Default,
            persistentListOf(0 until 12, 13 until 24)
        ),
        RangeCase(
            "hello:world.com/x",
            LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN,
            persistentListOf(0 until 5, 6 until 17)
        ),
        RangeCase(
            "http://a.com:80a b.com:",
            LinksDetektorOptions.Default,
            persistentListOf(0 until 15, 17 until 23)
        ),
        RangeCase("x.com\"\"", LinksDetektorOptions.Default, persistentListOf(0 until 5)),
        RangeCase("x.co.. y", LinksDetektorOptions.Default, persistentListOf(0 until 5)),
        RangeCase("http://[::1]] x", LinksDetektorOptions.Default, persistentListOf(0 until 12)),
        RangeCase(
            "ｅｘ。com 例え。jp/x",
            LinksDetektorOptions.Default,
            persistentListOf(0 until 6, 7 until 14)
        ),
        RangeCase("%%a.com", LinksDetektorOptions.Default, persistentListOf(2 until 7)),
        RangeCase(
            "see http://exa%mple.com/x ok",
            LinksDetektorOptions.Default,
            persistentListOf(4 until 25),
            dropsStrayPercent = true
        ),
        RangeCase(
            "ab%zz.com rest",
            LinksDetektorOptions.Default,
            persistentListOf(0 until 9),
            dropsStrayPercent = true
        ),
        RangeCase(
            "a.com a%.com",
            LinksDetektorOptions.Default,
            persistentListOf(0 until 5, 6 until 12),
            dropsStrayPercent = true
        ),
    )

    @Test
    fun rangesMatchSourceSpans() {
        for (case in cases) {
            assertEquals(case.expected, ranges(case.input, case.options), "case: ${case.input}")
        }
    }

    @Test
    fun substringMatchesOriginalUrl() {
        for (case in cases.filterNot { it.dropsStrayPercent }) {
            for (url in LinksDetektor(case.input, case.options).detect()) {
                val sliced = case.input.substring(url.range).replace('\n', ' ').replace('\t', ' ').replace('\r', ' ')
                assertEquals(url.originalUrl, sliced, "case: ${case.input}")
            }
        }
    }

    @Test
    fun rangesAreAscendingAndDisjoint() {
        for (case in cases) {
            for (options in LinksDetektorOptions.entries) {
                val found = ranges(case.input, options)
                found.forEachIndexed { index, range ->
                    assertTrue(!range.isEmpty(), "empty range: ${case.input} / $options")
                    if (index > 0) {
                        assertTrue(
                            range.first >= found[index - 1].last + 1,
                            "overlap: ${case.input} / $options / $found"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun normalizeKeepsRange() {
        for (case in cases) {
            for (url in LinksDetektor(case.input, case.options).detect()) {
                assertEquals(url.range, url.normalize().range, "case: ${case.input}")
            }
        }
    }

    @Test
    fun createRangeIsRelativeToSanitisedInput() {
        assertEquals(0 until 24, Url.create("  http://example.com/a b  ").range)
    }
}
