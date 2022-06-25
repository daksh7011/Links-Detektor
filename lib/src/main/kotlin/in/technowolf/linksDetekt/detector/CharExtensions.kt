package `in`.technowolf.linksDetekt.detector

import org.apache.commons.lang3.StringUtils

object CharExtensions {
    /**
     * Checks if character is a valid hex character.
     * @return [Boolean]
     */
    fun Char.isHex(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }

    /**
     * Checks if character is a valid alphabetic character.
     * @return [Boolean]
     */
    fun Char.isAlpha(): Boolean {
        return this in 'a'..'z' || this in 'A'..'Z'
    }

    /**
     * Checks if character is a valid numeric character.
     * @return [Boolean]
     */
    fun Char.isNumeric(): Boolean {
        return this in '0'..'9'
    }

    /**
     * Checks if character is a valid alphanumeric character.
     * @return [Boolean]
     */
    fun Char.isAlphaNumeric(): Boolean {
        return this.isAlpha() || this.isNumeric()
    }

    /**
     * Checks if character is a valid unreserved character. This is defined by the RFC 3986 ABNF
     * @return [Boolean]
     */
    fun Char.isUnreserved(): Boolean {
        return this.isAlphaNumeric() || this == '-' || this == '.' || this == '_' || this == '~'
    }

    /**
     * Checks if character is a dot.
     * Reference: http://docs.oracle.com/javase/6/docs/api/java/net/IDN.html#toASCII%28java.lang.String,%20int%29
     * @return [Boolean]
     */
    fun Char.isDot(): Boolean {
        return this == '.' || this == '\u3002' || this == '\uFF0E' || this == '\uFF61'
    }

    /**
     * Checks if character is whitespace.
     * @return [Boolean]
     */
    fun Char.isWhiteSpace(): Boolean {
        return this == '\n' || this == '\t' || this == '\r' || this == ' '
    }

    /**
     * Splits a string without the use of a regex, which could split either by isDot() or %2e
     * @return an array of strings that is a partition of the original string split by dot
     */
    fun String.splitByDot(): Array<String> {
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
                    inputTextReader.read() //advance past the 2e
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
