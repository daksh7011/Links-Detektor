package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.Url

/**
 * Tracks the positions of different URL parts within a string.
 * This class is used during URL parsing to mark where each component of a URL begins,
 * allowing for efficient extraction and manipulation of URL parts.
 */
internal class UrlMarker {
    /**
     * The original URL string that this marker is tracking.
     */
    var originalUrl: String? = null
    private var _schemeIndex = -1
    private var _usernamePasswordIndex = -1
    private var _hostIndex = -1
    private var _portIndex = -1
    private var _pathIndex = -1
    private var _queryIndex = -1
    private var _fragmentIndex = -1

    /**
     * Creates a new Url object using the current marker positions.
     *
     * @return A new Url instance initialized with this marker.
     */
    fun createUrl(): Url {
        return Url(this)
    }

    /**
     * Sets the starting index for a specific URL part.
     *
     * @param urlPart The URL part to set the index for.
     * @param index The starting position of the URL part in the original string.
     */
    fun setIndex(urlPart: UrlPart?, index: Int) {
        when (urlPart) {
            UrlPart.SCHEME -> _schemeIndex = index
             UrlPart.USERNAME_PASSWORD -> _usernamePasswordIndex = index
             UrlPart.HOST -> _hostIndex = index
             UrlPart.PORT -> _portIndex = index
             UrlPart.PATH -> _pathIndex = index
             UrlPart.QUERY -> _queryIndex = index
             UrlPart.FRAGMENT -> _fragmentIndex = index
            else -> {}
        }
    }

    /**
     * Gets the starting index of a specific URL part in the original string.
     *
     * @param urlPart The URL part to get the index for.
     * @return The starting position of the URL part in the original string, or -1 if the part doesn't exist.
     */
    fun indexOf(urlPart: UrlPart?): Int {
        return when (urlPart) {
            UrlPart.SCHEME -> _schemeIndex
            UrlPart.USERNAME_PASSWORD -> _usernamePasswordIndex
            UrlPart.HOST -> _hostIndex
            UrlPart.PORT -> _portIndex
            UrlPart.PATH -> _pathIndex
            UrlPart.QUERY -> _queryIndex
            UrlPart.FRAGMENT -> _fragmentIndex
            else -> -1
        }
    }

    /**
     * Removes the index for a specific URL part by setting it to -1.
     *
     * @param urlPart The URL part to unset the index for.
     */
    fun unsetIndex(urlPart: UrlPart?) {
        setIndex(urlPart, -1)
    }

    /**
     * Sets all URL part indices at once using an array.
     * This is primarily used in testing to set indices more easily.
     *
     * @param indices Array of indices for all URL parts. Must be of size 7, with indices in the order:
     *                [SCHEME, USERNAME_PASSWORD, HOST, PORT, PATH, QUERY, FRAGMENT]
     * @return This UrlMarker instance for method chaining.
     * @throws IllegalArgumentException If the indices array is null or not of size 7.
     */
    fun setIndices(indices: IntArray?): UrlMarker {
        require(!(indices == null || indices.size != 7)) { "Malformed index array." }
        setIndex(UrlPart.SCHEME, indices[0])
        setIndex(UrlPart.USERNAME_PASSWORD, indices[1])
        setIndex(UrlPart.HOST, indices[2])
        setIndex(UrlPart.PORT, indices[3])
        setIndex(UrlPart.PATH, indices[4])
        setIndex(UrlPart.QUERY, indices[5])
        setIndex(UrlPart.FRAGMENT, indices[6])
        return this
    }
}
