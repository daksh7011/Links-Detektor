package com.slothiesmooth.linksdetektor

import com.slothiesmooth.linksdetektor.internal.hex
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization tests: values captured from the JVM implementation before the KMP migration.
 */
class NormalizeGoldenTest {

    private data class NormalizeCase(
        val input: String,
        val host: String,
        val path: String,
        val bytesHex: String?,
        val fullUrl: String,
    )

    @Test
    fun normalizeGoldens() {
        persistentListOf(
            NormalizeCase("http://EXAMPLE.com/a/../b/./c//d?x=1#Frag", "example.com", "/b/c/d", null, "http://example.com/b/c/d?x=1#Frag"),
            NormalizeCase("http://b\u00FCcher.de/stra\u00DFe", "xn--bcher-kva.de", "/stra%DFe", null, "http://xn--bcher-kva.de/stra%DFe"),
            NormalizeCase("http://3232235521/", "192.168.0.1", "/", "00000000000000000000ffffc0a80001", "http://192.168.0.1/"),
            NormalizeCase("http://0xC0.0250.0.1:8080/a%2e%2e/b", "192.168.0.1", "/a../b", "00000000000000000000ffffc0a80001", "http://192.168.0.1:8080/a../b"),
            NormalizeCase("http://[2001:db8::1]:8080/a", "[2001:db8:0:0:0:0:0:1]", "/a", "20010db8000000000000000000000001", "http://[2001:db8:0:0:0:0:0:1]:8080/a"),
            NormalizeCase("http://[::ffff:192.168.0.1]/", "192.168.0.1", "/", "00000000000000000000ffffc0a80001", "http://192.168.0.1/"),
            NormalizeCase("https://user:pw@Example.COM:443/%7Efoo/", "example.com", "/~foo/", null, "https://user:pw@example.com/~foo/"),
            NormalizeCase("http://www.example.com./a%20b", "www.example.com", "/a%20b", null, "http://www.example.com/a%20b"),
            NormalizeCase("http://\u4F8B\u3048\u3002jp/\u4F8B", "xn--r8jz45g.jp", "/%8B", null, "http://xn--r8jz45g.jp/%8B"),
            NormalizeCase("ftp://Example.com:21/x", "example.com", "/x", null, "ftp://example.com/x"),
            NormalizeCase("http://example.com:80", "example.com", "/", null, "http://example.com/"),
            NormalizeCase("http://exa%6Dple.com/%41", "example.com", "/A", null, "http://example.com/A"),
            NormalizeCase("//Example.com/a/..", "example.com", "/", null, "//example.com/"),
            NormalizeCase("http://example.com/a/b/../../../c", "example.com", "/c", null, "http://example.com/c")
        ).forEach { case ->
            val normalized = Url.create(case.input).normalize()
            assertEquals(case.host, normalized.host, "case: ${case.input}")
            assertEquals(case.path, normalized.path, "case: ${case.input}")
            assertEquals(case.bytesHex, normalized.hostBytes.hex(), "case: ${case.input}")
            assertEquals(case.fullUrl, normalized.fullUrl, "case: ${case.input}")
        }
    }
}
