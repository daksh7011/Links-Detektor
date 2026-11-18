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
}
