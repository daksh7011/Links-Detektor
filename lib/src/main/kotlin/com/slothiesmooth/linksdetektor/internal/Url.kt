package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.LinksDetektor
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import org.apache.commons.lang3.StringUtils
import java.net.MalformedURLException

/**
 * Creating own Uri class since java.net.Uri would throw parsing exceptions
 * for URL's considered ok by browsers.
 *
 * Also to avoid further conflict, this does stuff that the normal Uri object doesn't do:
 * - Converts http://google.com/a/b/.//./../c to http://google.com/a/c
 * - Decodes repeatedly so that http://host/%2525252525252525 becomes http://host/%25 while normal decoders
 * would make it http://host/%25252525252525 (one less 25)
 * - Removes tabs and new lines: http://www.google.com/foo\tbar\rbaz\n2 becomes "http://www.google.com/foobarbaz2"
 * - Converts IP addresses: http://3279880203/blah becomes http://195.127.0.11/blah
 * - Strips fragments (anything after #)
 *
 */
open class Url internal constructor(urlMarker: UrlMarker) {
    private val _urlMarker: UrlMarker
    private var _scheme: String? = null
    private var _username: String? = null
    private var _password: String? = null
    protected var rawHost: String? = null
    private var _port = 0
    protected var rawPath: String? = null
    private var _query: String? = null
    private var _fragment: String? = null

    /**
     * The original, unmodified URL string that was used to create this Url object.
     * This is useful for debugging or when the original form needs to be preserved.
     */
    val originalUrl: String?

    init {
        _urlMarker = urlMarker
        originalUrl = urlMarker.originalUrl
    }

    /**
     * Creates a normalized version of this URL.
     *
     * The normalized version provides canonical representations of URL components:
     * - Host names are converted to lowercase and properly encoded
     * - IP addresses are converted to their standard representation
     * - Paths are normalized (resolving "./" and "../" segments)
     *
     * @return A new NormalizedUrl instance representing the normalized version of this URL.
     */
    fun normalize(): NormalizedUrl {
        return NormalizedUrl(_urlMarker)
    }

    /**
     * Returns a string representation of this URL.
     *
     * This method returns the full URL including all components (scheme, username, password,
     * host, port, path, query, and fragment).
     *
     * @return The string representation of this URL.
     */
    override fun toString(): String {
        return fullUrl
    }

    /**
     * Gets the complete URL string including all components.
     *
     * This property returns a formatted URL string that includes all parts of the URL,
     * including the fragment (if present).
     *
     * @return A string in the format: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]#[fragment]
     */
    val fullUrl: String
        get() = fullUrlWithoutFragment + StringUtils.defaultString(fragment)

    /**
     * Gets the URL string without the fragment component.
     *
     * This property returns a formatted URL string that includes all parts of the URL
     * except for the fragment.
     *
     * @return A string in the format: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]
     */
    val fullUrlWithoutFragment: String
        get() {
            val url = StringBuilder()
            if (!StringUtils.isEmpty(scheme)) {
                url.append(scheme)
                url.append(":")
            }
            url.append("//")
            if (!StringUtils.isEmpty(username)) {
                url.append(username)
                if (!StringUtils.isEmpty(password)) {
                    url.append(":")
                    url.append(password)
                }
                url.append("@")
            }
            url.append(host)
            if (port > 0 && port != SCHEME_PORT_MAP[scheme]) {
                url.append(":")
                url.append(port)
            }
            url.append(path)
            url.append(query)
            return url.toString()
        }
    val scheme: String
        get() {
            if (_scheme == null) {
                if (exists(UrlPart.SCHEME)) {
                    _scheme = getPart(UrlPart.SCHEME)
                    val index = _scheme!!.indexOf(":")
                    if (index != -1) {
                        _scheme = _scheme!!.substring(0, index)
                    }
                } else if (!originalUrl!!.startsWith("//")) {
                    _scheme = DEFAULT_SCHEME
                }
            }
            return StringUtils.defaultString(_scheme)
        }
    val username: String
        get() {
            if (_username == null) {
                populateUsernamePassword()
            }
            return StringUtils.defaultString(_username)
        }
    val password: String
        get() {
            if (_password == null) {
                populateUsernamePassword()
            }
            return StringUtils.defaultString(_password)
        }
    open val host: String?
        get() {
            if (rawHost == null) {
                rawHost = getPart(UrlPart.HOST)
                if (exists(UrlPart.PORT)) {
                    rawHost = rawHost!!.substring(0, rawHost!!.length - 1)
                }
            }
            return rawHost
        }

