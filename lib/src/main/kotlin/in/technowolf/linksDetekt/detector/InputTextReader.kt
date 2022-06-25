package `in`.technowolf.linksDetekt.detector

import `in`.technowolf.linksDetekt.detector.CharExtensions.isWhiteSpace

/**
 * Class used to read a text input character by character. This also gives the ability to backtrack.
 * @param _content The content to read.
*/
class InputTextReader(private val _content: String) {
    /**
     * Gets the current position in the stream.
     * @return The index to the current position.
     */
    /**
     * The current position in the content we are looking at.
     */
    var position = 0
        private set
    /**
     * Gets the total number of characters that were backtracked when reading.
     */
    /**
     * Contains the amount of characters that were backtracked. This is used for performance analysis.
     */
    var backtrackedCount = 0
        private set

    /**
     * Reads a single char from the content stream and increments the index.
     * @return The next available character.
     */
    fun read(): Char {
        val chr = _content[position++]
        return if (chr.isWhiteSpace()) ' ' else chr
    }

    /**
     * Peeks at the next number of chars and returns as a string without incrementing the current index.
     * @param numberChars The number of chars to peek.
     */
    fun peek(numberChars: Int): String {
        return _content.substring(position, position + numberChars)
    }

    /**
     * Gets the character in the array offset by the current index.
     * @param offset The number of characters to offset.
     * @return The character at the location of the index plus the provided offset.
     */
    fun peekChar(offset: Int): Char {
        if (!canReadChars(offset)) {
            throw ArrayIndexOutOfBoundsException()
        }
        return _content[position + offset]
    }

    /**
     * Returns true if the reader has more the specified number of chars.
     * @param numberChars The number of chars to see if we can read.
     * @return True if we can read this number of chars, else false.
     */
    fun canReadChars(numberChars: Int): Boolean {
        return _content.length >= position + numberChars
    }

    /**
     * Checks if the current stream is at the end.
     * @return True if the stream is at the end and no more can be read.
     */
    fun eof(): Boolean {
        return _content.length <= position
    }

    /**
     * Moves the index to the specified position.
     * @param position The position to set the index to.
     */
    fun seek(position: Int) {
        val backtrackLength = Math.max(this.position - position, 0)
        backtrackedCount += backtrackLength
        this.position = position
        checkBacktrackLoop(backtrackLength)
    }

    /**
     * Goes back a single character.
     */
    fun goBack() {
        backtrackedCount++
        position--
        checkBacktrackLoop(1)
    }

    private fun checkBacktrackLoop(backtrackLength: Int) {
        var backtrackLength = backtrackLength
        if (backtrackedCount > _content.length * MAX_BACKTRACK_MULTIPLIER) {
            if (backtrackLength < MINIMUM_BACKTRACK_LENGTH) {
                backtrackLength = MINIMUM_BACKTRACK_LENGTH
            }
            val start = position.coerceAtLeast(0)
            if (start + backtrackLength > _content.length) {
                backtrackLength = _content.length - start
            }
            val badText: String = _content.substring(start, start + backtrackLength)
            throw NegativeArraySizeException(
                "Backtracked max amount of characters. Endless loop detected. Bad Text: '" +
                        badText + "'"
            )
        }
    }

    companion object {
        /**
         * The number of times something can be backtracked is this multiplier times the length of the string.
         */
        const val MAX_BACKTRACK_MULTIPLIER = 10

        /**
         * When detecting for exceeding the backtrack limit, make sure the text is at least 20 characters.
         */
        private const val MINIMUM_BACKTRACK_LENGTH = 20
    }
}
