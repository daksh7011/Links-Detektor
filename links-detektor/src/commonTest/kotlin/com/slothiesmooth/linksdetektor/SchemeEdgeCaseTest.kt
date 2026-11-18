package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Schemes written in an unusual way.
 */
class SchemeEdgeCaseTest {

    @Test
    fun percentEncodedColonEndsTheScheme() {
        for (input in persistentListOf("http%3a//example.com/a", "http%3A//example.com/a")) {
            val url = LinksDetektor(input, LinksDetektorOptions.Default).detect().first()

            assertEquals("http", url.scheme, "case: $input")
            assertEquals("example.com", url.host, "case: $input")
            assertEquals(80, url.port, "case: $input")
            assertEquals("http://example.com/a", url.fullUrl, "case: $input")
        }
    }

    @Test
    fun defaultPortIgnoresSchemeCase() {
        val http = LinksDetektor("HTTP://example.com/a", LinksDetektorOptions.Default).detect().first()
        assertEquals(80, http.port)
        assertEquals("HTTP://example.com/a", http.fullUrl)

        val https = LinksDetektor("HtTpS://example.com:443/a", LinksDetektorOptions.Default).detect().first()
        assertEquals(443, https.port)
        assertEquals("HtTpS://example.com/a", https.fullUrl)

        val ftp = LinksDetektor("FTP://example.com", LinksDetektorOptions.Default).detect().first()
        assertEquals(21, ftp.port)
    }
}
