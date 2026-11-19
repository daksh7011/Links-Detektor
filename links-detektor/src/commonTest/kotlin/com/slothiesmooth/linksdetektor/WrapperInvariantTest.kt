package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Whatever the option combination, brackets, quotes and colons around a URL never end up in its host.
 */
class WrapperInvariantTest {

    private val wrappers = "<>\"'()[]{}"

    private val corpus: ImmutableList<String> = persistentListOf(
        "then (http://localhost) x", "(http://a.com)", "(http://a.com/p)", "'http://a.com'", "\"http://a.com\" x",
        "'http://localhost'", "\"http://localhost\"", "[http://a.com]", "[http://localhost]", "{https://a.com}",
        "<http://a.com>", "x,http://a.com;", "*http://a.com", ":http://a.com", "(http%3a//a.com)", "((http://a.com))",
        "(\"http://localhost)", "*http://localhost*", "<b>http://a.com</b>",
        "<a href=\"http://localhost\">http://localhost</a>", "<a href=\"http://localhost\">x</a>",
        "{\"u\":\"http://a.com\"}", "(served at http://localhost)", "http://localhost)",
        "open \"the app at http://localhost\"", "x http://localhost' y", "{url: http://svc}", "[docs: http://wiki]",
        "see http://localhost> now", "docs: now", "http://evil#@good.com", "http://evil#u:p@good.com",
        "http://evil\\@good.com", "http://lo(cal", "http://a>b",
        "http%3a//}?.o.uk", "https://}evil.com", "x http://)evil.com y", "(http://)a.com",
        "key:\"http://a.com\"", "{url:\"http://svc\"}", "var u={host:'h'}", "title:<b>", "docs:(x)",
        "[docs:(http://wiki)]", "http://localhost:8080", "localhost:8080", "http://localhost:8080)",
        "(served at http://localhost:8080)", "http://a.com:8080)", "(a.com)", "'a.com'", "http://}a.com",
        "see (a.com) and 'b.org'", "http://[::1]/x", "[::1]",
    )

    /** Every subset of the eight non-default options: 256 combinations. */
    private val combinations: ImmutableList<ImmutableSet<LinksDetektorOptions>> = run {
        val options = LinksDetektorOptions.entries.filter { it != LinksDetektorOptions.Default }
        (0 until (1 shl options.size)).map { mask ->
            options.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.toImmutableSet()
        }.map { if (it.isEmpty()) persistentSetOf(LinksDetektorOptions.Default) else it }.toImmutableList()
    }

    @Test
    fun combinationsCoverEveryOptionPair() {
        assertEquals(256, combinations.size)
    }

    @Test
    fun hostsNeverHoldOrEndWithWrappersOrAColon() {
        val violations = persistentSetOf<String>().builder()
        for (options in combinations) {
            for (input in corpus) {
                for (url in LinksDetektor(input, options).detect()) {
                    val host = url.host.orEmpty()
                    if (!host.startsWith("[") && (host.any { it in wrappers } || host.endsWith(":"))) {
                        violations.add("$input -> host=$host")
                    }
                }
            }
        }
        assertEquals(persistentSetOf<String>(), violations.build())
    }

    @Test
    fun rangesNeverStartOrEndOnAWrapper() {
        val violations = persistentSetOf<String>().builder()
        for (options in combinations) {
            for (input in corpus) {
                for (url in LinksDetektor(input, options).detect()) {
                    if (url.host.orEmpty().startsWith("[")) {
                        continue
                    }
                    val text = input.substring(url.range)
                    // A path, query or fragment may legitimately end with a bracket (Default keeps http://a.com/p)).
                    val hasTail = url.path.orEmpty().length > 1 || url.query.orEmpty().isNotEmpty() ||
                        url.fragment.orEmpty().isNotEmpty()
                    if (text.isEmpty() || text.first() in wrappers || !hasTail && text.last() in wrappers) {
                        violations.add("$input -> text=$text")
                    }
                }
            }
        }
        assertEquals(persistentSetOf<String>(), violations.build())
    }
}
