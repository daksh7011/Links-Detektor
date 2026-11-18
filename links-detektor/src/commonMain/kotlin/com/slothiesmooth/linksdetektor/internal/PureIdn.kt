package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Pure-Kotlin host name to ASCII conversion (IDNA 2003 style), for targets without `java.net.IDN`.
 *
 * Documented divergence from `java.net.IDN`: the Nameprep prohibited-character, bidirectional and unassigned
 * code point checks are not performed, so this accepts some hosts that `java.net.IDN` rejects (for example
 * emoji and U+1E9E).
 */
internal object PureIdn {
    private const val MAX_LABEL_LENGTH = 63
    private const val ASCII_LIMIT = 0x80
    private const val ACE_PREFIX = "xn--"
    private const val ROOT = "."

    /** The Nameprep "map to nothing" characters. */
    private fun isMappedToNothing(char: Char): Boolean = when (char.code) {
        0x00AD, 0x034F, 0x1806, 0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF -> true
        in 0x180B..0x180D -> true
        in 0xFE00..0xFE0F -> true
        else -> false
    }

    /**
     * Converts [host] to ASCII, label by label.
     *
     * @param host The host name; labels may be separated by `.`, `U+3002`, `U+FF0E` or `U+FF61`.
     * @return The ASCII host name, with `.` between labels.
     * @throws IllegalArgumentException If a label is empty (other than the last), too long, or starts with `xn--`
     *         after mapping.
     */
    fun toAscii(host: String): String {
        val labels = host.split('.', '\u3002', '\uFF0E', '\uFF61').toImmutableList()
        // A host that is exactly one separator is the root label.
        if (labels.size == 2 && host.length == 1) return ROOT
        val result = persistentListOf<String>().builder()
        labels.forEachIndexed { index, label ->
            require(label.isNotEmpty() || index == labels.lastIndex) { "Empty label is not a legal name" }
            result.add(labelToAscii(label))
        }
        return result.build().joinToString(".")
    }

    private fun labelToAscii(label: String): String {
        if (label.all { it.code < ASCII_LIMIT }) {
            require(label.length <= MAX_LABEL_LENGTH) { "The label in the input is too long" }
            return label
        }
        val mapped = nfkc(
            label.filterNot { isMappedToNothing(it) }
                .lowercase()
                .replace("ß", "ss")
                .replace('ς', 'σ')
        ).lowercase()
        require(mapped.isNotEmpty()) { "Empty label is not a legal name" }
        val ascii = if (mapped.all { it.code < ASCII_LIMIT }) {
            mapped
        } else {
            require(!mapped.startsWith(ACE_PREFIX)) { "The input starts with the ACE Prefix" }
            ACE_PREFIX + Punycode.encode(mapped)
        }
        require(ascii.length <= MAX_LABEL_LENGTH) { "The label in the input is too long" }
        return ascii
    }
}
