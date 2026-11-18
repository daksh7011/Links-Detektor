package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

/** Lowercase hex, two digits per byte; `null` for `null`. */
internal fun ByteArray?.hex(): String? =
    this?.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

/**
 * Characterization tests: values captured from the JVM implementation before the KMP migration.
 */
class HostNormalizerGoldenTest {

    @Test
    fun hostGoldens() {
        persistentListOf(
            Triple(null, null, null),
            Triple("", null, null),
            Triple("example.com", "example.com", null),
            Triple("EXAMPLE.Com", "example.com", null),
            Triple("www.Example.COM", "www.example.com", null),
            Triple("TITLE.com", "title.com", null),
            Triple("b\u00FCcher.de", "xn--bcher-kva.de", null),
            Triple("B\u00DCCHER.de", "xn--bcher-kva.de", null),
            Triple("\u4F8B\u3048.jp", "xn--r8jz45g.jp", null),
            Triple("\u4F8B\u3048\u3002jp", "xn--r8jz45g.jp", null),
            Triple("\u4F8B\u3048\uFF0Ejp", "xn--r8jz45g.jp", null),
            Triple("\u4F8B\u3048\uFF61jp", "xn--r8jz45g.jp", null),
            Triple("stra\u00DFe.de", "strasse.de", null),
            Triple("xn--bcher-kva.de", "xn--bcher-kva.de", null),
            Triple("\u03B1\u03B2\u03B3.gr", "xn--mxacd.gr", null),
            Triple("\uFF45\uFF58\uFF41\uFF4D\uFF50\uFF4C\uFF45.com", "example.com", null),
            Triple("\u2168.com", "ix.com", null),
            Triple("\u0130stanbul.com", "xn--istanbul-o0e.com", null),
            Triple("caf\u00E9.fr", "xn--caf-dma.fr", null),
            Triple("cafe\u0301.fr", "xn--caf-dma.fr", null),
            Triple("\u00E4\u00F6\u00FC.example", "xn--4ca0bs.example", null),
            Triple("\u0627\u0644\u0645\u063A\u0631\u0628.icom.museum", "xn--mgbc0a9azcg.icom.museum", null),
            Triple("a".repeat(63) + ".com", "a".repeat(63) + ".com", null),
            Triple("a".repeat(64) + ".com", null, null),
            Triple("\u00FC".repeat(60) + ".com", null, null),
            Triple("192.168.0.1", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("127.0.0.1", "127.0.0.1", "00000000000000000000ffff7f000001"),
            Triple("0.0.0.0", "0.0.0.0", "00000000000000000000ffff00000000"),
            Triple("255.255.255.255", "255.255.255.255", "00000000000000000000ffffffffffff"),
            Triple("0xC0.0xA8.0x00.0x01", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("0xc0.0250.0.1", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("0300.0250.0.01", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("3232235521", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("0xC0A80001", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("030052000001", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("4294967295", "255.255.255.255", "00000000000000000000ffffffffffff"),
            Triple("0", "0.0.0.0", "00000000000000000000ffff00000000"),
            Triple("1", "0.0.0.1", "00000000000000000000ffff00000001"),
            Triple("0x", "0.0.0.0", "00000000000000000000ffff00000000"),
            Triple("00", "0.0.0.0", "00000000000000000000ffff00000000"),
            Triple("192.168.0%2e1", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("%31%39%32.168.0.1", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("256.1.1.1", "256.1.1.1", null),
            Triple("1.2.3", "1.2.3", null),
            Triple("1.2.3.4.5", "1.2.3.4.5", null),
            Triple("192.0x00A80001", "192.0x00a80001", null),
            Triple("4294967296", "4294967296", null),
            Triple("1.2.3.08", "1.2.3.08", null),
            Triple("1.2.3.-4", "1.2.3.-4", null),
            Triple("192.168.0.1.", "192.168.0.1", null),
            Triple("[2001:db8:0:0:0:0:0:1]", "[2001:db8:0:0:0:0:0:1]", "20010db8000000000000000000000001"),
            Triple("[2001:0db8:85a3:0000:0000:8a2e:0370:7334]", "[2001:db8:85a3:0:0:8a2e:370:7334]", "20010db885a3000000008a2e03707334"),
            Triple("[2001:db8::1]", "[2001:db8:0:0:0:0:0:1]", "20010db8000000000000000000000001"),
            Triple("[::1]", "[0:0:0:0:0:0:0:1]", "00000000000000000000000000000001"),
            Triple("[::]", "[0:0:0:0:0:0:0:0]", "00000000000000000000000000000000"),
            Triple("[1::]", "[1:0:0:0:0:0:0:0]", "00010000000000000000000000000000"),
            Triple("[2001:DB8::ABCD]", "[2001:db8:0:0:0:0:0:abcd]", "20010db800000000000000000000abcd"),
            Triple("[1:2:3:4:5:6:7:8]", "[1:2:3:4:5:6:7:8]", "00010002000300040005000600070008"),
            Triple("[::ffff:192.168.0.1]", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("[::ffff:c0a8:1]", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("[0:0:0:0:0:ffff:c0a8:1]", "192.168.0.1", "00000000000000000000ffffc0a80001"),
            Triple("[::192.168.0.1]", "[0:0:0:0:0:0:c0a8:1]", "000000000000000000000000c0a80001"),
            Triple("[64:ff9b::192.0.2.33]", "[64:ff9b:0:0:0:0:c000:221]", "0064ff9b0000000000000000c0000221"),
            Triple("[1::2::3]", "[1:0:0:0:0:0:0:3]", "00010000000000000000000000000003"),
            Triple("[fe80::1%25eth0]", "[fe80::1%25eth0]", null),
            Triple("[fe80::1%eth0]", "[fe80::1%25eth0]", null),
            Triple("[1:2]", "[1:2]", null),
            Triple("[::g]", "[::g]", null),
            Triple("[12345::1]", "[12345::1]", null),
            Triple("[]", "[]", null),
            Triple("exa%6Dple.com", "example.com", null),
            Triple("ex%41mple.com", "exAmple.com", null),
            Triple("example%2Ecom", "example.com", null),
            Triple("exam%20ple.com", "exam%20ple.com", null),
            Triple("exa%25mple.com", "exa%25mple.com", null),
            Triple("exa%mple.com", "exa%25mple.com", null),
            Triple("exa#mple.com", "exa%23mple.com", null),
            Triple("ex ample.com", "ex%20ample.com", null),
            Triple("exa\u0001mple.com", "exa%01mple.com", null),
            Triple("example.com.", "example.com", null),
            Triple(".", "", null),
            Triple(".example.com", null, null),
            Triple("..example..com..", null, null),
            Triple("a...b", null, null),
            Triple("...", null, null),
            Triple("[1:2:3:4:5:6:7:8:9]", "[1:2:3:4:5:6:7:8:9]", null),
            Triple("[1::2:3:4:5:6:7:8:9]", "[1::2:3:4:5:6:7:8:9]", null),
            Triple("localhost", "localhost", null),
            Triple("ex_ample.com", "ex_ample.com", null),
            Triple("-example-.com", "-example-.com", null)
        ).forEach { (input, normalizedHost, bytesHex) ->
            val normalizer = HostNormalizer(input)
            assertEquals(normalizedHost, normalizer.normalizedHost, "case: $input")
            assertEquals(bytesHex, normalizer.bytes.hex(), "case: $input")
        }
    }
}
