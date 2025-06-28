package com.slothiesmooth.linksdetektor.internal

import org.apache.commons.lang3.StringUtils

/**
 * Provides extension functions for character and string operations used in URL detection.
 *
 * This object contains utility extension functions for character validation, classification,
 * and string manipulation specifically tailored for URL parsing and normalization.
 */
internal object CharExtensions {
    /**
     * Determines if a character is a valid hexadecimal digit.
     *
     * Valid hexadecimal digits include 0-9, a-f, and A-F.
     *
     * @return `true` if the character is a valid hexadecimal digit, `false` otherwise.
     */
    internal fun Char.isHex(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }

    /**
     * Determines if a character is a valid alphabetic character.
     *
     * Valid alphabetic characters include a-z and A-Z.
     *
     * @return `true` if the character is a valid alphabetic character, `false` otherwise.
     */
    internal fun Char.isAlpha(): Boolean {
        return this in 'a'..'z' || this in 'A'..'Z'
    }

    /**
     * Determines if a character is a valid numeric digit.
     *
     * Valid numeric digits include 0-9.
     *
     * @return `true` if the character is a valid numeric digit, `false` otherwise.
     */
    internal fun Char.isNumeric(): Boolean {
        return this in '0'..'9'
    }

    /**
     * Determines if a character is a valid alphanumeric character.
     *
     * Valid alphanumeric characters include a-z, A-Z, and 0-9.
     *
     * @return `true` if the character is a valid alphanumeric character, `false` otherwise.
     */
    internal fun Char.isAlphaNumeric(): Boolean {
        return this.isAlpha() || this.isNumeric()
    }

    /**
     * Determines if a character is a valid unreserved character as defined by RFC 3986.
     *
     * According to RFC 3986, unreserved characters include:
     * - Alphanumeric characters (a-z, A-Z, 0-9)
     * - Hyphen (-)
     * - Period (.)
     * - Underscore (_)
     * - Tilde (~)
     *
     * @return `true` if the character is a valid unreserved character, `false` otherwise.
     * @see <a href="https://tools.ietf.org/html/rfc3986#section-2.3">RFC 3986 Section 2.3</a>
     */
    internal fun Char.isUnreserved(): Boolean {
        return this.isAlphaNumeric() || this == '-' || this == '.' || this == '_' || this == '~'
    }

    /**
     * Determines if a character is a dot or a Unicode equivalent of a dot.
     *
     * This method recognizes the following characters as dots:
     * - ASCII period (.)
     * - Ideographic full stop (。) - U+3002
     * - Fullwidth full stop (．) - U+FF0E
     * - Halfwidth ideographic full stop (｡) - U+FF61
     *
     * @return `true` if the character is a dot or a Unicode equivalent, `false` otherwise.
     * @see <a href="https://www.unicode.org/reports/tr46/">Unicode IDNA Compatibility Processing</a>
     */
    internal fun Char.isDot(): Boolean {
        return this == '.' || this == '\u3002' || this == '\uFF0E' || this == '\uFF61'
    }

    /**
     * Determines if a character is a whitespace character.
     *
     * This method recognizes the following characters as whitespace:
     * - Line feed (\n)
     * - Tab (\t)
     * - Carriage return (\r)
     * - Space ( )
     *
     * @return `true` if the character is a whitespace character, `false` otherwise.
     */
    internal fun Char.isWhiteSpace(): Boolean {
        return this == '\n' || this == '\t' || this == '\r' || this == ' '
    }

    /**
     * Splits a string by dot characters, including both standard and URL-encoded dots.
     *
     * This method provides a more robust alternative to standard string splitting by:
     * 1. Recognizing both standard ASCII dots (.) and Unicode equivalents via [isDot]
     * 2. Handling URL-encoded dots (%2E or %2e)
     * 3. Avoiding regex-based splitting for better performance
     *
     * For example, the string "example.com" or "example%2ecom" would both be split into
     * ["example", "com"].
     *
     * @return An array of strings resulting from splitting the original string at all dot characters.
     *         Returns an array containing an empty string if the input is empty.
     */
    internal fun String.splitByDot(): Array<String> {
        val splitList = ArrayList<String>()
        val section = StringBuilder()
        if (StringUtils.isEmpty(this)) {
            return arrayOf("")
        }
        val inputTextReader = InputTextReader(this)
        while (inputTextReader.eof().not()) {
            val currentCharacter: Char = inputTextReader.read()
            when {
                currentCharacter.isDot() -> {
                    splitList.add(section.toString())
                    section.setLength(0)
                }
                currentCharacter == '%' &&
                        inputTextReader.canReadChars(2) &&
                        inputTextReader.peek(2).equals("2e", ignoreCase = true) ->
                {
                    inputTextReader.read()
                    inputTextReader.read() // advance past the 2e
                    splitList.add(section.toString())
                    section.setLength(0)
                }
                else -> {
                    section.append(currentCharacter)
                }
            }
        }
        splitList.add(section.toString())
        return splitList.toTypedArray()
    }
}
