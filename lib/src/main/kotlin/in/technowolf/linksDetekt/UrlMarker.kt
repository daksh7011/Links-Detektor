package `in`.technowolf.linksDetekt

class UrlMarker {
    var originalUrl: String? = null
    private var _schemeIndex = -1
    private var _usernamePasswordIndex = -1
    private var _hostIndex = -1
    private var _portIndex = -1
    private var _pathIndex = -1
    private var _queryIndex = -1
    private var _fragmentIndex = -1
    fun createUrl(): Url {
        return Url(this)
    }

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
     * @param urlPart The part you want the index of
     * @return Returns the index of the part
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

    fun unsetIndex(urlPart: UrlPart?) {
        setIndex(urlPart, -1)
    }

    /**
     * This is used in TestUrlMarker to set indices more easily.
     * @param indices array of indices of size 7
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