package `in`.technowolf.linksDetekt.detector

import `in`.technowolf.linksDetekt.Url
import `in`.technowolf.linksDetekt.UrlMarker
import `in`.technowolf.linksDetekt.UrlPart
import `in`.technowolf.linksDetekt.detector.CharExtensions.isAlpha
import `in`.technowolf.linksDetekt.detector.CharExtensions.isDot
import `in`.technowolf.linksDetekt.detector.CharExtensions.isHex
import `in`.technowolf.linksDetekt.detector.CharExtensions.isNumeric
import java.util.Collections
import java.util.Locale

/**
 * Creates a new UrlDetector object used to find urls inside of text.
 * @param content [String] The content to search inside.
 * @param options [LinksDetektorOptions] The UrlDetectorOptions to use when detecting the content.
 */
class LinksDetektor(content: String, options: LinksDetektorOptions) {
    /**
     * The response of character matching.
     */
    private enum class CharacterMatch {
        /**
         * The character was not matched.
         */
        CharacterNotMatched,

        /**
         * A character was matched with requires a stop.
         */
        CharacterMatchStop,

        /**
         * The character was matched which is a start of parentheses.
         */
        CharacterMatchStart
    }

    /**
     * Stores options for detection.
     */
    private val linksDetektorOptions: LinksDetektorOptions

    /**
     * The input stream to read.
     */
    private val inputTextReader: InputTextReader

    /**
     * Buffer to store temporary urls inside.
     */
    private val bufferStringBuilder = StringBuilder()

    /**
     * Has the scheme been found in this iteration?
     */
    private var hasScheme = false

    /**
     * If the first character in the url is a quote, then look for matching quote at the end.
     */
    private var quoteStart = false

    /**
     * If the first character in the url is a single quote, then look for matching quote at the end.
     */
    private var singleQuoteStart = false

    /**
     * If we see a '[', didn't find an ipv6 address, and the bracket option is on, then look for urls inside the brackets.
     */
    private var dontMatchIpv6 = false

    /**
     * Stores the found urls.
     */
    private val urlList = mutableListOf<Url>()

    /**
     * Keeps the count of special characters used to match quotes and different types of brackets.
     */
    private val characterMatchHashMap = HashMap<Char, Int>()

    /**
     * Keeps track of certain indices to create a Url object.
     */
    private var currentUrlMarker: UrlMarker = UrlMarker()

    /**
     * The states to use to continue writing or not.
     */
    enum class ReadEndState {
        /**
         * The current url is valid.
         */
        ValidUrl,

        /**
         * The current url is invalid.
         */
        InvalidUrl
    }

    init {
        inputTextReader = InputTextReader(content)
        linksDetektorOptions = options
    }

    /**
     * Gets the number of characters that were backtracked while reading the input. This is useful for performance
     * measurement.
     * @return The count of characters that were backtracked while reading.
     */
    val backtracked: Int
        get() = inputTextReader.backtrackedCount

    /**
     * Detects the urls and returns a list of detected url strings.
     * @return A list with detected urls.
     */
    fun detect(): List<Url> {
        readDefault()
        return urlList.toList()
    }

