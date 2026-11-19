package com.slothiesmooth.linksdetektor.internal.model

/**
 * The response of character matching.
 */
internal enum class CharacterMatch {
    /**
     * The character was not matched.
     */
    CharacterNotMatched,

    /**
     * A character was matched with requires a stop.
     */
    CharacterMatchStop,

    /**
     * The character was matched, which is a start of parentheses.
     */
    CharacterMatchStart
}
