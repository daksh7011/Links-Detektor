package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.CharExtensions.isHex
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isWhiteSpace
import java.util.*

/**
 * Utility object for URL manipulation operations.
 *
 * This object provides methods for encoding, decoding, and normalizing URLs.
 */
internal object UrlUtil {
    /**
     * Decodes the url by iteratively removing hex characters with backtracking.
     * For example: %2525252525252525 becomes %
     *
     * @param url The URL string to decode
     * @return The decoded URL string, or empty string if input is null
     */
    internal fun decode(url: String?): String {
        url ?: return ""

        val decodedUrlBuilder = StringBuilder(url)
        val pendingPercentIndices = Stack<Int>()
        var currentPosition = 0
        while (currentPosition < decodedUrlBuilder.length - 2) {
            val currentCharacter = decodedUrlBuilder[currentPosition]
            if (currentCharacter == '%') {
                if (decodedUrlBuilder.getOrNull(currentPosition + 1)?.isHex() == true &&
                    decodedUrlBuilder.getOrNull(currentPosition + 2)?.isHex() == true
                ) {

                    val hexDigits = decodedUrlBuilder.substring(currentPosition + 1, currentPosition + 3)
                    val decodedCharacter = hexDigits.toInt(16).toChar()

                    decodedUrlBuilder.delete(currentPosition, currentPosition + 3) // delete the % and two hex digits
                    decodedUrlBuilder.insert(currentPosition, decodedCharacter) // add decoded character

                    if (decodedCharacter == '%') {
                        currentPosition-- // backtrack one character to check for another decoding with this %.
                    } else if (pendingPercentIndices.isNotEmpty()
                        && decodedCharacter.isHex()
                        && decodedUrlBuilder.getOrNull(currentPosition - 1)?.isHex() == true
                        && currentPosition - pendingPercentIndices.peek() == 2
                    ) {
                        // Go back to the last non-decoded percent sign if it can be decoded.
                        // We only need to go back if it's of form %[HEX][HEX]
                        currentPosition = pendingPercentIndices.pop() - 1 // backtrack to the % sign.
                    } else if (pendingPercentIndices.isNotEmpty() &&
                        currentPosition == decodedUrlBuilder.length - 2
                    ) {
                        // special case to handle %[HEX][Unknown][end of string]
                        currentPosition = pendingPercentIndices.pop() - 1 // backtrack to the % sign.
                    }
                } else {
                    pendingPercentIndices.push(currentPosition)
                }
            }
            currentPosition++
        }
        return decodedUrlBuilder.toString()
    }

    /**
     * Removes TAB (0x09), CR (0x0d), LF (0x0a), and spaces from the URL
     *
     * @param urlPart The part of the url we are canonicalizing
     * @return The URL part with all whitespace characters removed, or empty string if input is null
     */
    internal fun removeSpecialSpaces(urlPart: String?): String =
        urlPart?.filterNot { it.isWhiteSpace() } ?: ""

    /**
     * Replaces all special characters in the url with hex strings.
     *
     * @param url The URL string to encode
     * @return The encoded URL string, or empty string if input is null
     */
    internal fun encode(url: String?): String {
        url ?: return ""

        return buildString(url.length * 2) {
            url.forEach { character ->
                when {
                    character.code <= 32 || character.code >= 127 || character == '#' || character == '%' -> {
                        append(String.format("%%%02X", character.code.toByte()))
                    }
                    else -> append(character)
                }
            }
        }
    }

    /**
     * Removes all leading and trailing dots; replaces consecutive dots with a single dot
     * Ex: ".local.....com." -> "local.com"
     *
     * @param host The host string to normalize
     * @return The normalized host string with extra dots removed, or empty string if input is null
     */
    internal fun removeExtraDots(host: String?): String {
        host ?: return ""
        if (host.isEmpty()) return ""

        // First, replace consecutive dots with a single dot
        val normalizedDotsString = host.replace(Regex("\\.{2,}"), ".")

        // Then remove leading and trailing dots
        return normalizedDotsString.trim('.')
    }
}
