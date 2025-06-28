package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.CharExtensions.isHex
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isAlpha
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isNumeric
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isAlphaNumeric
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isUnreserved
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isDot
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isWhiteSpace
import com.slothiesmooth.linksdetektor.internal.CharExtensions.splitByDot
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.apache.commons.lang3.StringUtils

/**
 * Tests for [CharExtensions] utility functions.
 */
class CharExtensionsTest {

    //region isHex tests
    @ParameterizedTest
    @ValueSource(chars = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'])
    fun `isHex returns true for valid hexadecimal characters`(char: Char) {
        assertTrue(char.isHex())
    }

    @ParameterizedTest
    @ValueSource(chars = ['g', 'G', 'z', 'Z', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/', '?', ' '])
    fun `isHex returns false for invalid hexadecimal characters`(char: Char) {
        assertFalse(char.isHex())
    }

    @Test
    fun `isHex handles non-ASCII characters`() {
        assertFalse('\u00F6'.isHex()) // ö
        assertFalse('\u20AC'.isHex()) // €
    }
    //endregion

    //region isAlpha tests
    @ParameterizedTest
    @ValueSource(chars = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'])
    fun `isAlpha returns true for alphabetic characters`(char: Char) {
        assertTrue(char.isAlpha())
    }

    @ParameterizedTest
    @ValueSource(chars = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/', '?', ' '])
    fun `isAlpha returns false for non-alphabetic characters`(char: Char) {
        assertFalse(char.isAlpha())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u00E0', '\u00E1', '\u00E2', '\u00E3', '\u00E4', '\u00E5', '\u00C0', '\u00C1', '\u00C2', '\u00C3', '\u00C4', '\u00C5'])
    fun `isAlpha returns false for accented characters`(char: Char) {
        assertFalse(char.isAlpha())
    }
    //endregion

    //region isNumeric tests
    @ParameterizedTest
    @ValueSource(chars = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'])
    fun `isNumeric returns true for numeric characters`(char: Char) {
        assertTrue(char.isNumeric())
    }

    @ParameterizedTest
    @ValueSource(chars = ['a', 'z', 'A', 'Z', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/', '?', ' '])
    fun `isNumeric returns false for non-numeric characters`(char: Char) {
        assertFalse(char.isNumeric())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u00BD', '\u00BC', '\u00BE']) // ½, ¼, ¾
    fun `isNumeric returns false for fraction characters`(char: Char) {
        assertFalse(char.isNumeric())
    }
    //endregion

    //region isAlphaNumeric tests
    @ParameterizedTest
    @ValueSource(chars = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'])
    fun `isAlphaNumeric returns true for alphanumeric characters`(char: Char) {
        assertTrue(char.isAlphaNumeric())
    }

    @ParameterizedTest
    @ValueSource(chars = ['!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/', '?', ' '])
    fun `isAlphaNumeric returns false for non-alphanumeric characters`(char: Char) {
        assertFalse(char.isAlphaNumeric())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u2113', '\u2118', '\u2126', '\u212A', '\u212B', '\u212E'])
    fun `isAlphaNumeric returns false for Unicode characters that look like alphanumeric characters`(char: Char) {
        assertFalse(char.isAlphaNumeric())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u2160', '\u2161', '\u2162', '\u2163']) // Roman numerals I, II, III, IV
    fun `isAlphaNumeric returns false for Roman numeral characters`(char: Char) {
        assertFalse(char.isAlphaNumeric())
    }
    //endregion

    //region isUnreserved tests
    @ParameterizedTest
    @ValueSource(chars = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '_', '~'])
    fun `isUnreserved returns true for unreserved characters`(char: Char) {
        assertTrue(char.isUnreserved())
    }

    @ParameterizedTest
    @ValueSource(chars = ['!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '/', '?', ' '])
    fun `isUnreserved returns false for reserved characters`(char: Char) {
        assertFalse(char.isUnreserved())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u2212', '\u2013', '\u2014', '\u2015', '\uFE63', '\uFF0D'])
    fun `isUnreserved returns false for Unicode characters that look like hyphens`(char: Char) {
        assertFalse(char.isUnreserved())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u2040', '\u203F', '\uFE33', '\uFE34', '\uFE4D', '\uFE4E', '\uFE4F', '\uFF3F'])
    fun `isUnreserved returns false for Unicode characters that look like underscores`(char: Char) {
        assertFalse(char.isUnreserved())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u02DC', '\u2053', '\u223C', '\u301C', '\uFF5E'])
    fun `isUnreserved returns false for Unicode characters that look like tildes`(char: Char) {
        assertFalse(char.isUnreserved())
    }
    //endregion

    //region isDot tests
    @ParameterizedTest
    @ValueSource(chars = ['.', '\u3002', '\uFF0E', '\uFF61'])
    fun `isDot returns true for dot characters`(char: Char) {
        assertTrue(char.isDot())
    }

    @ParameterizedTest
    @ValueSource(chars = ['a', 'z', 'A', 'Z', '0', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '/', '?', ' '])
    fun `isDot returns false for non-dot characters`(char: Char) {
        assertFalse(char.isDot())
    }

    @ParameterizedTest
    @ValueSource(chars = [',', ';', ':', '·', '•'])
    fun `isDot returns false for characters that might be confused with dots`(char: Char) {
        assertFalse(char.isDot())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u2022', '\u2023', '\u25E6', '\u2043', '\u2219']) // Various bullet characters
    fun `isDot returns false for bullet and dot-like characters`(char: Char) {
        assertFalse(char.isDot())
    }
    //endregion

    //region isWhiteSpace tests
    @ParameterizedTest
    @ValueSource(chars = ['\n', '\t', '\r', ' '])
    fun `isWhiteSpace returns true for whitespace characters`(char: Char) {
        assertTrue(char.isWhiteSpace())
    }

    @ParameterizedTest
    @ValueSource(chars = ['a', 'z', 'A', 'Z', '0', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '/'])
    fun `isWhiteSpace returns false for non-whitespace characters`(char: Char) {
        assertFalse(char.isWhiteSpace())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u00A0', '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2007', '\u2008', '\u2009', '\u200A', '\u202F', '\u205F', '\u3000'])
    fun `isWhiteSpace returns false for other Unicode whitespace characters`(char: Char) {
        assertFalse(char.isWhiteSpace())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007', '\u0008', '\u000B', '\u000C', '\u000E', '\u000F'])
    fun `isWhiteSpace returns false for control characters`(char: Char) {
        assertFalse(char.isWhiteSpace())
    }

    @ParameterizedTest
    @ValueSource(chars = ['\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF']) // Zero-width spaces and joiners
    fun `isWhiteSpace returns false for zero-width spaces`(char: Char) {
        assertFalse(char.isWhiteSpace())
    }
    //endregion

    //region splitByDot tests
    @Test
    fun `splitByDot handles empty string`() {
        val result = "".splitByDot()
        assertArrayEquals(arrayOf(""), result)
    }

    @Test
    fun `splitByDot splits by standard dots`() {
        val result = "example.com".splitByDot()
        assertArrayEquals(arrayOf("example", "com"), result)
    }

    @Test
    fun `splitByDot splits by URL-encoded dots`() {
        val result = "example%2Ecom".splitByDot()
        assertArrayEquals(arrayOf("example", "com"), result)
    }

    @Test
    fun `splitByDot splits by mixed standard and URL-encoded dots`() {
        val result = "sub.example%2Ecom".splitByDot()
        assertArrayEquals(arrayOf("sub", "example", "com"), result)
    }

    @Test
    fun `splitByDot splits by Unicode dot equivalents`() {
        val result = "example\u3002com".splitByDot()
        assertArrayEquals(arrayOf("example", "com"), result)
    }

    @Test
    fun `splitByDot handles consecutive dots`() {
        val result = "example..com".splitByDot()
        assertArrayEquals(arrayOf("example", "", "com"), result)
    }

    @Test
    fun `splitByDot handles dots at the beginning and end`() {
        val result = ".example.com.".splitByDot()
        assertArrayEquals(arrayOf("", "example", "com", ""), result)
    }

    @Test
    fun `splitByDot handles complex cases with mixed dot types`() {
        val result = "sub%2Eexample\u3002co\uFF0Em\uFF61org".splitByDot()
        assertArrayEquals(arrayOf("sub", "example", "co", "m", "org"), result)
    }

    @Test
    fun `splitByDot handles null input`() {
        val nullString: String? = null
        val result = (nullString ?: "").splitByDot()
        assertArrayEquals(arrayOf(""), result)
    }

    @Test
    fun `splitByDot handles URL-encoded dots at beginning and end`() {
        val result = "%2Eexample.com%2E".splitByDot()
        assertArrayEquals(arrayOf("", "example", "com", ""), result)
    }

    @Test
    fun `splitByDot handles lowercase and uppercase URL-encoded dots`() {
        val resultLower = "example%2ecom".splitByDot()
        val resultUpper = "example%2Ecom".splitByDot()
        assertArrayEquals(arrayOf("example", "com"), resultLower)
        assertArrayEquals(arrayOf("example", "com"), resultUpper)
    }

    @Test
    fun `splitByDot handles incomplete URL-encoded dots`() {
        val result = "example%2".splitByDot()
        assertArrayEquals(arrayOf("example%2"), result)
    }

    @Test
    fun `splitByDot handles multiple consecutive URL-encoded dots`() {
        val result = "example%2E%2Ecom".splitByDot()
        assertArrayEquals(arrayOf("example", "", "com"), result)
    }

    @Test
    fun `splitByDot handles very long strings`() {
        val longString = "a".repeat(1000) + "." + "b".repeat(1000) + ".c"
        val result = longString.splitByDot()
        assertArrayEquals(arrayOf("a".repeat(1000), "b".repeat(1000), "c"), result)
    }

    @Test
    fun `splitByDot handles mixed URL-encoded and Unicode dots`() {
        val result = "example%2E\u3002com\uFF0Eorg%2eio".splitByDot()
        assertArrayEquals(arrayOf("example", "", "com", "org", "io"), result)
    }

    @Test
    fun `splitByDot handles URL-encoded dots with mixed case combinations`() {
        val result = "example%2E%2ecom%2E%2Eorg".splitByDot()
        assertArrayEquals(arrayOf("example", "", "com", "", "org"), result)
    }

    @Test
    fun `splitByDot handles malformed URL-encoded dots`() {
        val result = "example%2com%2".splitByDot()
        assertArrayEquals(arrayOf("example%2com%2"), result)
    }

    @Test
    fun `splitByDot handles string with only dots`() {
        val result = "...".splitByDot()
        assertArrayEquals(arrayOf("", "", "", ""), result)
    }

    @Test
    fun `splitByDot handles string with only URL-encoded dots`() {
        val result = "%2E%2e%2E".splitByDot()
        assertArrayEquals(arrayOf("", "", "", ""), result)
    }

    @Test
    fun `splitByDot handles string with partial URL-encoded sequence at the end`() {
        val result = "example%".splitByDot()
        assertArrayEquals(arrayOf("example%"), result)
    }

    @Test
    fun `splitByDot handles mix of URL-encoded dots and non-dot characters`() {
        val result = "a%2Eb%2Ec%2Ed".splitByDot()
        assertArrayEquals(arrayOf("a", "b", "c", "d"), result)
    }

    @Test
    fun `splitByDot handles URL-encoded dot followed by percent sign`() {
        val result = "example%2E%com".splitByDot()
        assertArrayEquals(arrayOf("example", "%com"), result)
    }

    @Test
    fun `splitByDot handles string with URL-encoded dot followed by another percent sign and valid hex`() {
        val result = "example%2E%20rest".splitByDot()
        assertArrayEquals(arrayOf("example", "%20rest"), result)
    }
    //endregion
}
