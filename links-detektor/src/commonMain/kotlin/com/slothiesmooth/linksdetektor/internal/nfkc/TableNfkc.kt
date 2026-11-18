package com.slothiesmooth.linksdetektor.internal.nfkc

/**
 * Unicode 15.0.0 NFKC for every platform without a normalizer of its own, driven by the tables in [NfkcData].
 */
internal object TableNfkc {
    private const val HANGUL_BASE = 0xAC00
    private const val L_BASE = 0x1100
    private const val V_BASE = 0x1161
    private const val T_BASE = 0x11A7
    private const val L_COUNT = 19
    private const val V_COUNT = 21
    private const val T_COUNT = 28
    private const val N_COUNT = V_COUNT * T_COUNT
    private const val S_COUNT = L_COUNT * N_COUNT
    private const val FIRST_COMBINING = 0x300
    private const val SECOND_SHIFT = 21

    private val tables: Tables by lazy { Tables.decode() }

    /**
     * Normalizes [input] to NFKC.
     *
     * @param input The text to normalize; lone surrogates are passed through unchanged.
     * @return The NFKC form of [input].
     */
    fun normalize(input: String): String {
        if (input.all { it.code < 0x80 }) return input
        val decomposed = decompose(input)
        reorder(decomposed)
        return compose(decomposed)
    }

    private fun decompose(input: String): IntBuffer {
        val result = IntBuffer(input.length + 8)
        var index = 0
        while (index < input.length) {
            val cp = codePointAt(input, index)
            index += if (cp > 0xFFFF) 2 else 1
            if (cp - HANGUL_BASE in 0 until S_COUNT) {
                val s = cp - HANGUL_BASE
                result.add(L_BASE + s / N_COUNT)
                result.add(V_BASE + s % N_COUNT / T_COUNT)
                if (s % T_COUNT != 0) result.add(T_BASE + s % T_COUNT)
            } else {
                tables.decompositionOf(cp, result)
            }
        }
        return result
    }

    private fun reorder(buffer: IntBuffer) {
        val data = buffer.data
        var i = 1
        while (i < buffer.size) {
            val cls = tables.combiningClass(data[i])
            if (cls != 0) {
                var j = i
                while (j > 0) {
                    val prev = tables.combiningClass(data[j - 1])
                    if (prev <= cls) break
                    val moved = data[j]
                    data[j] = data[j - 1]
                    data[j - 1] = moved
                    j--
                }
            }
            i++
        }
    }

    private fun compose(buffer: IntBuffer): String {
        val data = buffer.data
        var out = 0
        var starterAt = -1
        var lastClass = 0
        for (i in 0 until buffer.size) {
            val cp = data[i]
            val cls = tables.combiningClass(cp)
            if (starterAt >= 0 && (lastClass < cls || lastClass == 0)) {
                val composite = composePair(data[starterAt], cp)
                if (composite >= 0) {
                    data[starterAt] = composite
                    continue
                }
            }
            if (cls == 0) starterAt = out
            lastClass = cls
            data[out++] = cp
        }
        val sb = StringBuilder(out)
        for (i in 0 until out) appendCodePoint(sb, data[i])
        return sb.toString()
    }

    private fun composePair(first: Int, second: Int): Int {
        val lIndex = first - L_BASE
        val vIndex = second - V_BASE
        if (lIndex in 0 until L_COUNT && vIndex in 0 until V_COUNT) {
            return HANGUL_BASE + (lIndex * V_COUNT + vIndex) * T_COUNT
        }
        val sIndex = first - HANGUL_BASE
        val tIndex = second - T_BASE
        if (sIndex in 0 until S_COUNT && sIndex % T_COUNT == 0 && tIndex in 1 until T_COUNT) {
            return first + tIndex
        }
        return tables.composite(first, second)
    }

    private fun searchInts(keys: IntArray, key: Int): Int {
        var lo = 0
        var hi = keys.size - 1
        while (lo <= hi) {
            val mid = lo + hi ushr 1
            when {
                keys[mid] < key -> lo = mid + 1
                keys[mid] > key -> hi = mid - 1
                else -> return mid
            }
        }
        return -1
    }

    private fun searchLongs(keys: LongArray, key: Long): Int {
        var lo = 0
        var hi = keys.size - 1
        while (lo <= hi) {
            val mid = lo + hi ushr 1
            when {
                keys[mid] < key -> lo = mid + 1
                keys[mid] > key -> hi = mid - 1
                else -> return mid
            }
        }
        return -1
    }

    private fun codePointAt(s: String, index: Int): Int {
        val high = s[index]
        if (high.isHighSurrogate() && index + 1 < s.length) {
            val low = s[index + 1]
            if (low.isLowSurrogate()) {
                return 0x10000 + (high.code - 0xD800 shl 10) + (low.code - 0xDC00)
            }
        }
        return high.code
    }

