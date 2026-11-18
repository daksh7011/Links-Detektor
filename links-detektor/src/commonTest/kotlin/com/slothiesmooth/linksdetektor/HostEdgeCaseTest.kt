package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Hosts written in an unusual way.
 */
class HostEdgeCaseTest {

    @Test
    fun ipv6WithoutClosingBracketIsNotAHost() {
        for (input in persistentListOf("http://[::1", "http://[1::2", "http://[::1 x", "see [fe80::1 there")) {
            val hosts = LinksDetektor(input, LinksDetektorOptions.Default).detect().map { it.host }.toImmutableList()

            assertEquals(persistentListOf(), hosts, "case: $input")
        }
    }

    @Test
    fun closedIpv6IsStillAHost() {
        val url = LinksDetektor("http://[::1]:8080/a", LinksDetektorOptions.Default).detect().first()

        assertEquals("[::1]", url.host)
        assertEquals(8080, url.port)
    }
}
