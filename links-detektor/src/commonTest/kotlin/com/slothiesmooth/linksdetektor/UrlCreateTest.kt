package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.persistentListOf
import java.net.MalformedURLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Characterization tests for [Url.create].
 */
class UrlCreateTest {

    @Test
    fun noUrlThrows() {
        persistentListOf("", "   ", "localhost", "hello", "http://", "://").forEach { input ->
            val error = assertFailsWith<MalformedURLException>("case: $input") { Url.create(input) }
            assertEquals("We couldn't find any urls in string: $input", error.message, "case: $input")
        }
    }

    @Test
    fun moreThanOneUrlThrows() {
        val input = "www.a.com,www.b.com"
        val error = assertFailsWith<MalformedURLException> { Url.create(input) }
        assertEquals("We found more than one url in string: www.a.com,www.b.com", error.message)
    }

    @Test
    fun createParsesParts() {
        val input = "http://user:pass@host.com:99/p?q=1#f"
        val url = Url.create(input)
        assertEquals("http", url.scheme)
        assertEquals("user", url.username)
        assertEquals("pass", url.password)
        assertEquals("host.com", url.host)
        assertEquals(99, url.port)
        assertEquals("/p", url.path)
        assertEquals("?q=1", url.query)
        assertEquals("#f", url.fragment)
        assertEquals(input, url.fullUrl)
    }

    @Test
    fun createSanitisesWhitespace() {
        assertEquals("http://example.com/a%20b", Url.create("  http://example.com/a b  ").originalUrl)
        assertEquals("http://example.com/path", Url.create("http://example.com/\tpath").originalUrl)
    }

    @Test
    fun createDefaults() {
        val bare = Url.create("example.com")
        assertEquals("http", bare.scheme)
        assertEquals(80, bare.port)
        assertEquals("/", bare.path)
        assertEquals("http://example.com/", bare.fullUrl)

        val relative = Url.create("//cdn.example.com/x.js")
        assertEquals("", relative.scheme)
        assertEquals(-1, relative.port)
        assertEquals("//cdn.example.com/x.js", relative.fullUrl)
    }

    @Test
    fun spacesDoNotSplit() {
        val url = Url.create("http://example.com http://example.org")
        assertEquals("example.com%20http", url.host)
    }
}