    private fun appendCodePoint(sb: StringBuilder, cp: Int) {
        if (cp > 0xFFFF) {
            val v = cp - 0x10000
            sb.append((0xD800 + (v shr 10)).toChar())
            sb.append((0xDC00 + (v and 0x3FF)).toChar())
        } else {
            sb.append(cp.toChar())
        }
    }

    /** A growable `IntArray`. */
    private class IntBuffer(capacity: Int) {
        var data = IntArray(capacity)
        var size = 0

        fun add(value: Int) {
            if (size == data.size) data = data.copyOf(size * 2)
            data[size++] = value
        }
    }

    private class Tables(
        private val cccStarts: IntArray,
        private val cccEnds: IntArray,
        private val cccValues: IntArray,
        private val decompKeys: IntArray,
        private val decompOffsets: IntArray,
        private val decompData: IntArray,
        private val compKeys: LongArray,
        private val compValues: IntArray,
    ) {
        fun combiningClass(cp: Int): Int {
            if (cp < FIRST_COMBINING) return 0
            var lo = 0
            var hi = cccStarts.size - 1
            while (lo <= hi) {
                val mid = lo + hi ushr 1
                when {
                    cp < cccStarts[mid] -> hi = mid - 1
                    cp > cccEnds[mid] -> lo = mid + 1
                    else -> return cccValues[mid]
                }
            }
            return 0
        }

        fun decompositionOf(cp: Int, into: IntBuffer) {
            val at = searchInts(decompKeys, cp)
            if (at < 0) {
                into.add(cp)
                return
            }
            for (k in decompOffsets[at] until decompOffsets[at + 1]) into.add(decompData[k])
        }

        fun composite(first: Int, second: Int): Int {
            val at = searchLongs(compKeys, first.toLong() shl SECOND_SHIFT or second.toLong())
            return if (at < 0) -1 else compValues[at]
        }

        companion object {
            fun decode(): Tables {
                val ccc = Reader(NfkcData.CCC_RANGES.joinToString(""))
                val starts = IntBuffer(512)
                val ends = IntBuffer(512)
                val values = IntBuffer(512)
                var previousEnd = 0
                while (ccc.hasNext()) {
                    val start = previousEnd + ccc.next()
                    val end = start + ccc.next()
                    starts.add(start)
                    ends.add(end)
                    values.add(ccc.next())
                    previousEnd = end
                }

                val decomp = Reader(NfkcData.DECOMPOSITIONS.joinToString(""))
                val keys = IntBuffer(8192)
                val offsets = IntBuffer(8192)
                val flat = IntBuffer(16384)
                var key = 0
                while (decomp.hasNext()) {
                    key += decomp.next()
                    keys.add(key)
                    offsets.add(flat.size)
                    repeat(decomp.next()) { flat.add(decomp.next()) }
                }
                offsets.add(flat.size)

                val comp = Reader(NfkcData.COMPOSITIONS.joinToString(""))
                val pairKeys = LongBuffer(1024)
                val composites = IntBuffer(1024)
                var first = 0
                while (comp.hasNext()) {
                    first += comp.next()
                    val second = comp.next()
                    pairKeys.add(first.toLong() shl SECOND_SHIFT or second.toLong())
                    composites.add(comp.next())
                }

                return Tables(
                    starts.data.copyOf(starts.size),
                    ends.data.copyOf(ends.size),
                    values.data.copyOf(values.size),
                    keys.data.copyOf(keys.size),
                    offsets.data.copyOf(offsets.size),
                    flat.data.copyOf(flat.size),
                    pairKeys.data.copyOf(pairKeys.size),
                    composites.data.copyOf(composites.size),
                )
            }
        }
    }

    private class LongBuffer(capacity: Int) {
        var data = LongArray(capacity)
        var size = 0

        fun add(value: Long) {
            if (size == data.size) data = data.copyOf(size * 2)
            data[size++] = value
        }
    }

    /** Reads the base-32 varints written by the generator. */
    private class Reader(private val text: String) {
        private var position = 0

        fun hasNext(): Boolean = position < text.length

        fun next(): Int {
            var value = 0
            var shift = 0
            while (true) {
                val digit = digitOf(text[position++])
                value = value or (digit and 31 shl shift)
                if (digit < 32) return value
                shift += 5
            }
        }

        private fun digitOf(c: Char): Int = when (c) {
            in 'A'..'Z' -> c - 'A'
            in 'a'..'z' -> c - 'a' + 26
            in '0'..'9' -> c - '0' + 52
            '+' -> 62
            else -> 63
        }
    }
}
