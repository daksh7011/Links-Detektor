package com.slothiesmooth.linksdetektor.internal

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization tests: values captured from the JVM implementation before the KMP migration.
 */
class PathNormalizerGoldenTest {

    @Test
    fun pathGoldens() {
        persistentListOf<Pair<String?, String?>>(
            null to null,
            "" to "",
            "/" to "/",
            "/a/b/c" to "/a/b/c",
            "/a/./b" to "/a/b",
            "/a/../b" to "/b",
            "/a/b/.." to "/a/",
            "/a/b/../" to "/a/",
            "/a/b/." to "/a/b/",
            "/a/b/./" to "/a/b/",
            "/.." to "/",
            "/../" to "/",
            "/../../a" to "/a",
            "/." to "/",
            "/./" to "/",
            "//" to "/",
            "///" to "/",
            "/a//b" to "/a/b",
            "/a///b//" to "/a/b/",
            "/a/b/../../c" to "/c",
            "/a/b/../../../c" to "/c",
            "/a/%2e/b" to "/a/b",
            "/a/%2E%2E/b" to "/b",
            "/a/%2e%2e" to "/",
            "/%2e%2e/%2e%2e/a" to "/a",
            "/a%2fb" to "/a/b",
            "/a/b%2f..%2fc" to "/a/c",
            "/a%2Fb/../c" to "/a/c",
            "/a b" to "/a%20b",
            "/a%20b" to "/a%20b",
            "/a%2520b" to "/a%20b",
            "/a%252520b" to "/a%20b",
            "/a%25%32%30b" to "/a%20b",
            "/%" to "/%25",
            "/%2" to "/%252",
            "/%zz" to "/%25zz",
            "/a%" to "/a%25",
            "/100%/x" to "/100%25/x",
            "/a#b" to "/a%23b",
            "/a%23b" to "/a%23b",
            "/caf\u00E9" to "/caf%E9",
            "/\u4F8B" to "/%8B",
            "/\uD83D\uDE00" to "/%3D%00",
            "/a\u0100b" to "/a%00b",
            "/a\u0009b" to "/a%09b",
            "/a%0Ab" to "/a%0Ab",
            "/a~b_c-d.e" to "/a~b_c-d.e",
            "/a/.../b" to "/a/.../b",
            "/a/..b/c" to "/a/..b/c",
            "/a/.b/c" to "/a/.b/c",
            "/a/b../c" to "/a/b../c",
            "a/b" to "a/b",
            "a/../b" to "/b",
            ".." to "..",
            "." to ".",
            "/a/b/c/../../.." to "/",
            "/a/b/c/../../../" to "/",
            "/a/./././b" to "/a/b",
            "/./a" to "/a",
            "/../a/./b/../c//d/" to "/a/c/d/",
            "/%7Euser" to "/~user",
            "/a?b" to "/a?b",
            "/%41%42%43" to "/ABC"
        ).forEach { (input, expected) ->
            assertEquals(expected, PathNormalizer().normalizePath(input), "case: $input")
        }
    }
}
