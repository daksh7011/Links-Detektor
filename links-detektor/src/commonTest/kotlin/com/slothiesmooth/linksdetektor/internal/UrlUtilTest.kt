package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.UrlUtil.decode
import com.slothiesmooth.linksdetektor.internal.UrlUtil.encode
import com.slothiesmooth.linksdetektor.internal.UrlUtil.removeExtraDots
import com.slothiesmooth.linksdetektor.internal.UrlUtil.removeSpecialSpaces
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [UrlUtil] utility functions.
 */
class UrlUtilTest {

    //region decode tests
    @Test
    fun `decode handles null input`() {
        val result = decode(null)
        assertEquals("", result)
    }

    @Test
    fun `decode handles empty string`() {
        val result = decode("")
        assertEquals("", result)
    }

    @Test
    fun `decode returns original string when no percent encoding is present`() {
        val input = "example.com"
        val result = decode(input)
        assertEquals(input, result)
    }

    @Test
    fun `decode handles common percent-encoded characters`() {
        persistentListOf(
            "%20" to " ",
            "%2F" to "/",
            "%3A" to ":",
            "%3F" to "?",
            "%3D" to "=",
            "%26" to "&",
            "%25" to "%",
            "%2B" to "+",
            "%23" to "#",
            "%40" to "@",
            "%21" to "!",
            "%24" to "\$",
            "%28" to "(",
            "%29" to ")",
            "%2C" to ",",
            "%3B" to ";",
            "%5B" to "[",
            "%5D" to "]",
            "%7B" to "{",
            "%7D" to "}"
        ).forEach { (input, expected) ->
            assertEquals(expected, decode(input), "case: $input")
        }
    }

    @Test
    fun `decode handles multiple percent-encoded characters`() {
        val input = "example%20domain%2Ecom%2Fpath%3Fquery%3Dvalue"
        val result = decode(input)
        assertEquals("example domain.com/path?query=value", result)
    }

    @Test
    fun `decode handles nested percent encoding`() {
        val input = "example%252Ecom"  // %25 decodes to %, then %2E decodes to .
        val result = decode(input)
        assertEquals("example.com", result)
    }

    @Test
    fun `decode handles deeply nested percent encoding`() {
        val input = "%25252525252525252E" // Multiple levels of % encoding
        val result = decode(input)
        assertEquals(".", result)
    }

    @Test
    fun `decode handles the example from documentation`() {
        val input = "%2525252525252525" // Multiple levels of % encoding
        val result = decode(input)
        assertEquals("%", result)
    }

    @Test
    fun `decode handles incomplete percent encoding`() {
        val input = "example%2"
        val result = decode(input)
        assertEquals("example%2", result)
    }

    @Test
    fun `decode handles invalid hex in percent encoding`() {
        val input = "example%2G"
        val result = decode(input)
        assertEquals("example%2G", result)
    }

    @Test
    fun `decode handles percent encoding at the beginning`() {
        val input = "%20example"
        val result = decode(input)
        assertEquals(" example", result)
    }

    @Test
    fun `decode handles percent encoding at the end`() {
        val input = "example%20"
        val result = decode(input)
        assertEquals("example ", result)
    }

    @Test
    fun `decode handles consecutive percent encodings`() {
        val input = "%20%20%20"
        val result = decode(input)
        assertEquals("   ", result)
    }

    @Test
    fun `decode handles mixed case in percent encoding`() {
        val input = "example%2e%2E%2f%2F"
        val result = decode(input)
        assertEquals("example..//", result)
    }

    @Test
    fun `decode handles special case with percent at end of string`() {
        val input = "example%"
        val result = decode(input)
        assertEquals("example%", result)
    }

    @Test
    fun `decode handles special case with percent and one hex digit at end of string`() {
        val input = "example%2"
        val result = decode(input)
        assertEquals("example%2", result)
    }

    @Test
    fun `decode handles complex backtracking case`() {
        // This tests the backtracking logic when a decoded character is hex and follows another hex character
        val input = "%253F" // %25 -> %, then %3F -> ?
        val result = decode(input)
        assertEquals("?", result)
    }

    @Test
    fun `decode handles special case with percent at end of string after decoding`() {
        val input = "example%25"
        val result = decode(input)
        assertEquals("example%", result)
    }
    //endregion

    //region removeSpecialSpaces tests
    @Test
    fun `removeSpecialSpaces handles null input`() {
        val result = removeSpecialSpaces(null)
        assertEquals("", result)
    }

    @Test
    fun `removeSpecialSpaces handles empty and whitespace-only strings`() {
        persistentListOf(null, "", " ", "\t", "\r", "\n", " \t\r\n").forEach { input ->
            assertEquals("", removeSpecialSpaces(input), "case: $input")
        }
    }

    @Test
    fun `removeSpecialSpaces returns original string when no special spaces are present`() {
        val input = "example.com"
        val result = removeSpecialSpaces(input)
        assertEquals(input, result)
    }

    @Test
    fun `removeSpecialSpaces removes tab characters`() {
        val input = "example\tcom"
        val result = removeSpecialSpaces(input)
        assertEquals("examplecom", result)
    }

    @Test
    fun `removeSpecialSpaces removes carriage return characters`() {
        val input = "example\rcom"
        val result = removeSpecialSpaces(input)
        assertEquals("examplecom", result)
    }

