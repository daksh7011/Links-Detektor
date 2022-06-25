package `in`.technowolf.linksDetekt.detector

import `in`.technowolf.linksDetekt.detector.CharExtensions.isAlpha
import `in`.technowolf.linksDetekt.detector.CharExtensions.isAlphaNumeric
import `in`.technowolf.linksDetekt.detector.CharExtensions.isDot
import `in`.technowolf.linksDetekt.detector.CharExtensions.isHex
import `in`.technowolf.linksDetekt.detector.CharExtensions.isNumeric
import `in`.technowolf.linksDetekt.detector.CharExtensions.isUnreserved
import `in`.technowolf.linksDetekt.detector.CharExtensions.splitByDot
import java.util.Locale

/**
 * The domain name reader reads input from a InputTextReader and validates if the content being read is a valid domain name.
 * After a domain name is read, the returning status is what to do next. If the domain is valid but a specific character is found,
 * the next state will be to read another part for the rest of the url. For example, if a "?" is found at the end and the
 * domain is valid, the return state will be to read a query string.
 *
 * @param inputTextReader The input stream to read.
 * @param bufferStringBuilder The string buffer to use for storing a domain name.
 * @param_ current The current string that was thought to be a domain name.
 * @param linksDetektorOptions The detector options of this reader.
 * @param characterHandler The handler to call on each non-matching character to count matching quotes and stuff.
 **/
