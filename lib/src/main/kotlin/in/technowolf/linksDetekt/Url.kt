package `in`.technowolf.linksDetekt

import `in`.technowolf.linksDetekt.detector.LinksDetektor
import `in`.technowolf.linksDetekt.detector.LinksDetektorOptions
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
open class Url(urlMarker: UrlMarker) {
    private val _urlMarker: UrlMarker
    private var _scheme: String? = null
    private var _username: String? = null
    private var _password: String? = null
    protected var rawHost: String? = null
    private var _port = 0
    protected var rawPath: String? = null
    private var _query: String? = null
    private var _fragment: String? = null
    val originalUrl: String?

    init {
        _urlMarker = urlMarker
        originalUrl = urlMarker.originalUrl
    }

    /**
     * Returns a normalized url given a url object
     */
    fun normalize(): NormalizedUrl {
        return NormalizedUrl(_urlMarker)
    }

    override fun toString(): String {
        return fullUrl
    }

    /**
     * Note that this includes the fragment
     * @return Formats the url to: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]#[fragment]
     */
    val fullUrl: String
        get() = fullUrlWithoutFragment + StringUtils.defaultString(fragment)

    /**
     *
     * @return Formats the url to: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]
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
     */
    open val hostBytes: ByteArray?
        get() = null

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
     * @param urlPart The url part we are checking for existence
     * @return Returns true if the part exists.
     */
    private fun exists(urlPart: UrlPart?): Boolean {
        return urlPart != null && _urlMarker.indexOf(urlPart) >= 0
    }

    /**
     * For example, in http://yahoo.com/lala/, nextExistingPart(UrlPart.HOST) would return UrlPart.PATH
     * @param urlPart The current url part
     * @return Returns the next part; if there is no existing next part, it returns null
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
     * @param part The part that we want. Ex: host, path
     */
    private fun getPart(part: UrlPart): String? {
        if (!exists(part)) {
            return null
        }
        val nextPart: UrlPart = nextExistingPart(part) ?: return originalUrl!!.substring(_urlMarker.indexOf(part))
        return originalUrl!!.substring(_urlMarker.indexOf(part), _urlMarker.indexOf(nextPart))
    }

    protected val urlMarker: UrlMarker
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
         * Returns a url given a single url.
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
