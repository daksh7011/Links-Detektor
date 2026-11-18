package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Userinfo cannot contain `#`, `?` or `/`: those start the fragment, query or path, so a later `@` belongs to them.
 */
class UserinfoDelimiterTest {

    private fun detectAll(input: String): ImmutableList<Url> =
        LinksDetektor(input, LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect()

    @Test
    fun fragmentAtSignDoesNotMakeUserinfo() {
        val urls = detectAll("http://evil#@good.com")

        assertEquals("evil", urls.first().host)
        assertEquals("", urls.first().username)
        assertEquals("http://evil/#@good.com", urls.first().fullUrl)
    }

    @Test
    fun queryAtSignDoesNotMakeUserinfo() {
        val url = detectAll("http://evil?@good.com").first()

        assertEquals("evil", url.host)
        assertEquals("", url.username)
        assertEquals("?@good.com", url.query)
    }

    @Test
    fun pathAtSignDoesNotMakeUserinfo() {
        val url = detectAll("http://evil/@good.com").first()

        assertEquals("evil", url.host)
        assertEquals("", url.username)
        assertEquals("/@good.com", url.path)
    }

    @Test
    fun realUserinfoStillWorks() {
        val url = detectAll("http://user:pw@good.com/x").first()

        assertEquals("good.com", url.host)
        assertEquals("user", url.username)
    }
}