class DomainNameReader(
    inputTextReader: InputTextReader,
    /**
     * The currently written string buffer.
     */
    private val bufferStringBuilder: StringBuilder,
    /**
     * The domain name started with a partial domain name found. This is the original string of the domain name only.
     */
    private val _current: String?,
    linksDetektorOptions: LinksDetektorOptions,
    characterHandler: CharacterHandler,
) {
    /**
     * This is the final return state of reading a domain name.
     */
    enum class ReaderNextState {
        /**
         * Trying to read the domain name caused it to be invalid.
         */
        InvalidDomainName,

        /**
         * The domain name is found to be valid.
         */
        ValidDomainName,

        /**
         * Finished reading, next step should be to read the fragment.
         */
        ReadFragment,

        /**
         * Finished reading, next step should be to read the path.
         */
        ReadPath,

        /**
         * Finished reading, next step should be to read the port.
         */
        ReadPort,

        /**
         * Finished reading, next step should be to read the query string.
         */
        ReadQueryString
    }

    /**
     * The interface that gets called for each character that's non-matching (to a valid domain name character) in to count
     * the matching quotes and parenthesis correctly.
     */
    interface CharacterHandler {
        fun addCharacter(character: Char)
    }

    /**
     * Detection option of this reader.
     */
    private val _options: LinksDetektorOptions

    /**
     * Keeps track the number of dots that were found in the domain name.
     */
    private var _dots = 0

    /**
     * Keeps track of the number of characters since the last "."
     */
    private var _currentLabelLength = 0

    /**
     * Keeps track of the number of characters in the top level domain.
     */
    private var _topLevelLength = 0

    /**
     * Keeps track where the domain name started. This is non zero if the buffer starts with
     * http://username:password@...
     */
    private var _startDomainName = 0

    /**
     * Keeps track if the entire domain name is numeric.
     */
    private var _numeric = false

    /**
     * Keeps track if we are seeing an ipv6 type address.
     */
    private var _seenBracket = false

    /**
     * Keeps track if we have seen a full bracket set "[....]"; used for ipv6 type address.
     */
    private var _seenCompleteBracketSet = false

    /**
     * Keeps track if we have a zone index in the ipv6 address.
     */
    private var _zoneIndex = false

    /**
     * Contains the input stream to read.
     */
    private val _reader: InputTextReader

    /**
     * Contains the handler for each character match.
     */
    private val _characterHandler: CharacterHandler

    init {
        _reader = inputTextReader
        _options = linksDetektorOptions
        _characterHandler = characterHandler
    }

    /**
     * Reads and parses the current string to make sure the domain name started where it was supposed to,
     * and the current domain name is correct.
     * @return The next state to use after reading the current.
     */
    private fun readCurrent(): ReaderNextState {
        if (_current != null) {
            // Handles the case where the string is ".hello"
            if (_current.length == 1 && _current[0].isDot()) {
                return ReaderNextState.InvalidDomainName
            } else if (_current.length == 3 && _current.equals("%$HEX_ENCODED_DOT", ignoreCase = true)) {
                return ReaderNextState.InvalidDomainName
            }

            // The location where the domain name started.
            _startDomainName = bufferStringBuilder.length - _current.length

            // flag that the domain is currently all numbers and/or dots.
            _numeric = true

            // If an invalid char is found, we can just restart the domain from there.
            var newStart = 0
            val currArray = _current.toCharArray()
            val length = currArray.size

            // hex special case
            var isAllHexSoFar = length > 2 && currArray[0] == '0' && (currArray[1] == 'x' || currArray[1] == 'X')
            var index = if (isAllHexSoFar) 2 else 0
            var done = false
            while (index < length && !done) {
                // get the current character and update length counts.
                val curr = currArray[index]
                _currentLabelLength++
                _topLevelLength = _currentLabelLength

                // Is the length of the last part > 64 (plus one since we just incremented)
                if (_currentLabelLength > MAX_LABEL_LENGTH) {
                    return ReaderNextState.InvalidDomainName
                } else if (curr.isDot()) {
                    // found a dot. Increment dot count, and reset last length
                    _dots++
                    _currentLabelLength = 0
                } else if (curr == '[') {
                    _seenBracket = true
                    _numeric = false
                } else if (
                    curr == '%' &&
                    index + 2 < length &&
                    currArray[index + 1].isHex() &&
                    currArray[index + 2].isHex()
                ) {
                    // handle url encoded dot
                    if (currArray[index + 1] == '2' && currArray[index + 2] == 'e') {
                        _dots++
                        _currentLabelLength = 0
                    } else {
                        _numeric = false
                    }
                    index += 2
                } else if (isAllHexSoFar) {
                    // if it's a valid character in the domain that is not numeric
                    if (curr.isHex().not()) {
                        _numeric = false
                        isAllHexSoFar = false
                        index-- // backtrack to rerun last character knowing it isn't hex.
                    }
                } else if (curr.isAlpha() || curr == '-' || curr.code >= INTERNATIONAL_CHAR_START) {
                    _numeric = false
                } else if (curr.isNumeric().not() &&
                    _options.hasFlag(LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).not()
                ) {
                    // if its not _numeric and not alphabetical, then restart searching for a domain from this point.
                    newStart = index + 1
                    _currentLabelLength = 0
                    _topLevelLength = 0
                    _numeric = true
                    _dots = 0
                    done = true
                }
                index++
            }

            // An invalid character for the domain was found somewhere in the current buffer.
            // cut the first part of the domain out. For example:
            // http://asdf%asdf.google.com <- asdf.google.com is still valid, so restart from the %
            if (newStart > 0) {

                // make sure the location is not at the end. Otherwise the thing is just invalid.
                if (newStart < _current.length) {
                    bufferStringBuilder.replace(0, bufferStringBuilder.length, _current.substring(newStart))

                    // cut out the previous part, so now the domain name has to be from here.
                    _startDomainName = 0
                }

                // now after cutting if the buffer is just "." newStart > current (last character in current is invalid)
                if (newStart >= _current.length || bufferStringBuilder.toString() == ".") {
                    return ReaderNextState.InvalidDomainName
                }
            }
        } else {
            _startDomainName = bufferStringBuilder.length
        }

        // all else is good, return OK
        return ReaderNextState.ValidDomainName
    }

    /**
     * Reads the Dns and returns the next state the state machine should take in throwing this out, or continue processing
     * if this is a valid domain name.
     * @return The next state to take.
     */
    fun readDomainName(): ReaderNextState {

        // Read the current, and if its bad, just return.
        if (readCurrent() == ReaderNextState.InvalidDomainName) {
            return ReaderNextState.InvalidDomainName
        }

        // while not done and not end of string keep reading.
        var done = false
        while (!done && !_reader.eof()) {
            val curr: Char = _reader.read()
            if (curr == '/') {
                // continue by reading the path
                return checkDomainNameValid(ReaderNextState.ReadPath, curr)
            } else if (curr == ':' && (!_seenBracket || _seenCompleteBracketSet)) {
                // Don't check for a port if it's in the middle of an ipv6 address
                // continue by reading the port.
                return checkDomainNameValid(ReaderNextState.ReadPort, curr)
            } else if (curr == '?') {
                // continue by reading the query string
                return checkDomainNameValid(ReaderNextState.ReadQueryString, curr)
            } else if (curr == '#') {
                // continue by reading the fragment
                return checkDomainNameValid(ReaderNextState.ReadFragment, curr)
            } else if (curr.isDot() || curr == '%' && _reader.canReadChars(2) && _reader.peek(2)
                    .equals(HEX_ENCODED_DOT, ignoreCase = true)
            ) {
                // if the current character is a dot or a urlEncodedDot

                // handles the case: hello..
                if (_currentLabelLength < 1) {
                    done = true
                } else {
                    // append the "." to the domain name
                    bufferStringBuilder.append(curr)

                    // if it was not a normal dot, then it is url encoded
                    // read the next two chars, which are the hex representation
                    if (curr.isDot().not()) {
                        bufferStringBuilder.append(_reader.read())
                        bufferStringBuilder.append(_reader.read())
                    }

                    // increment the dots only if it's not part of the zone index and reset the last length.
                    if (!_zoneIndex) {
                        _dots++
                        _currentLabelLength = 0
                    }

                    // if the length of the last section is longer than or equal to 64, it's too long to be a valid domain
                    if (_currentLabelLength >= MAX_LABEL_LENGTH) {
                        return ReaderNextState.InvalidDomainName
                    }
                }
            } else if (_seenBracket &&
                (curr.isHex() || curr == ':' || curr == '[' || curr == ']' || curr == '%') &&
                _seenCompleteBracketSet.not()
            ) { // if this is an ipv6 address.
                when (curr) {
                    ':' -> _currentLabelLength = 0
                    '[' -> {
                        // if we read another '[', we need to restart by re-reading from this bracket instead.
                        _reader.goBack()
                        return ReaderNextState.InvalidDomainName
                    }
                    ']' -> {
                        _seenCompleteBracketSet = true // means that we already have a complete ipv6 address.
                        _zoneIndex = false // set this back off so that we can keep counting dots after ipv6 is over.
                    }
                    '%' -> _zoneIndex = true
                    else -> _currentLabelLength++
                }
                _numeric = false
                bufferStringBuilder.append(curr)
            } else if (curr.isAlphaNumeric() || curr == '-' || curr.code >= INTERNATIONAL_CHAR_START) {
                // Valid domain name character. Either a-z, A-Z, 0-9, -, or international character
                if (_seenCompleteBracketSet) {
                    // covers case of [fe80::]www.google.com
                    _reader.goBack()
                    done = true
                } else {
                    // if its not numeric, remember that; excluded x/X for hex ip addresses.
                    if (curr != 'x' && curr != 'X' && curr.isNumeric().not()) {
                        _numeric = false
                    }

                    // append to the states.
                    bufferStringBuilder.append(curr)
                    _currentLabelLength++
                    _topLevelLength = _currentLabelLength
                }
            } else if (curr == '[' && !_seenBracket) {
                _seenBracket = true
                _numeric = false
                bufferStringBuilder.append(curr)
            } else if (curr == '[' && _seenCompleteBracketSet) { // Case where [::][ ...
                _reader.goBack()
                done = true
            } else if (
                curr == '%' &&
                _reader.canReadChars(2) &&
                _reader.peekChar(0).isHex() &&
                _reader.peekChar(1).isHex()
            ) {
                // append to the states.
                bufferStringBuilder.append(curr)
                bufferStringBuilder.append(_reader.read())
                bufferStringBuilder.append(_reader.read())
                _currentLabelLength += 3
                _topLevelLength = _currentLabelLength
            } else {
                // called to increment the count of matching characters
                _characterHandler.addCharacter(curr)

                // invalid character, we are done.
                done = true
            }
        }

        // Check the domain name to make sure its ok.
        return checkDomainNameValid(ReaderNextState.ValidDomainName, null)
    }

    /**
     * Checks the current state of this object and returns if the valid state indicates that the
     * object has a valid domain name. If it does, it will return append the last character
     * and return the validState specified.
     * @param validState The state to return if this check indicates that the dns is ok.
     * @param lastChar The last character to add if the domain is ok.
     * @return The validState if the domain is valid, else ReaderNextState.InvalidDomainName
     */
    private fun checkDomainNameValid(validState: ReaderNextState, lastChar: Char?): ReaderNextState {
        var valid = false

        // Max domain length is 255 which includes the trailing "."
        // most of the time this is not included in the url.
        // If the _currentLabelLength is not 0 then the last "." is not included so add it.
        // Same with number of labels (or dots including the last)
        val lastDotLength = if (bufferStringBuilder.length > 3 && bufferStringBuilder.substring(bufferStringBuilder.length - 3)
                .equals("%$HEX_ENCODED_DOT", ignoreCase = true)
        ) 3 else 1
        val domainLength = bufferStringBuilder.length - _startDomainName + if (_currentLabelLength > 0) lastDotLength else 0
        val dotCount = _dots + if (_currentLabelLength > 0) 1 else 0
        if (domainLength >= MAX_DOMAIN_LENGTH || dotCount > MAX_NUMBER_LABELS) {
            valid = false
        } else if (_numeric) {
            val testDomain = bufferStringBuilder.substring(_startDomainName).lowercase(Locale.getDefault())
            valid = isValidIpv4(testDomain)
        } else if (_seenBracket) {
            val testDomain = bufferStringBuilder.substring(_startDomainName).lowercase(Locale.getDefault())
            valid = isValidIpv6(testDomain)
        } else if (_currentLabelLength > 0 && _dots >= 1 || _dots >= 2 && _currentLabelLength == 0 || _options.hasFlag(
                LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN
            ) && _dots == 0
        ) {
            var topStart = bufferStringBuilder.length - _topLevelLength
            if (_currentLabelLength == 0) {
                topStart--
            }
            topStart = topStart.coerceAtLeast(0)

            // get the first 4 characters of the top level domain
            val topLevelStart = bufferStringBuilder.substring(topStart, topStart + 4.coerceAtMost(bufferStringBuilder.length - topStart))

            // There is no size restriction if the top level domain is international (starts with "xn--")
            valid =
                topLevelStart.equals("xn--", ignoreCase = true) ||
                        (_topLevelLength in MIN_TOP_LEVEL_DOMAIN..MAX_TOP_LEVEL_DOMAIN)
        }
        if (valid) {
            // if it's valid, add the last character (if specified) and return the valid state.
            if (lastChar != null) {
                bufferStringBuilder.append(lastChar)
            }
            return validState
        }

        // Roll back one char if its invalid to handle: "00:41.<br />"
        // This gets detected as 41.br otherwise.
        _reader.goBack()

        // return invalid state.
        return ReaderNextState.InvalidDomainName
    }

    /**
     * Handles Hexadecimal, octal, decimal, dotted decimal, dotted hex, dotted octal.
     * @param testDomain the string we are testing
     * @return Returns true if it's a valid ipv4 address
     */
    private fun isValidIpv4(testDomain: String): Boolean {
        var valid = false
        if (testDomain.isNotEmpty()) {
            // handling format without dots. Ex: http://2123123123123/path/a, http://0x8242343/aksdjf
            if (_dots == 0) {
                valid = try {
                    val value: Long = if (testDomain.length > 2 && testDomain[0] == '0' && testDomain[1] == 'x') { // hex
                        testDomain.substring(2).toLong(16)
                    } else if (testDomain[0] == '0') { // octal
                        testDomain.substring(1).toLong(8)
                    } else { // decimal
                        testDomain.toLong()
                    }
                    value in MIN_NUMERIC_DOMAIN_VALUE..MAX_NUMERIC_DOMAIN_VALUE
                } catch (e: NumberFormatException) {
                    false
                }
            } else if (_dots == 3) {
                // Dotted decimal/hex/octal format
                val parts: Array<String> = testDomain.splitByDot()
                valid = true

                // check each part of the ip and make sure its valid.
                var i = 0
                while (i < parts.size && valid) {
                    val part = parts[i]
                    if (part.isNotEmpty()) {
                        var parsedNum: String
                        var base: Int
                        if (part.length > 2 && part[0] == '0' && part[1] == 'x') { // dotted hex
                            parsedNum = part.substring(2)
                            base = 16
                        } else if (part[0] == '0') { // dotted octal
                            parsedNum = part.substring(1)
                            base = 8
                        } else { // dotted decimal
                            parsedNum = part
                            base = 10
                        }
                        val section: Int = if (parsedNum.isEmpty()) {
                            0
                        } else {
                            try {
                                parsedNum.toInt(base)
                            } catch (e: NumberFormatException) {
                                return false
                            }
                        }
                        if (section < MIN_IP_PART || section > MAX_IP_PART) {
                            valid = false
                        }
                    } else {
                        valid = false
                    }
                    i++
                }
            }
        }
        return valid
    }

    /**
     * Sees that there's an open "[", and is now checking for ":"'s and stopping when there is a ']' or invalid character.
     * Handles ipv4 formatted ipv6 addresses, zone indices, truncated notation.
     * @return Returns true if it is a valid ipv6 address
     */
    private fun isValidIpv6(testDomain: String): Boolean {
        val domainArray = testDomain.toCharArray()

        // Return false if we don't see [....]
        // or if we only have '[]'
        // or if we detect [:8000: ...]; only [::8000: ...] is okay
        if ((domainArray.size < 3 || domainArray[domainArray.size - 1] != ']' || domainArray[0] != '[' || domainArray[1] == ':') && domainArray[2] != ':') {
            return false
        }
        var numSections = 1
        var hexDigits = 0
        var prevChar = 0.toChar()

        // used to check ipv4 addresses at the end of ipv6 addresses.
        val lastSection = StringBuilder()
        var hexSection = true

        // If we see a '%'. Example: http://[::ffff:0xC0.0x00.0x02.0xEB%251]
        var zoneIndicesMode = false

        // If doubleColonFlag is true, that means we've already seen one "::"; we're not allowed to have more than one.
        var doubleColonFlag = false
        var index = 0
        while (index < domainArray.size) {
            when (domainArray[index]) {
                '[' -> {}
                '%', ']' -> {
                    if (domainArray[index] == '%') {
                        // see if there's a urlencoded dot
                        if (domainArray.size - index >= 2 && domainArray[index + 1] == '2' && domainArray[index + 2] == 'e') {
                            lastSection.append("%2e")
                            index += 2
                            hexSection = false
                            break
                        }
                        zoneIndicesMode = true
                    }
                    if (!hexSection && (!zoneIndicesMode || domainArray[index] == '%')) {
                        if (isValidIpv4(lastSection.toString())) {
                            numSections++ // ipv4 takes up 2 sections.
                        } else {
                            return false
                        }
                    }
                }
                ':' -> {
                    if (prevChar == ':') {
                        if (doubleColonFlag) { // only allowed to have one "::" in an ipv6 address.
                            return false
                        }
                        doubleColonFlag = true
                    }

                    // This means that we reached invalid characters in the previous section
                    if (!hexSection) {
                        return false
                    }
                    hexSection = true // reset hex to true
                    hexDigits = 0 // reset count for hex digits
                    numSections++
                    lastSection.delete(0, lastSection.length) // clear last section
                }
                else -> if (zoneIndicesMode) {
                    if (domainArray[index].isUnreserved().not()) {
                        return false
                    }
                } else {
                    lastSection.append(domainArray[index]) // collect our possible ipv4 address
                    if (hexSection && domainArray[index].isHex()) {
                        hexDigits++
                    } else {
                        hexSection = false // non hex digit.
                    }
                }
            }
            if (hexDigits > 4 || numSections > 8) {
                return false
            }
            prevChar = domainArray[index]
            index++
        }

        // numSections != 1 checks for things like: [adf]
        // If there are more than 8 sections for the address or there isn't a double colon, then it's invalid.
        return numSections != 1 && (numSections >= 8 || doubleColonFlag)
    }

    companion object {
        /**
         * The minimum length of a ascii based top level domain.
         */
        private const val MIN_TOP_LEVEL_DOMAIN = 2

        /**
         * The maximum length of a ascii based top level domain.
         */
        private const val MAX_TOP_LEVEL_DOMAIN = 22

        /**
         * The maximum number that the url can be in a url that looks like:
         * http://123123123123/path
         */
        private const val MAX_NUMERIC_DOMAIN_VALUE = 4294967295L

        /**
         * The minimum number the url can be in a url that looks like:
         * http://123123123123/path
         */
        private const val MIN_NUMERIC_DOMAIN_VALUE = 16843008L

        /**
         * If the domain name is an ip address, for each part of the address, what's the minimum value?
         */
        private const val MIN_IP_PART = 0

        /**
         * If the domain name is an ip address, for each part of the address, what's the maximum value?
         */
        private const val MAX_IP_PART = 255

        /**
         * The start of the utf character code table which indicates that this character is an international character.
         * Everything below this value is either a-z, A-Z, 0-9 or symbols that are not included in domain name.
         */
        private const val INTERNATIONAL_CHAR_START = 192

        /**
         * The maximum length of each label in the domain name.
         */
        private const val MAX_LABEL_LENGTH = 64

        /**
         * The maximum number of labels in a single domain name.
         */
        private const val MAX_NUMBER_LABELS = 127

        /**
         * The maximum domain name length.
         */
        private const val MAX_DOMAIN_LENGTH = 255

        /**
         * Encoded hex dot.
         */
        private const val HEX_ENCODED_DOT = "2e"
    }
}
