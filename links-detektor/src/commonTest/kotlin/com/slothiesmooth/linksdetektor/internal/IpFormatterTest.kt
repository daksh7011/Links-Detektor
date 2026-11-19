package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class IpFormatterTest {

    private fun bytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    @Test
    fun formatsLikeInetAddress() {
        persistentListOf(
            "00000000000000000000ffffc0a80001" to "192.168.0.1",
            "00000000000000000000ffffffffffff" to "255.255.255.255",
            "20010db8000000000000000000000001" to "[2001:db8:0:0:0:0:0:1]",
            "20010db885a3000000008a2e03707334" to "[2001:db8:85a3:0:0:8a2e:370:7334]",
            "00000000000000000000000000000000" to "[0:0:0:0:0:0:0:0]",
            // IPv4-compatible is not unwrapped.
            "000000000000000000000000c0a80001" to "[0:0:0:0:0:0:c0a8:1]",
            // The prefix is not exactly the mapped one.
            "0000000000000000000100ffc0a80001" to "[0:0:0:0:1:ff:c0a8:1]",
        ).forEach { (hex, expected) ->
            assertEquals(expected, formatIp(bytes(hex)), "case: $hex")
        }
    }
}