    @Test
    fun `removeSpecialSpaces removes line feed characters`() {
        val input = "example\ncom"
        val result = removeSpecialSpaces(input)
        assertEquals("examplecom", result)
    }

    @Test
    fun `removeSpecialSpaces removes regular space characters`() {
        val input = "example com"
        val result = removeSpecialSpaces(input)
        assertEquals("examplecom", result)
    }

    @Test
    fun `removeSpecialSpaces removes multiple types of whitespace`() {
        val input = "example\t \r\ncom"
        val result = removeSpecialSpaces(input)
        assertEquals("examplecom", result)
    }

    @Test
    fun `removeSpecialSpaces removes whitespace at the beginning`() {
        val input = " \t\r\nexample.com"
        val result = removeSpecialSpaces(input)
        assertEquals("example.com", result)
    }

    @Test
    fun `removeSpecialSpaces removes whitespace at the end`() {
        val input = "example.com \t\r\n"
        val result = removeSpecialSpaces(input)
        assertEquals("example.com", result)
    }

    @Test
    fun `removeSpecialSpaces handles whitespace scattered throughout the string`() {
        val input = "e x a m p\tl\re\n.c o m"
        val result = removeSpecialSpaces(input)
        assertEquals("example.com", result)
    }
    //endregion

    //region encode tests
    @Test
    fun `encode handles null input`() {
        val result = encode(null)
        assertEquals("", result)
    }

    @Test
    fun `encode handles empty string`() {
        persistentListOf(null, "").forEach { input ->
            assertEquals("", encode(input), "case: $input")
        }
    }

    @Test
    fun `encode preserves alphanumeric and unreserved characters`() {
        persistentListOf("example", "com", "domain", "path", "query", "fragment").forEach { input ->
            assertEquals(input, encode(input), "case: $input")
        }
    }

    @Test
    fun `encode encodes space character`() {
        val input = "example domain"
        val result = encode(input)
        assertEquals("example%20domain", result)
    }

    @Test
    fun `encode encodes percent character`() {
        val input = "example%domain"
        val result = encode(input)
        assertEquals("example%25domain", result)
    }

    @Test
    fun `encode encodes hash character`() {
        val input = "example#domain"
        val result = encode(input)
        assertEquals("example%23domain", result)
    }

    @Test
    fun `encode encodes control characters`() {
        val input = "example\u0001domain"
        val result = encode(input)
        assertEquals("example%01domain", result)
    }

    @Test
    fun `encode encodes characters above ASCII 127`() {
        val input = "exampleñdomain"
        val result = encode(input)
        assertEquals("example%F1domain", result)
    }

    @Test
    fun `encode handles string with multiple characters to encode`() {
        val input = "example domain#with%special chars"
        val result = encode(input)
        assertEquals("example%20domain%23with%25special%20chars", result)
    }

    @Test
    fun `encode handles string with only characters to encode`() {
        val input = " #%"
        val result = encode(input)
        assertEquals("%20%23%25", result)
    }

    @Test
    fun `encode handles characters at the beginning and end`() {
        val input = " example#"
        val result = encode(input)
        assertEquals("%20example%23", result)
    }

    @Test
    fun `encode handles all ASCII control characters`() {
        val controlChars = StringBuilder()
        for (i in 0..31) {
            controlChars.append(i.toChar())
        }
        val result = encode(controlChars.toString())
        val expected = StringBuilder()
        for (i in 0..31) {
            expected.append("%" + i.toString(16).uppercase().padStart(2, '0'))
        }
        assertEquals(expected.toString(), result)
    }

    @Test
    fun `encode handles all ASCII non-alphanumeric characters`() {
        val input = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
        val result = encode(input)
        // Only encode #, %, and characters <= 32 or >= 127
        assertEquals("!\"%23$%25&'()*+,-./:;<=>?@[\\]^_`{|}~", result)
    }
    //endregion

    //region removeExtraDots tests
    @Test
    fun `removeExtraDots handles null input`() {
        val result = removeExtraDots(null)
        assertEquals("", result)
    }

    @Test
    fun `removeExtraDots handles empty and dot-only strings`() {
        persistentListOf(null, "", "...", "..", ".", "....", ".....").forEach { input ->
            assertEquals("", removeExtraDots(input), "case: $input")
        }
    }

    @Test
    fun `removeExtraDots returns original string when no extra dots are present`() {
        val input = "example.com"
        val result = removeExtraDots(input)
        assertEquals(input, result)
    }

    @Test
    fun `removeExtraDots handles various dot patterns`() {
        persistentListOf(
            ".example.com" to "example.com",
            "example.com." to "example.com",
            ".example.com." to "example.com",
            "example..com" to "example.com",
            "example.....com" to "example.com",
            ".local.....com." to "local.com",
            "..example...domain....com..." to "example.domain.com",
            ".a.b.c.d.e." to "a.b.c.d.e",
            "...sub...example....co....uk..." to "sub.example.co.uk"
        ).forEach { (input, expected) ->
            assertEquals(expected, removeExtraDots(input), "case: $input")
        }
    }

    @Test
    fun `removeExtraDots handles very long strings`() {
        val longString = "." + "a".repeat(1000) + "...." + "b".repeat(1000) + "."
        val result = removeExtraDots(longString)
        assertEquals("a".repeat(1000) + "." + "b".repeat(1000), result)
    }
    //endregion
}
