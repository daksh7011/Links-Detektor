package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.CharExtensions.isAlpha
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isAlphaNumeric
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isDot
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isHex
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isNumeric
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isUnreserved
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isWhiteSpace
import com.slothiesmooth.linksdetektor.internal.CharExtensions.splitByDot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [CharExtensions] utility functions.
 */
class CharExtensionsTest {

    //region isHex tests
    @Test
    fun `isHex returns true for valid hexadecimal characters`() {
        persistentListOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F').forEach { char ->
            assertTrue(char.isHex(), "case: $char")
        }
    }

    @Test
    fun `isHex returns false for invalid hexadecimal characters`() {
        persistentListOf('g', 'G', 'z', 'Z', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/', '?', ' ').forEach { char ->
            assertFalse(char.isHex(), "case: $char")
        }
    }

    @Test
    fun `isHex handles non-ASCII characters`() {
        assertFalse('\u00F6'.isHex()) // ö
        assertFalse('\u20AC'.isHex()) // €
    }
    //endregion

    //region isAlpha tests
    @Test
    fun `isAlpha returns true for alphabetic characters`() {
        persistentListOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z').forEach { char ->
            assertTrue(char.isAlpha(), "case: $char")
        }
    }

    @Test
    fun `isAlpha returns false for non-alphabetic characters`() {
        persistentListOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/', '?', ' ').forEach { char ->
            assertFalse(char.isAlpha(), "case: $char")
        }
    }

    @Test
    fun `isAlpha returns false for accented characters`() {
        persistentListOf('\u00E0', '\u00E1', '\u00E2', '\u00E3', '\u00E4', '\u00E5', '\u00C0', '\u00C1', '\u00C2', '\u00C3', '\u00C4', '\u00C5').forEach { char ->
            assertFalse(char.isAlpha(), "case: $char")
        }
    }
    //endregion

    //region isNumeric tests
    @Test
    fun `isNumeric returns true for numeric characters`() {
        persistentListOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9').forEach { char ->
            assertTrue(char.isNumeric(), "case: $char")
        }
    }

    @Test
    fun `isNumeric returns false for non-numeric characters`() {
        persistentListOf('a', 'z', 'A', 'Z', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/', '?', ' ').forEach { char ->
            assertFalse(char.isNumeric(), "case: $char")
        }
    }

    @Test
    fun `isNumeric returns false for fraction characters`() {
        // ½, ¼, ¾
        persistentListOf('\u00BD', '\u00BC', '\u00BE').forEach { char ->
            assertFalse(char.isNumeric(), "case: $char")
        }
    }
    //endregion

    //region isAlphaNumeric tests
    @Test
    fun `isAlphaNumeric returns true for alphanumeric characters`() {
        persistentListOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9').forEach { char ->
            assertTrue(char.isAlphaNumeric(), "case: $char")
        }
    }

    @Test
    fun `isAlphaNumeric returns false for non-alphanumeric characters`() {
        persistentListOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/', '?', ' ').forEach { char ->
            assertFalse(char.isAlphaNumeric(), "case: $char")
        }
    }

    @Test
    fun `isAlphaNumeric returns false for Unicode characters that look like alphanumeric characters`() {
        persistentListOf('\u2113', '\u2118', '\u2126', '\u212A', '\u212B', '\u212E').forEach { char ->
            assertFalse(char.isAlphaNumeric(), "case: $char")
        }
    }

    @Test
    fun `isAlphaNumeric returns false for Roman numeral characters`() {
        // Roman numerals I, II, III, IV
        persistentListOf('\u2160', '\u2161', '\u2162', '\u2163').forEach { char ->
            assertFalse(char.isAlphaNumeric(), "case: $char")
        }
    }
    //endregion

    //region isUnreserved tests
    @Test
    fun `isUnreserved returns true for unreserved characters`() {
        persistentListOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '_', '~').forEach { char ->
            assertTrue(char.isUnreserved(), "case: $char")
        }
    }

    @Test
    fun `isUnreserved returns false for reserved characters`() {
        persistentListOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '/', '?', ' ').forEach { char ->
            assertFalse(char.isUnreserved(), "case: $char")
        }
    }

    @Test
    fun `isUnreserved returns false for Unicode characters that look like hyphens`() {
        persistentListOf('\u2212', '\u2013', '\u2014', '\u2015', '\uFE63', '\uFF0D').forEach { char ->
            assertFalse(char.isUnreserved(), "case: $char")
        }
    }

    @Test
    fun `isUnreserved returns false for Unicode characters that look like underscores`() {
        persistentListOf('\u2040', '\u203F', '\uFE33', '\uFE34', '\uFE4D', '\uFE4E', '\uFE4F', '\uFF3F').forEach { char ->
            assertFalse(char.isUnreserved(), "case: $char")
        }
    }

    @Test
    fun `isUnreserved returns false for Unicode characters that look like tildes`() {
        persistentListOf('\u02DC', '\u2053', '\u223C', '\u301C', '\uFF5E').forEach { char ->
            assertFalse(char.isUnreserved(), "case: $char")
        }
    }
    //endregion

    //region isDot tests
    @Test
    fun `isDot returns true for dot characters`() {
        persistentListOf('.', '\u3002', '\uFF0E', '\uFF61').forEach { char ->
            assertTrue(char.isDot(), "case: $char")
        }
    }

    @Test
    fun `isDot returns false for non-dot characters`() {
        persistentListOf('a', 'z', 'A', 'Z', '0', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '/', '?', ' ').forEach { char ->
            assertFalse(char.isDot(), "case: $char")
        }
    }

    @Test
    fun `isDot returns false for characters that might be confused with dots`() {
        persistentListOf(',', ';', ':', '·', '•').forEach { char ->
            assertFalse(char.isDot(), "case: $char")
        }
    }

    @Test
    fun `isDot returns false for bullet and dot-like characters`() {
        // Various bullet characters
        persistentListOf('\u2022', '\u2023', '\u25E6', '\u2043', '\u2219').forEach { char ->
            assertFalse(char.isDot(), "case: $char")
        }
    }
    //endregion

    //region isWhiteSpace tests
    @Test
    fun `isWhiteSpace returns true for whitespace characters`() {
        persistentListOf('\n', '\t', '\r', ' ').forEach { char ->
            assertTrue(char.isWhiteSpace(), "case: $char")
        }
    }

    @Test
    fun `isWhiteSpace returns false for non-whitespace characters`() {
        persistentListOf('a', 'z', 'A', 'Z', '0', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/').forEach { char ->
            assertFalse(char.isWhiteSpace(), "case: $char")
        }
    }

    @Test
    fun `isWhiteSpace returns false for other Unicode whitespace characters`() {
        persistentListOf('\u00A0', '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2007', '\u2008', '\u2009', '\u200A', '\u202F', '\u205F', '\u3000').forEach { char ->
            assertFalse(char.isWhiteSpace(), "case: $char")
        }
    }

    @Test
    fun `isWhiteSpace returns false for control characters`() {
        persistentListOf('\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007', '\u0008', '\u000B', '\u000C', '\u000E', '\u000F').forEach { char ->
            assertFalse(char.isWhiteSpace(), "case: $char")
        }
    }

    @Test
    fun `isWhiteSpace returns false for zero-width spaces`() {
        // Zero-width spaces and joiners
        persistentListOf('\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF').forEach { char ->
            assertFalse(char.isWhiteSpace(), "case: $char")
        }
    }
    //endregion

    //region splitByDot tests
    @Test
    fun `splitByDot returns an immutable list`() {
        val parts: ImmutableList<String> = "a.b".splitByDot()
        assertEquals(persistentListOf("a", "b"), parts)
    }

    @Test
    fun `splitByDot handles empty string`() {
        val result = "".splitByDot()
        assertEquals(persistentListOf<String>(), result)
    }

    @Test
    fun `splitByDot splits by standard dots`() {
        val result = "example.com".splitByDot()
        assertEquals(persistentListOf("example", "com"), result)
    }

    @Test
    fun `splitByDot splits by URL-encoded dots`() {
        val result = "example%2Ecom".splitByDot()
        assertEquals(persistentListOf("example", "com"), result)
    }

    @Test
    fun `splitByDot splits by mixed standard and URL-encoded dots`() {
        val result = "sub.example%2Ecom".splitByDot()
        assertEquals(persistentListOf("sub", "example", "com"), result)
    }

    @Test
    fun `splitByDot splits by Unicode dot equivalents`() {
        val result = "example\u3002com".splitByDot()
        assertEquals(persistentListOf("example", "com"), result)
    }

    @Test
    fun `splitByDot handles consecutive dots`() {
        val result = "example..com".splitByDot()
        assertEquals(persistentListOf("example", "", "com"), result)
    }

    @Test
    fun `splitByDot handles dots at the beginning and end`() {
        val result = ".example.com.".splitByDot()
        assertEquals(persistentListOf("", "example", "com", ""), result)
    }

    @Test
    fun `splitByDot handles complex cases with mixed dot types`() {
        val result = "sub%2Eexample\u3002co\uFF0Em\uFF61org".splitByDot()
        assertEquals(persistentListOf("sub", "example", "co", "m", "org"), result)
    }

    @Test
    fun `splitByDot handles null input`() {
        val nullString: String? = null
        val result = (nullString ?: "").splitByDot()
        assertEquals(persistentListOf<String>(), result)
    }

    @Test
    fun `splitByDot handles URL-encoded dots at beginning and end`() {
        val result = "%2Eexample.com%2E".splitByDot()
        assertEquals(persistentListOf("", "example", "com", ""), result)
    }

    @Test
    fun `splitByDot handles lowercase and uppercase URL-encoded dots`() {
        val resultLower = "example%2ecom".splitByDot()
        val resultUpper = "example%2Ecom".splitByDot()
        assertEquals(persistentListOf("example", "com"), resultLower)
        assertEquals(persistentListOf("example", "com"), resultUpper)
    }

    @Test
    fun `splitByDot handles incomplete URL-encoded dots`() {
        val result = "example%2".splitByDot()
        assertEquals(persistentListOf("example%2"), result)
    }

    @Test
    fun `splitByDot handles multiple consecutive URL-encoded dots`() {
        val result = "example%2E%2Ecom".splitByDot()
        assertEquals(persistentListOf("example", "", "com"), result)
    }

    @Test
    fun `splitByDot handles very long strings`() {
        val longString = "a".repeat(1000) + "." + "b".repeat(1000) + ".c"
        val result = longString.splitByDot()
        assertEquals(persistentListOf("a".repeat(1000), "b".repeat(1000), "c"), result)
    }

    @Test
    fun `splitByDot handles mixed URL-encoded and Unicode dots`() {
        val result = "example%2E\u3002com\uFF0Eorg%2eio".splitByDot()
        assertEquals(persistentListOf("example", "", "com", "org", "io"), result)
    }

    @Test
    fun `splitByDot handles URL-encoded dots with mixed case combinations`() {
        val result = "example%2E%2ecom%2E%2Eorg".splitByDot()
        assertEquals(persistentListOf("example", "", "com", "", "org"), result)
    }

    @Test
    fun `splitByDot handles malformed URL-encoded dots`() {
        val result = "example%2com%2".splitByDot()
        assertEquals(persistentListOf("example%2com%2"), result)
    }

    @Test
    fun `splitByDot handles string with only dots`() {
        val result = "...".splitByDot()
        assertEquals(persistentListOf("", "", "", ""), result)
    }

    @Test
    fun `splitByDot handles string with only URL-encoded dots`() {
        val result = "%2E%2e%2E".splitByDot()
        assertEquals(persistentListOf("", "", "", ""), result)
    }

    @Test
    fun `splitByDot handles string with partial URL-encoded sequence at the end`() {
        val result = "example%".splitByDot()
        assertEquals(persistentListOf("example%"), result)
    }

    @Test
    fun `splitByDot handles mix of URL-encoded dots and non-dot characters`() {
        val result = "a%2Eb%2Ec%2Ed".splitByDot()
        assertEquals(persistentListOf("a", "b", "c", "d"), result)
    }

    @Test
    fun `splitByDot handles URL-encoded dot followed by percent sign`() {
        val result = "example%2E%com".splitByDot()
        assertEquals(persistentListOf("example", "%com"), result)
    }

    @Test
    fun `splitByDot handles string with URL-encoded dot followed by another percent sign and valid hex`() {
        val result = "example%2E%20rest".splitByDot()
        assertEquals(persistentListOf("example", "%20rest"), result)
    }
    //endregion
}
