package com.slothiesmooth.linksdetektor

import com.slothiesmooth.linksdetektor.internal.UrlMarker
import com.slothiesmooth.linksdetektor.internal.UrlPart
import com.slothiesmooth.linksdetektor.internal.UrlUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList

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
 * - Keeps the fragment in [fullUrl] (anything after #); [fullUrlWithoutFragment] leaves it out
 *
 */
public open class Url internal constructor(urlMarker: UrlMarker) {
    private val _urlMarker: UrlMarker = urlMarker
    private var _scheme: String? = null
    private var _username: String? = null
    private var _password: String? = null
    internal var rawHost: String? = null
    private var _port = 0
    internal var rawPath: String? = null
    private var _query: String? = null
    private var _fragment: String? = null

    /**
     * The original, unmodified URL string that was used to create this Url object.
     * This is useful for debugging or when the original form needs to be preserved.
     */
    public val originalUrl: String? = urlMarker.originalUrl

    /**
     * Where this URL was read from in the scanned string, as an index range.
     *
     * `range.first` is the index of the first character the URL was read from and `range.last + 1` is the index
     * just past the last one, so `content.substring(range)` recovers the source text of the URL. Ranges
     * returned by one [LinksDetektor.detect] call are ascending and never overlap.
     *
     * `content.substring(range)` equals [originalUrl] (with `\n`, `\t` and `\r` read as spaces), except when the
     * detector dropped a stray `%` from the URL: the range then covers the whole source span, including it.
     * For [Url.create] the range is relative to the sanitised string that was scanned (trimmed, spaces replaced
     * by `%20`, whitespace removed), not to the argument. If the URL cannot be located in the scanned string
     * (not expected for a URL produced by this library), the range is [IntRange.EMPTY].
     * A [NormalizedUrl] has the range of the [Url] it came from.
     */
    public val range: IntRange = urlMarker.range

    internal val urlMarker: UrlMarker
        get() = _urlMarker

    /**
     * Gets the complete URL string including all components.
     *
     * This property returns a formatted URL string that includes all parts of the URL,
     * including the fragment (if present).
     *
     * If the host is absent (or, for a [NormalizedUrl], could not be normalised) the host as written in the
     * original URL is used, and an empty string if there is none; the text `null` never appears.
     *
     * @return A string in the format: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]#[fragment]
     */
    public val fullUrl: String
        get() = fullUrlWithoutFragment + fragment.orEmpty()

    /**
     * Gets the URL string without the fragment component.
     *
     * This property returns a formatted URL string that includes all parts of the URL
     * except for the fragment.
     *
     * @return A string in the format: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]
     */
    public val fullUrlWithoutFragment: String
        get() {
            val url = StringBuilder()
            if (scheme.isNotEmpty()) {
                url.append(scheme)
                url.append(":")
            }
            url.append("//")
            if (username.isNotEmpty()) {
                url.append(username)
                if (password.isNotEmpty()) {
                    url.append(":")
                    url.append(password)
                }
                url.append("@")
            }
            url.append(renderedHost.orEmpty())
            if (port > 0 && port != defaultPort()) {
                url.append(":")
                url.append(port)
            }
            url.append(path)
            url.append(query)
            return url.toString()
        }

    /**
     * Gets the scheme component of the URL (e.g., "http", "https", "ftp").
     *
     * If the URL doesn't explicitly specify a scheme and doesn't start with "//",
     * the default scheme "http" is returned.
     *
     * @return The scheme of the URL, or an empty string if not present.
     */
    public val scheme: String
        get() {
            if (_scheme == null) {
                if (exists(UrlPart.SCHEME)) {
                    val schemePart = getPart(UrlPart.SCHEME).orEmpty()
                    val colon = schemePart.indexOf(":")
                    // The detector also accepts a percent-encoded colon, as in "http%3a//".
                    val index = if (colon != -1) colon else schemePart.indexOf(ENCODED_COLON, ignoreCase = true)
                    _scheme = if (index != -1) schemePart.substring(0, index) else schemePart
                } else if (originalUrl?.startsWith("//") != true) {
                    _scheme = DEFAULT_SCHEME
                }
            }
            return _scheme.orEmpty()
        }

    /**
     * Gets the username component of the URL, if present.
     *
     * This property is lazily populated when first accessed by parsing
     * the USERNAME_PASSWORD part of the URL.
     *
     * @return The username from the URL, or an empty string if not present.
     */
    public val username: String
        get() {
            if (_username == null) {
                populateUsernamePassword()
            }
            return _username.orEmpty()
        }

    /**
     * Gets the password component of the URL, if present.
     *
     * This property is lazily populated when first accessed by parsing
     * the USERNAME_PASSWORD part of the URL.
     *
     * @return The password from the URL, or an empty string if not present.
     */
    public val password: String
        get() {
            if (_password == null) {
                populateUsernamePassword()
            }
            return _password.orEmpty()
        }

    /**
     * Gets the host component of the URL (domain name or IP address).
     *
     * This property is lazily populated when first accessed. If a port is specified
     * in the URL, it's excluded from the host value.
     *
     * @return The host from the URL, or null if not present.
     */
    public open val host: String?
        get() {
            if (rawHost == null) {
                val hostPart = getPart(UrlPart.HOST)
                rawHost = if (hostPart != null && exists(UrlPart.PORT)) {
                    hostPart.substring(0, hostPart.length - 1)
                } else {
                    hostPart
                }
            }
            return rawHost
        }

    /**
     * The host as written into [fullUrl]. This is [host], except that a [NormalizedUrl] whose host could not
     * be normalised renders the host as it appears in the original URL.
     */
    internal open val renderedHost: String?
        get() = host

    /**
     * Gets the port of the URL.
     *
     * @return The port written in the URL; otherwise the default port of the scheme (80 for http, 443 for https,
     * 21 for ftp); `-1` if there is neither, or the written port is not a valid number or is above 65535 (such a
     * port is treated as no port, and [fullUrl] omits it). A written port of `0` is returned as `0` and is not
     * rendered into [fullUrl].
     */
    public val port: Int
        get() {
            if (_port == 0) {
                val portString = getPart(UrlPart.PORT)
                _port = if (portString != null && !portString.isEmpty()) {
                    try {
                        portString.toInt().takeIf { it <= MAX_PORT } ?: -1
                    } catch (e: NumberFormatException) {
                        -1
                    }
                } else {
                    defaultPort() ?: -1
                }
            }
            return _port
        }

    /**
     * Gets the path component of the URL.
     *
     * This property is lazily populated when first accessed. If no path is specified
     * in the URL, a default path of "/" is returned.
     *
     * @return The path from the URL, or "/" if not present.
     */
    public open val path: String?
        get() {
            if (rawPath == null) {
                rawPath = if (exists(UrlPart.PATH)) getPart(UrlPart.PATH) else "/"
            }
            return rawPath
        }

    /**
     * Gets the query component of the URL (the part after '?').
     *
     * This property is lazily populated when first accessed.
     *
     * @return The query string from the URL, or an empty string if not present.
     */
    public val query: String
        get() {
            if (_query == null) {
                _query = getPart(UrlPart.QUERY)
            }
            return _query.orEmpty()
        }

    /**
     * Gets the fragment component of the URL (the part after '#').
     *
     * This property is lazily populated when first accessed.
     *
     * @return The fragment string from the URL, or an empty string if not present.
     */
    public val fragment: String
        get() {
            if (_fragment == null) {
                _fragment = getPart(UrlPart.FRAGMENT)
            }
            return _fragment.orEmpty()
        }

    /**
     * Always returns null for non normalized urls.
     * Subclasses like NormalizedUrl may override this to provide the byte representation.
     *
     * @return The byte representation of the host if it's an IP address, or null for non-normalized URLs.
     */
    public open val hostBytes: ByteArray?
        get() = null

    /**
     * Parses and populates the username and password fields from the URL.
     *
     * This method is called lazily when the username or password properties are accessed.
     * It extracts the username and password from the USERNAME_PASSWORD part of the URL
     * if it exists.
     */
    /** The default port of [scheme], whatever its case, or null if the scheme has none. */
    private fun defaultPort(): Int? = SCHEME_PORT_MAP[scheme.lowercase()]

    private fun populateUsernamePassword() {
        val usernamePassword = getPart(UrlPart.USERNAME_PASSWORD) ?: return
        val usernamePasswordParts =
            usernamePassword.substring(0, usernamePassword.length - 1).split(':')
                .dropLastWhile { it.isEmpty() }
                .toImmutableList()
        if (usernamePasswordParts.size == 1) {
            _username = usernamePasswordParts[0]
        } else if (usernamePasswordParts.size == 2) {
            _username = usernamePasswordParts[0]
            _password = usernamePasswordParts[1]
        }
    }

    /**
     * Checks if a specific URL part exists in this URL.
     *
     * @param urlPart The URL part to check for existence.
     * @return True if the part exists in this URL, false otherwise.
     */
    private fun exists(urlPart: UrlPart?): Boolean = urlPart != null && _urlMarker.indexOf(urlPart) >= 0

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
        val source = originalUrl ?: return null
        val nextPart: UrlPart = nextExistingPart(part) ?: return source.substring(_urlMarker.indexOf(part))
        return source.substring(_urlMarker.indexOf(part), _urlMarker.indexOf(nextPart))
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
    public fun normalize(): NormalizedUrl = NormalizedUrl(_urlMarker)

    /**
     * Returns a string representation of this URL.
     *
     * This method returns the full URL including all components (scheme, username, password,
     * host, port, path, query, and fragment).
     *
     * @return The string representation of this URL.
     */
    override fun toString(): String = fullUrl

    /**
     * Companion object providing factory methods
     */
    public companion object {
        private const val MAX_PORT = 65535
        private const val DEFAULT_SCHEME = "http"
        private const val ENCODED_COLON = "%3a"
        private val SCHEME_PORT_MAP: ImmutableMap<String, Int> =
            persistentMapOf("http" to 80, "https" to 443, "ftp" to 21)

        /**
         * Creates a Url object from a string representation.
         *
         * This method parses the input string to extract a single URL. It handles various URL formats
         * and performs basic preprocessing like trimming and space replacement.
         *
         * @param url The URL string to parse.
         * @return A new Url instance representing the parsed URL.
         * @throws MalformedUrlException If the input string is not a valid URL, contains no URLs,
         *                               or contains multiple URLs.
         */
        @Throws(MalformedUrlException::class)
        public fun create(url: String): Url {
            val formattedString: String = UrlUtil.removeSpecialSpaces(url.trim { it <= ' ' }.replace(" ", "%20"))
            val urls: ImmutableList<Url> = LinksDetektor(formattedString, LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect()
            return when (urls.size) {
                1 -> urls[0]
                0 -> throw MalformedUrlException("We couldn't find any urls in string: $url")
                else -> throw MalformedUrlException("We found more than one url in string: $url")
            }
        }
    }
}
