package com.slothiesmooth.linksdetektor

import com.slothiesmooth.linksdetektor.internal.HostNormalizer
import com.slothiesmooth.linksdetektor.internal.PathNormalizer
import com.slothiesmooth.linksdetektor.internal.UrlMarker

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
public class NormalizedUrl internal constructor(urlMarker: UrlMarker) : Url(urlMarker) {
    private var _isPopulated = false
    private var _hostBytes: ByteArray? = null
    private var _unnormalizedHost: String? = null

    /**
     * Gets the normalized host part of the URL.
     *
     * The host is normalized using [com.slothiesmooth.linksdetektor.internal.HostNormalizer] which handles:
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
     * The host as it appears in [fullUrl]: the normalised host, or the host as written in the original URL
     * when normalisation failed (so [fullUrl] never contains `null` as a host).
     */
    override val renderedHost: String?
        get() = host ?: _unnormalizedHost

    /**
     * Gets the normalized path part of the URL.
     *
     * The path is normalized using [com.slothiesmooth.linksdetektor.internal.PathNormalizer] which:
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
     * @return A copy of the byte array representing the IP address (changing it does not affect later calls),
     * or null if the host is not an IP address.
     */
    override val hostBytes: ByteArray?
        get() {
            if (_hostBytes == null) {
                populateHostAndHostBytes()
            }
            return _hostBytes?.copyOf()
        }

    /**
     * Populates the normalized host and host bytes using the HostNormalizer.
     * This method is called lazily when the host or hostBytes properties are accessed.
     */
    private fun populateHostAndHostBytes() {
        if (!_isPopulated) {
            val original = super.host
            _unnormalizedHost = original
            val hostNormalizer = HostNormalizer(original)
            rawHost = hostNormalizer.normalizedHost
            _hostBytes = hostNormalizer.bytes
            _isPopulated = true
        }
    }

    internal companion object {
        /**
         * Creates a normalized URL from a string representation.
         *
         * This is a convenience method that creates a regular URL and then normalizes it.
         *
         * @param url The URL string to normalize.
         * @return A new NormalizedUrl instance.
         * @throws MalformedUrlException If the input string is not a valid URL or contains multiple URLs.
         */
        @Throws(MalformedUrlException::class)
        fun create(url: String): NormalizedUrl = Url.Companion.create(url).normalize()
    }
}
