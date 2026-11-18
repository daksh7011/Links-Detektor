package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Punycode (RFC 3492) encoder for a single label.
 */
internal object Punycode {
    private const val BASE = 36
    private const val T_MIN = 1
    private const val T_MAX = 26
    private const val SKEW = 38
    private const val DAMP = 700
    private const val INITIAL_BIAS = 72
    private const val INITIAL_N = 128
    private const val DELIMITER = '-'
    private const val ASCII_LIMIT = 0x80
    private const val DIGIT_OFFSET = 22 // '0' (0x30) is digit 26: 26 + 22 = 48

    /**
     * Encodes [label] to Punycode, without the `xn--` prefix.
     *
     * @param label The label to encode; surrogate pairs are treated as one code point.
     * @return The Punycode text.
     * @throws IllegalArgumentException If the arithmetic overflows.
     */
    fun encode(label: String): String {
        val codePoints = toCodePoints(label)
        val output = StringBuilder()
        for (codePoint in codePoints) {
            if (codePoint < ASCII_LIMIT) output.append(codePoint.toChar())
        }
        val basicCount = output.length
        var handled = basicCount
        if (basicCount > 0) output.append(DELIMITER)

        var n = INITIAL_N
        var delta = 0L
        var bias = INITIAL_BIAS
        while (handled < codePoints.size) {
            val next = codePoints.filter { it >= n }.min()
            delta += (next - n).toLong() * (handled + 1)
            requireNoOverflow(delta)
            n = next
            for (codePoint in codePoints) {
                if (codePoint < n) {
                    delta++
                    requireNoOverflow(delta)
                } else if (codePoint == n) {
                    appendVariableLength(output, delta, bias)
                    bias = adapt(delta, handled + 1, handled == basicCount)
                    delta = 0
                    handled++
                }
            }
            delta++
            n++
        }
        return output.toString()
    }

    private fun appendVariableLength(output: StringBuilder, delta: Long, bias: Int) {
        var q = delta
        var k = BASE
        while (true) {
            val t = when {
                k <= bias -> T_MIN
                k >= bias + T_MAX -> T_MAX
                else -> k - bias
            }
            if (q < t) break
            output.append(digit((t + (q - t) % (BASE - t)).toInt()))
            q = (q - t) / (BASE - t)
            k += BASE
        }
        output.append(digit(q.toInt()))
    }

    private fun adapt(delta: Long, numPoints: Int, first: Boolean): Int {
        var scaled = if (first) delta / DAMP else delta / 2
        scaled += scaled / numPoints
        var k = 0
        while (scaled > (BASE - T_MIN) * T_MAX / 2) {
            scaled /= BASE - T_MIN
            k += BASE
        }
        return (k + (BASE - T_MIN + 1) * scaled / (scaled + SKEW)).toInt()
    }

    private fun digit(value: Int): Char = if (value < 26) 'a' + value else (value + DIGIT_OFFSET).toChar()

    private fun requireNoOverflow(delta: Long) {
        require(delta <= Int.MAX_VALUE) { "Punycode overflow" }
    }

    private fun toCodePoints(label: String): ImmutableList<Int> {
        val result = persistentListOf<Int>().builder()
        var index = 0
        while (index < label.length) {
            val high = label[index]
            if (high.isHighSurrogate() && index + 1 < label.length && label[index + 1].isLowSurrogate()) {
                val low = label[index + 1]
                result.add((high.code - 0xD800 shl 10) + (low.code - 0xDC00) + 0x10000)
                index += 2
            } else {
                result.add(high.code)
                index++
            }
        }
        return result.build()
    }
}