    /**
     * The default input reader which looks for specific flags to start detecting the url.
     */
    private fun readDefault() {
        // Keeps track of the number of characters read to be able to later cut out the domain name.
        var length = 0

        // until end of string read the contents
        while (inputTextReader.eof().not()) {
            // read the next char to process.
            when (val curr: Char = inputTextReader.read()) {
                ' ' -> {
                    // space was found, check if it's a valid single level domain.
                    if (
                        linksDetektorOptions.hasFlag(LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) &&
                        bufferStringBuilder.isNotEmpty() &&
                        hasScheme
                    ) {
                        inputTextReader.goBack()
                        readDomainName(bufferStringBuilder.substring(length))
                    }
                    bufferStringBuilder.append(curr)
                    readEnd(ReadEndState.InvalidUrl)
                    length = 0
                }
                '%' -> if (inputTextReader.canReadChars(2)) {
                    if (inputTextReader.peek(2).equals("3a", ignoreCase = true)) {
                        bufferStringBuilder.append(curr)
                        bufferStringBuilder.append(inputTextReader.read())
                        bufferStringBuilder.append(inputTextReader.read())
                        length = processColon(length)
                    } else if (
                        inputTextReader.peekChar(0).isHex() &&
                        inputTextReader.peekChar(1).isHex()
                    ) {
                        bufferStringBuilder.append(curr)
                        bufferStringBuilder.append(inputTextReader.read())
                        bufferStringBuilder.append(inputTextReader.read())
                        readDomainName(bufferStringBuilder.substring(length))
                        length = 0
                    }
                }
                '\u3002', '\uFF0E', '\uFF61', '.' -> {
                    bufferStringBuilder.append(curr)
                    readDomainName(bufferStringBuilder.substring(length))
                    length = 0
                }
                '@' -> if (bufferStringBuilder.isNotEmpty()) {
                    currentUrlMarker.setIndex(UrlPart.USERNAME_PASSWORD, length)
                    bufferStringBuilder.append(curr)
                    readDomainName(null)
                    length = 0
                }
                '[' -> {
                    if (dontMatchIpv6) {
                        // Check if we need to match characters. If we match characters and this is a start or stop of range,
                        // either way reset the world and start processing again.
                        if (checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
                            readEnd(ReadEndState.InvalidUrl)
                            length = 0
                        }
                    }
                    val beginning: Int = inputTextReader.position

                    // if it doesn't have a scheme, clear the buffer.
                    if (!hasScheme) {
                        bufferStringBuilder.delete(0, bufferStringBuilder.length)
                    }
                    bufferStringBuilder.append(curr)
                    if (!readDomainName(bufferStringBuilder.substring(length))) {
                        // if we didn't find an ipv6 address, then check inside the brackets for urls
                        inputTextReader.seek(beginning)
                        dontMatchIpv6 = true
                    }
                    length = 0
                }
                '/' -> // "/" was found, then we either read a scheme, or if we already read a scheme, then
                    // we are reading an url in the format http://123123123/asdf
                    if (hasScheme ||
                        linksDetektorOptions.hasFlag(LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) &&
                        bufferStringBuilder.length > 1
                    ) {
                        // we already have the scheme, so then we already read:
                        // http://something/ <- if something is all numeric then its a valid url.
                        // OR we are searching for single level domains. We have buffer length > 1 condition
                        // to weed out infinite backtrack in cases of html5 roots

                        // unread this "/" and continue to check the domain name starting from the beginning of the domain
                        inputTextReader.goBack()
                        readDomainName(bufferStringBuilder.substring(length))
                        length = 0
                    } else {

                        // we don't have a scheme already, then clear state, then check for html5 root such as: "//google.com/"
                        // remember the state of the quote when clearing state just in case its "//google.com" so its not cleared.
                        readEnd(ReadEndState.InvalidUrl)
                        bufferStringBuilder.append(curr)
                        hasScheme = readHtml5Root()
                        length = bufferStringBuilder.length
                    }
                ':' -> {
                    // add the ":" to the url and check for scheme/username
                    bufferStringBuilder.append(curr)
                    length = processColon(length)
                }
                else -> // Check if we need to match characters. If we match characters and this is a start or stop of range,
                    // either way reset the world and start processing again.
                    if (checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
                        readEnd(ReadEndState.InvalidUrl)
                        length = 0
                    } else {
                        bufferStringBuilder.append(curr)
                    }
            }
        }
        if (linksDetektorOptions.hasFlag(LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) &&
            bufferStringBuilder.isNotEmpty() &&
            hasScheme
        ) {
            readDomainName(bufferStringBuilder.substring(length))
        }
    }

