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
 * Whatever the options, brackets, quotes, colons and slashes around a URL never end up in its host.
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
        // R1: userinfo with closers in the password
        "http://u:pa)ss@a.com", "http://u:it's@a.com", "user:it's@a.com", "ftp://u:pa)ss@h.com",
        // R2: other schemes in a wrapper
        "(git://github.com/x)", "(ws://a.com)", "'ws://a.com'", "\"file://a.com\"", "(s3://bucket/key)",
        "(jdbc://db:5432/x)", "(ssh://u@h)", "ws://a.com",
        // R3: hosts that would end with a colon
        "docs:/x y", "see C:/tmp now", "localhost:/x y", "C:/tmp", "hi:/a", "localhost:/x", "(docs:/x)", "[docs:/x]",
        "\"key:/v\"", "(http:/a.com)",
        // R4: a word and a colon in front of a wrapper
        "{url:\"http://svc\"}", "key:\"x", "key:\"x\" y",
        // R5: hosts with a wrapper glued in front
        "<p>a.com</p>", "<b>a.com</b>", "x(a.com)", "<x>evil.org#@good.com</x>", "<p>'www.a.com'",
        "\"www.evil.org\uFF1Fa=b",
    )

    /** One option set for each distinct bit mask that the subsets of the eight non-default options produce: 28. */
    private val masks: ImmutableList<ImmutableSet<LinksDetektorOptions>> = run {
        val options = LinksDetektorOptions.entries.filter { it != LinksDetektorOptions.Default }
        val byMask = (0 until (1 shl options.size)).map { bits ->
            options.filterIndexed { index, _ -> bits and (1 shl index) != 0 }.toImmutableSet()
        }.groupBy { set -> set.fold(0) { mask, option -> mask or option.value } }
        byMask.values.map { sets -> sets.first() }.map { it.ifEmpty { persistentSetOf(LinksDetektorOptions.Default) } }
            .toImmutableList()
    }

    @Test
    fun theSubsetsOfTheOptionsMakeTwentyEightDistinctMasks() {
        assertEquals(28, masks.size)
    }

    @Test
    fun hostsNeverHoldAWrapperAColonOrASlashOutsideAnIpv6Literal() {
        val violations = persistentSetOf<String>().builder()
        for (options in masks) {
            for (input in corpus) {
                for (url in LinksDetektor(input, options).detect()) {
                    val host = url.host.orEmpty()
                    if (!isIpv6Literal(host) && host.any { it in wrappers || it == ':' || it == '/' }) {
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
        for (options in masks) {
            for (input in corpus) {
                for (url in LinksDetektor(input, options).detect()) {
                    if (isIpv6Literal(url.host.orEmpty())) {
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

    @Test
    fun aWrapperDoesNotHideTheHostThatTheBareUrlHas() {
        val wrapped = persistentListOf(
            "(git://github.com/x)" to "git://github.com/x", "(ws://a.com)" to "ws://a.com",
            "'ws://a.com'" to "ws://a.com", "\"file://a.com\"" to "file://a.com", "(s3://bucket/key)" to "s3://bucket/key",
            "(ssh://u@h)" to "ssh://u@h", "(a.com)" to "a.com", "'a.com'" to "a.com",
        )
        val missing = persistentSetOf<String>().builder()
        for (options in masks) {
            for ((input, bare) in wrapped) {
                val bareHosts = LinksDetektor(bare, options).detect().map { it.host }.toImmutableSet()
                val wrappedHosts = LinksDetektor(input, options).detect().map { it.host }.toImmutableSet()
                if (!wrappedHosts.containsAll(bareHosts)) {
                    missing.add("$input -> $wrappedHosts, bare $bare -> $bareHosts")
                }
            }
        }
        assertEquals(persistentSetOf<String>(), missing.build())
    }

    /** A bracketed IPv6 literal such as `[::1]` or `[fe80::1%25eth0]`, checked rather than assumed. */
    private fun isIpv6Literal(host: String): Boolean =
        host.length > 3 && host.first() == '[' && host.last() == ']' && host.count { it == ':' } >= 2 &&
            host.substring(1, host.length - 1).all { it.isLetterOrDigit() || it == ':' || it == '.' || it == '%' || it == '-' }
}
