package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.CharExtensions.isHex
import com.slothiesmooth.linksdetektor.internal.CharExtensions.isWhiteSpace
import com.slothiesmooth.linksdetektor.internal.InputTextReader
import java.util.Stack

internal object UrlUtil {
    /**
     * Decodes the url by iteratively removing hex characters with backtracking.
     * For example: %2525252525252525 becomes %
     */
    internal fun decode(url: String?): String {
        val stringBuilder = StringBuilder(url)
        val nonDecodedPercentIndices = Stack<Int>()
        var i = 0
        while (i < stringBuilder.length - 2) {
            val curr = stringBuilder[i]
            if (curr == '%') {
                if (stringBuilder[i + 1].isHex() && stringBuilder[i + 2].isHex()) {
                    val decodedChar = String.format(
                        "%s", stringBuilder.substring(i + 1, i + 3).toShort(16)
                            .toInt().toChar()
                    )[0]
                    stringBuilder.delete(i, i + 3) // delete the % and two hex digits
                    stringBuilder.insert(i, decodedChar) // add decoded character
                    if (decodedChar == '%') {
                        i-- // backtrack one character to check for another decoding with this %.
                    } else if (
                        (nonDecodedPercentIndices.isEmpty().not() &&
                                decodedChar.isHex() &&
                                stringBuilder[i - 1].isHex()) &&
                        i - nonDecodedPercentIndices.peek() == 2
                    ) {
                        // Go back to the last non-decoded percent sign if it can be decoded.
                        // We only need to go back if it's of form %[HEX][HEX]
                        i = nonDecodedPercentIndices.pop() - 1 // backtrack to the % sign.
                    } else if (!nonDecodedPercentIndices.isEmpty() && i == stringBuilder.length - 2) {
                        // special case to handle %[HEX][Unknown][end of string]
                        i = nonDecodedPercentIndices.pop() - 1 // backtrack to the % sign.
                    }
                } else {
                    nonDecodedPercentIndices.add(i)
                }
            }
            i++
        }
        return stringBuilder.toString()
    }

    /**
     * Removes TAB (0x09), CR (0x0d), and LF (0x0a) from the URL
     * @param urlPart The part of the url we are canonicalizing
     */
    internal fun removeSpecialSpaces(urlPart: String?): String {
        val stringBuilder = StringBuilder(urlPart)
        for (i in stringBuilder.indices) {
            val curr = stringBuilder[i]
            if (curr.isWhiteSpace()) {
                stringBuilder.deleteCharAt(i)
            }
        }
        return stringBuilder.toString()
    }

    /**
     * Replaces all special characters in the url with hex strings.
     */
    internal fun encode(url: String?): String {
        val encoder = StringBuilder()
        for (chr in url!!.toCharArray()) {
            val chrByte = chr.code.toByte()
            if (chrByte <= 32 || chrByte >= 127 || chr == '#' || chr == '%') {
                encoder.append(String.format("%%%02X", chrByte))
            } else {
                encoder.append(chr)
            }
        }
        return encoder.toString()
    }

    /**
     * Removes all leading and trailing dots; replaces consecutive dots with a single dot
     * Ex: ".local.....com." -> "local.com"
     */
    internal fun removeExtraDots(host: String): String {
        val stringBuilder = StringBuilder()
        val reader = InputTextReader(host)
        while (!reader.eof()) {
            val curr: Char = reader.read()
            stringBuilder.append(curr)
            if (curr == '.') {
                var possibleDot = curr
                while (possibleDot == '.' && !reader.eof()) {
                    possibleDot = reader.read()
                }
                if (possibleDot != '.') {
                    stringBuilder.append(possibleDot)
                }
            }
        }
        if (stringBuilder.isNotEmpty() &&
            stringBuilder[stringBuilder.length - 1] == '.'
        ) {
            stringBuilder.deleteCharAt(stringBuilder.length - 1)
        }
        if (stringBuilder.isNotEmpty() &&
            stringBuilder[0] == '.'
        ) {
            stringBuilder.deleteCharAt(0)
        }
        return stringBuilder.toString()
    }
}