    /**
     * port = 0 means it hasn't been set yet. port = -1 means there is no port
     */
    val port: Int
        get() {
            if (_port == 0) {
                val portString = getPart(UrlPart.PORT)
                _port = if (portString != null && !portString.isEmpty()) {
                    try {
                        portString.toInt()
                    } catch (e: NumberFormatException) {
                        -1
                    }
                } else if (SCHEME_PORT_MAP.containsKey(scheme)) {
                    SCHEME_PORT_MAP[scheme]!!
                } else {
                    -1
                }
            }
            return _port
        }
    open val path: String?
        get() {
            if (rawPath == null) {
                rawPath = if (exists(UrlPart.PATH)) getPart(UrlPart.PATH) else "/"
            }
            return rawPath
        }
    val query: String
        get() {
            if (_query == null) {
                _query = getPart(UrlPart.QUERY)
            }
            return StringUtils.defaultString(_query)
        }
    val fragment: String
        get() {
            if (_fragment == null) {
                _fragment = getPart(UrlPart.FRAGMENT)
            }
            return StringUtils.defaultString(_fragment)
        }

    /**
     * Always returns null for non normalized urls.
     * Subclasses like NormalizedUrl may override this to provide the byte representation.
     *
     * @return The byte representation of the host if it's an IP address, or null for non-normalized URLs.
     */
    open val hostBytes: ByteArray?
        get() = null

    /**
     * Parses and populates the username and password fields from the URL.
     *
     * This method is called lazily when the username or password properties are accessed.
     * It extracts the username and password from the USERNAME_PASSWORD part of the URL
     * if it exists.
     */
    private fun populateUsernamePassword() {
        if (exists(UrlPart.USERNAME_PASSWORD)) {
            val usernamePassword = getPart(UrlPart.USERNAME_PASSWORD)
            val usernamePasswordParts =
                usernamePassword!!.substring(0, usernamePassword.length - 1).split(":".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            if (usernamePasswordParts.size == 1) {
                _username = usernamePasswordParts[0]
            } else if (usernamePasswordParts.size == 2) {
                _username = usernamePasswordParts[0]
                _password = usernamePasswordParts[1]
            }
        }
    }

    /**
     * Checks if a specific URL part exists in this URL.
     *
     * @param urlPart The URL part to check for existence.
     * @return True if the part exists in this URL, false otherwise.
     */
    private fun exists(urlPart: UrlPart?): Boolean {
        return urlPart != null && _urlMarker.indexOf(urlPart) >= 0
    }

    /**
     * Finds the next existing URL part after the specified part.
     *
     * This method traverses the URL structure to find the next part that exists in this URL.
     * For example, in "http://yahoo.com/lala/", nextExistingPart(UrlPart.HOST) would return UrlPart.PATH.
     *
     * @param urlPart The current URL part.
     * @return The next existing URL part, or null if there are no more parts.
     */
    private fun nextExistingPart(urlPart: UrlPart): UrlPart? {
        val nextPart: UrlPart? = urlPart.nextPart
        return if (exists(nextPart)) {
            nextPart
        } else if (nextPart == null) {
            null
        } else {
            nextExistingPart(nextPart)
        }
    }

    /**
     * Extracts a specific part of the URL from the original string.
     *
     * This method uses the URL marker indices to extract the substring corresponding
     * to the requested URL part.
     *
     * @param part The URL part to extract (e.g., HOST, PATH, QUERY).
     * @return The extracted part as a string, or null if the part doesn't exist in this URL.
     */
    private fun getPart(part: UrlPart): String? {
        if (!exists(part)) {
            return null
        }
        val nextPart: UrlPart = nextExistingPart(part) ?: return originalUrl!!.substring(_urlMarker.indexOf(part))
        return originalUrl!!.substring(_urlMarker.indexOf(part), _urlMarker.indexOf(nextPart))
    }

    internal val urlMarker: UrlMarker
        get() = _urlMarker

    companion object {
        private const val DEFAULT_SCHEME = "http"
        private var SCHEME_PORT_MAP: HashMap<String, Int> = HashMap()

        init {
            SCHEME_PORT_MAP["http"] = 80
            SCHEME_PORT_MAP["https"] = 443
            SCHEME_PORT_MAP["ftp"] = 21
        }

        /**
         * Creates a Url object from a string representation.
         *
         * This method parses the input string to extract a single URL. It handles various URL formats
         * and performs basic preprocessing like trimming and space replacement.
         *
         * @param url The URL string to parse.
         * @return A new Url instance representing the parsed URL.
         * @throws java.net.MalformedURLException If the input string is not a valid URL, contains no URLs,
         *                               or contains multiple URLs.
         */
        @Throws(MalformedURLException::class)
        fun create(url: String): Url {
            val formattedString: String = UrlUtil.removeSpecialSpaces(url.trim { it <= ' ' }.replace(" ", "%20"))
            val urls: List<Url> = LinksDetektor(formattedString, LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect()
            return when (urls.size) {
                1 -> {
                    urls[0]
                }
                0 -> {
                    throw MalformedURLException("We couldn't find any urls in string: $url")
                }
                else -> {
                    throw MalformedURLException("We found more than one url in string: $url")
                }
            }
        }
    }
}
