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
 * RFC 3986 allows `'`, `(` and `)` in userinfo, and the other brackets and quotes do no harm there: a password that
 * holds one still has its scheme, user, password and host.
 */
class UserinfoPasswordTest {

    private val characters = ")]}>\"'<("

    private val masks: ImmutableList<ImmutableSet<LinksDetektorOptions>> = run {
        val options = LinksDetektorOptions.entries.filter { it != LinksDetektorOptions.Default }
        val byMask = (0 until (1 shl options.size)).map { bits ->
            options.filterIndexed { index, _ -> bits and (1 shl index) != 0 }.toImmutableSet()
        }.groupBy { set -> set.fold(0) { mask, option -> mask or option.value } }
        byMask.values.map { sets -> sets.first() }.map { it.ifEmpty { persistentSetOf(LinksDetektorOptions.Default) } }
            .toImmutableList()
    }

    @Test
    fun aClosingCharacterInThePasswordKeepsTheUserinfo() {
        val failures = persistentSetOf<String>().builder()
        for (options in masks) {
            for (character in characters.filterNot { isTracked(options, it) }) {
                val input = "http://u:pa${character}ss@a.com/x"
                val url = LinksDetektor(input, options).detect().firstOrNull()
                val actual = persistentListOf(url?.scheme, url?.username, url?.password, url?.host)
                val expected = persistentListOf<String?>("http", "u", "pa${character}ss", "a.com")
                if (actual != expected) {
                    failures.add("options=$options input=$input actual=$actual")
                }
            }
        }
        assertEquals(persistentSetOf<String>(), failures.build())
    }

    /** Whether an option makes [character] end a URL on purpose, as a closing quote, bracket or tag does. */
    private fun isTracked(options: ImmutableSet<LinksDetektorOptions>, character: Char): Boolean = when (character) {
        '"' -> options.any { it.value and LinksDetektorOptions.QUOTE_MATCH.value != 0 }
        '\'' -> options.any { it.value and LinksDetektorOptions.SINGLE_QUOTE_MATCH.value != 0 }
        '(', ')', '[', ']', '{', '}' -> options.any { it.value and LinksDetektorOptions.BRACKET_MATCH.value != 0 }
        '<', '>' -> options.any { it.value and LinksDetektorOptions.XML.value != 0 }
        else -> false
    }

    @Test
    fun theSchemeLessAndFtpFormsKeepThePassword() {
        for (options in masks) {
            if (!isTracked(options, ')')) {
                val ftp = LinksDetektor("ftp://u:pa)ss@h.com", options).detect().firstOrNull()
                assertEquals("pa)ss", ftp?.password, "options=$options")
                assertEquals(21, ftp?.port, "options=$options")
            }
            if (!isTracked(options, '\'')) {
                val bare = LinksDetektor("user:it's@a.com", options).detect().firstOrNull()
                assertEquals("a.com", bare?.host, "options=$options")
            }
        }
    }
}
