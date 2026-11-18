package com.slothiesmooth.linksdetektor.internal

/**
 * A character buffer for in-place rewriting. Deleting or inserting costs time proportional to the distance
 * from the previous edit, not to the length of the text, so a pass that edits as it scans stays linear where
 * a [StringBuilder] would be quadratic.
 *
 * @param text The initial content.
 */
internal class GapBuffer(text: String) {
    private var chars: CharArray = text.toCharArray()
    private var gapStart: Int = chars.size
    private var gapEnd: Int = chars.size

    /** The number of characters in the buffer. */
    val length: Int
        get() = chars.size - (gapEnd - gapStart)

    /**
     * Returns the character at [index].
     *
     * @throws IndexOutOfBoundsException If [index] is not in `0 until length`.
     */
    operator fun get(index: Int): Char {
        if (index < 0 || index >= length) {
            throw IndexOutOfBoundsException("index $index, length $length")
        }
        return if (index < gapStart) chars[index] else chars[index + gapEnd - gapStart]
    }

    /** Returns the character at [index], or null if [index] is out of bounds. */
    fun getOrNull(index: Int): Char? = if (index in 0 until length) get(index) else null

    /** Whether the buffer has no characters. */
    fun isEmpty(): Boolean = length == 0

    /**
     * Removes the characters from [startIndex] (inclusive) to [endIndex] (exclusive).
     *
     * @throws IndexOutOfBoundsException If the range is not within the buffer.
     */
    fun deleteRange(startIndex: Int, endIndex: Int) {
        if (startIndex < 0 || startIndex > endIndex || endIndex > length) {
            throw IndexOutOfBoundsException("range $startIndex until $endIndex, length $length")
        }
        if (gapStart < startIndex) {
            moveGap(startIndex)
        } else if (gapStart > endIndex) {
            moveGap(endIndex)
        }
        gapEnd += endIndex - gapStart
        gapStart = startIndex
    }

    /** Removes the character at [index]. */
    fun deleteAt(index: Int) {
        deleteRange(index, index + 1)
    }

    /**
     * Inserts [char] so that it ends up at [index].
     *
     * @throws IndexOutOfBoundsException If [index] is not in `0..length`.
     */
    fun insert(index: Int, char: Char) {
        if (index < 0 || index > length) {
            throw IndexOutOfBoundsException("index $index, length $length")
        }
        if (gapStart == gapEnd) {
            grow()
        }
        moveGap(index)
        chars[gapStart++] = char
    }

    /** Adds [char] at the end. */
    fun append(char: Char) {
        insert(length, char)
    }

    override fun toString(): String = chars.concatToString(0, gapStart) + chars.concatToString(gapEnd, chars.size)

    private fun moveGap(index: Int) {
        // An empty gap has nothing to carry: it can jump.
        if (gapStart != gapEnd) {
            if (index < gapStart) {
                chars.copyInto(chars, gapEnd - (gapStart - index), index, gapStart)
            } else if (index > gapStart) {
                chars.copyInto(chars, gapStart, gapEnd, gapEnd + (index - gapStart))
            }
        }
        gapEnd += index - gapStart
        gapStart = index
    }

    private fun grow() {
        val extra = maxOf(MIN_GROWTH, chars.size / 2)
        val grown = CharArray(chars.size + extra)
        chars.copyInto(grown, 0, 0, gapStart)
        chars.copyInto(grown, gapStart + extra, gapEnd, chars.size)
        gapEnd = gapStart + extra
        chars = grown
    }

    private companion object {
        private const val MIN_GROWTH = 16
    }
}
