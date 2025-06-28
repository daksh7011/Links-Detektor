package com.slothiesmooth.linksdetektor.internal

import java.net.MalformedURLException

/**
 * Represents a normalized version of a URL.
 *
 * This class extends the base [Url] class to provide normalized versions of URL components:
 * - Host names are normalized to their canonical form (e.g., converting to lowercase, handling IDNs)
 * - IP addresses are converted to their standard representation
 * - Paths are normalized (e.g., resolving "./" and "../" segments)
 *
 * Normalization helps with URL comparison and ensures consistent representation.
 */
class NormalizedUrl internal constructor(urlMarker: UrlMarker) : Url(urlMarker) {
    private var _isPopulated = false
    private var _hostBytes: ByteArray? = null

    /**
     * Gets the normalized host part of the URL.
     *
     * The host is normalized using [HostNormalizer] which handles:
     * - Converting to lowercase
     * - Handling IDN (Internationalized Domain Names)
     * - Converting IP addresses to their canonical form
     *
     * @return The normalized host string, or null if the host part doesn't exist or couldn't be normalized.
     */
    override val host: String?
        get() {
            if (rawHost == null) {
                populateHostAndHostBytes()
            }
            return rawHost
        }

    /**
     * Gets the normalized path part of the URL.
     *
     * The path is normalized using [PathNormalizer] which:
     * - Resolves relative path segments (e.g., "./" and "../")
     * - Removes duplicate slashes
     * - Decodes and re-encodes special characters
     *
     * @return The normalized path string, or null if the path part doesn't exist.
     */
    override val path: String?
        get() {
            if (rawPath == null) {
                rawPath = PathNormalizer().normalizePath(super.path)
            }
            return rawPath
        }

    /**
     * Gets the byte representation of the host if it's an IP address.
     *
     * This is useful for efficient IP address comparison and manipulation.
     *
     * @return The byte array representing the IP address, or null if the host is not an IP address.
     */
    override val hostBytes: ByteArray?
        get() {
            if (_hostBytes == null) {
                populateHostAndHostBytes()
            }
            return _hostBytes
        }

    /**
     * Populates the normalized host and host bytes using the HostNormalizer.
     * This method is called lazily when the host or hostBytes properties are accessed.
     */
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
         * Creates a normalized URL from a string representation.
         *
         * This is a convenience method that creates a regular URL and then normalizes it.
         *
         * @param url The URL string to normalize.
         * @return A new NormalizedUrl instance.
         * @throws java.net.MalformedURLException If the input string is not a valid URL or contains multiple URLs.
         */
        @Throws(MalformedURLException::class)
        fun create(url: String): NormalizedUrl {
            return Url.Companion.create(url).normalize()
        }
    }
}