    /**
     * We found a ":" and is now trying to read either scheme, username/password
     *
     * @param length first index of the previous part (could be beginning of the buffer, beginning of the
     * username/password, or beginning
     *
     * @return new index of where the domain starts
     */
    @Suppress("NAME_SHADOWING")
    private fun processColon(length: Int): Int {
        var length = length
        if (hasScheme) {
            // read it as username/password if it has scheme
            if (!readUserPass(length) && bufferStringBuilder.isNotEmpty()) {
                // unread the ":" so that the domain reader can process it
                inputTextReader.goBack()
                bufferStringBuilder.delete(bufferStringBuilder.length - 1, bufferStringBuilder.length)
                val backtrackOnFail: Int = inputTextReader.position - bufferStringBuilder.length + length
                if (!readDomainName(bufferStringBuilder.substring(length))) {
                    // go back to length location and restart search
                    inputTextReader.seek(backtrackOnFail)
                    readEnd(ReadEndState.InvalidUrl)
                }
                length = 0
            }
        } else if (readScheme() && bufferStringBuilder.isNotEmpty()) {
            hasScheme = true
            length = bufferStringBuilder.length // set length to be right after the scheme
        } else if (bufferStringBuilder.isNotEmpty() &&
            linksDetektorOptions.hasFlag(LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) &&
            inputTextReader.canReadChars(1)
        ) { // takes care of case like hi:
            inputTextReader.goBack() // unread the ":" so readDomainName can take care of the port
            bufferStringBuilder.delete(bufferStringBuilder.length - 1, bufferStringBuilder.length)
            readDomainName(bufferStringBuilder.toString())
        } else {
            readEnd(ReadEndState.InvalidUrl)
            length = 0
        }
        return length
    }

    /**
     * Gets the number of times the current character was seen in the document. Only special characters are tracked.
     * @param curr The character to look for.
     * @return The number of times that character was seen
     */
    private fun getCharacterCount(curr: Char): Int {
        val count = characterMatchHashMap[curr]
        return count ?: 0
    }

    /**
     * Increments the counter for the characters seen and return if this character matches a special character
     * that might require stopping reading the url.
     * @param curr The character to check.
     * @return The state that this character requires.
     */
    private fun checkMatchingCharacter(curr: Char): CharacterMatch {

        // This is a quote and we are matching quotes.
        if (curr == '\"' && linksDetektorOptions.hasFlag(LinksDetektorOptions.QUOTE_MATCH) || curr == '\'' && linksDetektorOptions.hasFlag(
                LinksDetektorOptions.SINGLE_QUOTE_MATCH
            )
        ) {
            val quoteStart: Boolean
            if (curr == '\"') {
                quoteStart = this.quoteStart

                // remember that a double quote was found.
                this.quoteStart = true
            } else {
                quoteStart = singleQuoteStart

                // remember that a single quote was found.
                singleQuoteStart = true
            }

            // increment the number of quotes found.
            val currVal = getCharacterCount(curr) + 1
            characterMatchHashMap[curr] = currVal

            // if there was already a quote found, or the number of quotes is even, return that we have to stop, else its a start.
            return if (quoteStart || currVal % 2 == 0) CharacterMatch.CharacterMatchStop else CharacterMatch.CharacterMatchStart
        } else if (linksDetektorOptions.hasFlag(LinksDetektorOptions.BRACKET_MATCH) && (curr == '[' || curr == '{' || curr == '(')) {
            // Look for start of bracket
            characterMatchHashMap[curr] = getCharacterCount(curr) + 1
            return CharacterMatch.CharacterMatchStart
        } else if (linksDetektorOptions.hasFlag(LinksDetektorOptions.XML) && curr == '<') {
            // If its html, look for "<"
            characterMatchHashMap[curr] = getCharacterCount(curr) + 1
            return CharacterMatch.CharacterMatchStart
        } else if (
            linksDetektorOptions.hasFlag(LinksDetektorOptions.BRACKET_MATCH) &&
            (curr == ']' || curr == '}' || curr == ')') ||
            linksDetektorOptions.hasFlag(LinksDetektorOptions.XML) && (curr == '>')
        ) {

            // If we catch a end bracket increment its count and get rid of not ipv6 flag
            val currVal = getCharacterCount(curr) + 1
            characterMatchHashMap[curr] = currVal

            // now figure out what the start bracket was associated with the closed bracket.
            var match = '\u0000'
            when (curr) {
                ']' -> match = '['
                '}' -> match = '{'
                ')' -> match = '('
                '>' -> match = '<'
                else -> {}
            }

            // If the number of open is greater then the number of closed, return a stop.
            return if (getCharacterCount(match) > currVal) CharacterMatch.CharacterMatchStop else CharacterMatch.CharacterMatchStart
        }

        // Nothing else was found.
        return CharacterMatch.CharacterNotMatched
    }

