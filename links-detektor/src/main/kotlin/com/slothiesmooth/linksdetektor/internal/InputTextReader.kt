package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.CharExtensions.isWhiteSpace

/**
 * A character-by-character reader for text input with backtracking capabilities.
 *
 * This class provides a streaming interface for reading text content, with features that are
 * particularly useful for URL parsing:
 * - Character-by-character reading
 * - Peeking ahead without advancing the position
 * - Backtracking to previously read positions
 * - Automatic whitespace normalization
 * - Loop detection to prevent infinite backtracking
 *
 * The reader maintains a current position in the text and provides methods to read,
 * peek ahead, and move backward as needed during parsing.
 *
 * @property content The text content to be read.
 */
internal class InputTextReader(private val content: String) {
    /**
     * The current reading position within the content.
     *
     * This property represents the index of the next character to be read.
     * It is automatically incremented by read operations and can be manually
     * adjusted using [seek] or [goBack].
     */
    var position = 0
        private set

    /**
     * The total number of characters that were backtracked during reading.
     *
     * This counter is incremented whenever the reader moves backward in the content,
     * either through [goBack] or [seek]. It's useful for performance analysis and
     * detecting potential infinite loops in parsing algorithms.
     */
    var backtrackedCount = 0
        private set

    /**
     * Reads the next character from the content and advances the position.
     *
     * This method reads a single character from the current position in the content,
     * increments the position, and returns the character. Any whitespace character
     * (as defined by [CharExtensions.isWhiteSpace]) is normalized to a space character.
     *
     * @return The next character in the content, with whitespace normalized to a space.
     */
    fun read(): Char {
        val chr = content[position++]
        return if (chr.isWhiteSpace()) ' ' else chr
    }

    /**
     * Looks ahead at upcoming characters without advancing the position.
     *
     * This method returns a substring of the specified length starting at the current position,
     * without changing the current position.
     *
     * @param numberChars The number of characters to peek ahead.
     * @return A string containing the next [numberChars] characters from the current position.
     * @throws StringIndexOutOfBoundsException If there aren't enough characters remaining.
     */
    fun peek(numberChars: Int): String = content.substring(position, position + numberChars)

    /**
     * Looks ahead at a single character at a specified offset from the current position.
     *
     * This method returns the character at the specified offset from the current position,
     * without changing the current position.
     *
     * @param offset The offset from the current position (0 for the current character).
     * @return The character at the specified offset from the current position.
     * @throws ArrayIndexOutOfBoundsException If the offset is beyond the content bounds.
     */
    fun peekChar(offset: Int): Char {
        if (!canReadChars(offset)) {
            throw ArrayIndexOutOfBoundsException()
        }
        return content[position + offset]
    }

    /**
     * Checks if there are enough characters remaining to read the specified number.
     *
     * This method determines if the reader can safely read or peek the specified
     * number of characters from the current position without exceeding the content bounds.
     *
     * @param numberChars The number of characters to check for availability.
     * @return `true` if at least [numberChars] characters remain in the content, `false` otherwise.
     */
    fun canReadChars(numberChars: Int): Boolean = content.length >= position + numberChars

    /**
     * Checks if the reader has reached the end of the content.
     *
     * This method determines if there are any more characters available to read
     * from the current position.
     *
     * @return `true` if the reader has reached the end of the content, `false` otherwise.
     */
    fun eof(): Boolean = content.length <= position

    /**
     * Moves the current position to a specified index in the content.
     *
     * This method sets the current position to the specified index, allowing for
     * both forward and backward movement within the content. If moving backward,
     * the [backtrackedCount] is incremented by the distance moved.
     *
     * The method also checks for potential infinite backtracking loops.
     *
     * @param position The new position to set (index in the content).
     * @throws NegativeArraySizeException If excessive backtracking is detected,
     *         indicating a potential infinite loop.
     */
    fun seek(position: Int) {
        val backtrackLength = Math.max(this.position - position, 0)
        backtrackedCount += backtrackLength
        this.position = position
        checkBacktrackLoop(backtrackLength)
    }

    /**
     * Moves the current position back by one character.
     *
     * This method decrements the current position by one, allowing the reader to
     * re-read the previous character. The [backtrackedCount] is incremented by one.
     *
     * The method also checks for potential infinite backtracking loops.
     *
     * @throws NegativeArraySizeException If excessive backtracking is detected,
     *         indicating a potential infinite loop.
     */
    fun goBack() {
        backtrackedCount++
        position--
        checkBacktrackLoop(1)
    }

    /**
     * Checks if backtracking has exceeded safe limits, indicating a potential infinite loop.
     *
     * This method is called after any backtracking operation to detect and prevent
     * infinite loops in parsing algorithms. If the total backtracked count exceeds
     * a threshold (content length × [MAX_BACKTRACK_MULTIPLIER]), an exception is thrown.
     *
     * @param backtrackLength The length of the most recent backtrack operation.
     * @throws NegativeArraySizeException If excessive backtracking is detected.
     */
    private fun checkBacktrackLoop(backtrackLength: Int) {
        var backtrackLength = backtrackLength
        if (backtrackedCount > content.length * MAX_BACKTRACK_MULTIPLIER) {
            if (backtrackLength < MINIMUM_BACKTRACK_LENGTH) {
                backtrackLength = MINIMUM_BACKTRACK_LENGTH
            }
            val start = position.coerceAtLeast(0)
            if (start + backtrackLength > content.length) {
                backtrackLength = content.length - start
            }
            val badText: String = content.substring(start, start + backtrackLength)
            throw NegativeArraySizeException(
                "Backtracked max amount of characters. Endless loop detected. Bad Text: '" +
                        badText + "'"
            )
        }
    }

    companion object {
        /**
         * Maximum allowed backtracking multiplier for loop detection.
         *
         * This constant defines the maximum ratio of backtracked characters to content length
         * before an infinite loop is suspected. The total allowed backtracking is calculated as:
         * content length × MAX_BACKTRACK_MULTIPLIER.
         *
         * A value of 10 means that backtracking can occur up to 10 times the length of the content
         * before an exception is thrown.
         */
        const val MAX_BACKTRACK_MULTIPLIER = 10

        /**
         * Minimum length of text to include in backtracking error messages.
         *
         * When reporting a backtracking error, this constant ensures that at least this many
         * characters of context are included in the error message, which helps with debugging.
         */
        private const val MINIMUM_BACKTRACK_LENGTH = 20
    }
}
