package `in`.technowolf.linksDetekt

import java.net.MalformedURLException

/**
 * Returns a normalized version of a url instead of the original url string.
 */
class NormalizedUrl(urlMarker: UrlMarker) : Url(urlMarker) {
    private var _isPopulated = false
    private var _hostBytes: ByteArray? = null
    override val host: String?
        get() {
            if (rawHost == null) {
                populateHostAndHostBytes()
            }
            return rawHost
        }
    override val path: String?
        get() {
            if (rawPath == null) {
                rawPath = PathNormalizer().normalizePath(super.path)
            }
            return rawPath
        }

    /**
     * Returns the byte representation of the ip address. If the host is not an ip address, it returns null.
     */
    override val hostBytes: ByteArray?
        get() {
            if (_hostBytes == null) {
                populateHostAndHostBytes()
            }
            return _hostBytes
        }

    private fun populateHostAndHostBytes() {
        if (!_isPopulated) {
            val hostNormalizer = HostNormalizer(super.host)
            rawHost = hostNormalizer.normalizedHost
            _hostBytes = hostNormalizer.bytes
            _isPopulated = true
        }
    }

    companion object {
        /**
         * Returns a normalized url given a single url.
         */
        @Throws(MalformedURLException::class)
        fun create(url: String): NormalizedUrl {
            return Url.create(url).normalize()
        }
    }
}