    /**
     * Checks if the url is in the format:
     * //google.com/static/js.js
     * @return True if the url is in this format and was matched correctly.
     */
    private fun readHtml5Root(): Boolean {
        // end of input then go away.
        if (inputTextReader.eof()) {
            return false
        }

        // read the next character. If its // then return true.
        val curr: Char = inputTextReader.read()
        if (curr == '/') {
            bufferStringBuilder.append(curr)
            return true
        } else {
            // if its not //, then go back and reset by 1 character.
            inputTextReader.goBack()
            readEnd(ReadEndState.InvalidUrl)
        }
        return false
    }

    /**
     * Reads the scheme and allows returns true if the scheme is http(s?):// or ftp(s?)://
     * @return True if the scheme was found, else false.
     */
    private fun readScheme(): Boolean {
        // Check if we are checking html and the length is longer than mailto:
        if (linksDetektorOptions.hasFlag(LinksDetektorOptions.HTML) && bufferStringBuilder.length >= HTML_MAILTO.length) {
            // Check if the string is actually mailto: then just return nothing.
            if (HTML_MAILTO.equals(
                    bufferStringBuilder.substring(bufferStringBuilder.length - HTML_MAILTO.length),
                    ignoreCase = true
                )
            ) {
                return readEnd(ReadEndState.InvalidUrl)
            }
        }
        val originalLength = bufferStringBuilder.length
        var numSlashes = 0
        while (!inputTextReader.eof()) {
            val curr: Char = inputTextReader.read()

            // if we match a slash, look for a second one.
            if (curr == '/') {
                bufferStringBuilder.append(curr)
                if (numSlashes == 1) {
                    // return only if its an approved protocol. This can be expanded to allow others
                    if (VALID_SCHEMES.contains(bufferStringBuilder.toString().lowercase(Locale.getDefault()))) {
                        currentUrlMarker.setIndex(UrlPart.SCHEME, 0)
                        return true
                    }
                    return false
                }
                numSlashes++
            } else if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
                // if we find a space or end of input, then nothing found.
                bufferStringBuilder.append(curr)
                return false
            } else if (curr == '[') { // if we're starting to see an ipv6 address
                inputTextReader.goBack() // unread the '[', so that we can start looking for ipv6
                return false
            } else if (originalLength > 0 || numSlashes > 0 || curr.isAlpha().not()) {
                // if it's not a character a-z or A-Z then assume we aren't matching scheme, but instead
                // matching username and password.
                inputTextReader.goBack()
                return readUserPass(0)
            }
        }
        return false
    }

    /**
     * Reads the input and looks for a username and password.
     * Handles:
     * http://username:password@...
     * @param beginningOfUsername Index of the buffer of where the username began
     * @return True if a valid username and password was found.
     */
    private fun readUserPass(beginningOfUsername: Int): Boolean {

        // The start of where we are.
        val start = bufferStringBuilder.length

        // keep looping until "done"
        var done = false

        // if we had a dot in the input, then it might be a domain name and not a username and password.
        var rollback = false
        while (!done && !inputTextReader.eof()) {
            val curr: Char = inputTextReader.read()

            // if we hit this, then everything is ok and we are matching a domain name.
            if (curr == '@') {
                bufferStringBuilder.append(curr)
                currentUrlMarker.setIndex(UrlPart.USERNAME_PASSWORD, beginningOfUsername)
                return readDomainName("")
            } else if (curr.isDot() || curr == '[') {
                // everything is still ok, just remember that we found a dot or '[' in case we might need to backtrack
                bufferStringBuilder.append(curr)
                rollback = true
            } else if (curr == '#' || curr == ' ' || curr == '/' || curr == ':' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
                // one of these characters indicates we are invalid state and should just return.
                rollback = true
                done = true
            } else {
                // all else, just append character assuming its ok so far.
                bufferStringBuilder.append(curr)
            }
        }
        return if (rollback) {
            // got to here, so there is no username and password. (We didn't find a @)
            val distance = bufferStringBuilder.length - start
            bufferStringBuilder.delete(start, bufferStringBuilder.length)
            val currIndex = (inputTextReader.position - distance - if (done) 1 else 0).coerceAtLeast(0)
            inputTextReader.seek(currIndex)
            false
        } else {
            readEnd(ReadEndState.InvalidUrl)
        }
    }

    /**
     * Try to read the current string as a domain name
     * @param current The current string used.
     * @return Whether the domain is valid or not.
     */
    private fun readDomainName(current: String?): Boolean {
        val hostIndex = if (current == null) bufferStringBuilder.length else bufferStringBuilder.length - current.length
        currentUrlMarker.setIndex(UrlPart.HOST, hostIndex)
        // create the domain name reader and specify the handler that will be called when a quote character
        // or something is found.
        val reader = DomainNameReader(
            inputTextReader,
            bufferStringBuilder,
            current,
            linksDetektorOptions,
            object : DomainNameReader.CharacterHandler {
                override fun addCharacter(character: Char) {
                    checkMatchingCharacter(character)
                }
            })

        // Try to read the dns and act on the response.
        return when (reader.readDomainName()) {
            DomainNameReader.ReaderNextState.ValidDomainName -> readEnd(ReadEndState.ValidUrl)
            DomainNameReader.ReaderNextState.ReadFragment -> readFragment()
            DomainNameReader.ReaderNextState.ReadPath -> readPath()
            DomainNameReader.ReaderNextState.ReadPort -> readPort()
            DomainNameReader.ReaderNextState.ReadQueryString -> readQueryString()
            else -> readEnd(ReadEndState.InvalidUrl)
        }
    }

    /**
     * Reads the fragments which is the part of the url starting with #
     * @return If a valid fragment was read true, else false.
     */
    private fun readFragment(): Boolean {
        currentUrlMarker.setIndex(UrlPart.FRAGMENT, bufferStringBuilder.length - 1)
        while (!inputTextReader.eof()) {
            val curr: Char = inputTextReader.read()

            // if it's the end or space, then a valid url was read.
            if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
                return readEnd(ReadEndState.ValidUrl)
            } else {
                // otherwise keep appending.
                bufferStringBuilder.append(curr)
            }
        }

        // if we are here, anything read is valid.
        return readEnd(ReadEndState.ValidUrl)
    }

    /**
     * Try to read the query string.
     * @return True if the query string was valid.
     */
    private fun readQueryString(): Boolean {
        currentUrlMarker.setIndex(UrlPart.QUERY, bufferStringBuilder.length - 1)
        while (!inputTextReader.eof()) {
            val curr: Char = inputTextReader.read()
            if (curr == '#') { // fragment
                bufferStringBuilder.append(curr)
                return readFragment()
            } else if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
                // end of query string
                return readEnd(ReadEndState.ValidUrl)
            } else { // all else add to buffer.
                bufferStringBuilder.append(curr)
            }
        }
        // a valid url was read.
        return readEnd(ReadEndState.ValidUrl)
    }

    /**
     * Try to read the port of the url.
     * @return True if a valid port was read.
     */
    private fun readPort(): Boolean {
        currentUrlMarker.setIndex(UrlPart.PORT, bufferStringBuilder.length)
        // The length of the port read.
        var portLen = 0
        while (!inputTextReader.eof()) {
            // read the next one and remember the length
            val curr: Char = inputTextReader.read()
            portLen++
            if (curr == '/') {
                // continue to read path
                bufferStringBuilder.append(curr)
                return readPath()
            } else if (curr == '?') {
                // continue to read query string
                bufferStringBuilder.append(curr)
                return readQueryString()
            } else if (curr == '#') {
                // continue to read fragment.
                bufferStringBuilder.append(curr)
                return readFragment()
            } else if (checkMatchingCharacter(curr) == CharacterMatch.CharacterMatchStop || curr.isNumeric().not()
            ) {
                // if we got here, then what we got so far is a valid url. don't append the current character.
                inputTextReader.goBack()

                // no port found; it was something like google.com:hello.world
                if (portLen == 1) {
                    // remove the ":" from the end.
                    bufferStringBuilder.delete(bufferStringBuilder.length - 1, bufferStringBuilder.length)
                }
                currentUrlMarker.unsetIndex(UrlPart.PORT)
                return readEnd(ReadEndState.ValidUrl)
            } else {
                // this is a valid character in the port string.
                bufferStringBuilder.append(curr)
            }
        }

        // found a correct url
        return readEnd(ReadEndState.ValidUrl)
    }

    /**
     * Tries to read the path
     * @return True if the path is valid.
     */
    private fun readPath(): Boolean {
        currentUrlMarker.setIndex(UrlPart.PATH, bufferStringBuilder.length - 1)
        while (!inputTextReader.eof()) {
            // read the next char
            val curr: Char = inputTextReader.read()
            if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
                // if end of state and we got here, then the url is valid.
                return readEnd(ReadEndState.ValidUrl)
            }

            // append the char
            bufferStringBuilder.append(curr)

            // now see if we move to another state.
            if (curr == '?') {
                // if ? read query string
                return readQueryString()
            } else if (curr == '#') {
                // if # read the fragment
                return readFragment()
            }
        }

        // end of input then this url is good.
        return readEnd(ReadEndState.ValidUrl)
    }

    /**
     * The url has been read to here. Remember the url if its valid, and reset state.
     * @param state The state indicating if this url is valid. If its valid it will be added to the list of urls.
     * @return True if the url was valid.
     */
    private fun readEnd(state: ReadEndState): Boolean {
        // if the url is valid and greater then 0
        if (state == ReadEndState.ValidUrl && bufferStringBuilder.isNotEmpty()) {
            // get the last character. if its a quote, cut it off.
            val len = bufferStringBuilder.length
            if (quoteStart && bufferStringBuilder[len - 1] == '\"') {
                bufferStringBuilder.delete(len - 1, len)
            }

            // Add the url to the list of good urls.
            if (bufferStringBuilder.isNotEmpty()) {
                currentUrlMarker.originalUrl = bufferStringBuilder.toString()
                urlList.add(currentUrlMarker.createUrl())
            }
        }

        // clear out the buffer.
        bufferStringBuilder.delete(0, bufferStringBuilder.length)

        // reset the state of internal objects.
        quoteStart = false
        hasScheme = false
        dontMatchIpv6 = false
        currentUrlMarker = UrlMarker()

        // return true if valid.
        return state == ReadEndState.ValidUrl
    }

    companion object {
        /**
         * Contains the string to check for and remove if the scheme is this.
         */
        private const val HTML_MAILTO = "mailto:"

        /**
         * Valid protocol schemes.
         */
        private val VALID_SCHEMES = Collections.unmodifiableSet(
            HashSet(
                listOf(
                    "http://",
                    "https://",
                    "ftp://",
                    "ftps://",
                    "http%3a//",
                    "https%3a//",
                    "ftp%3a//",
                    "ftps%3a//",
                )
            )
        )
    }
}
