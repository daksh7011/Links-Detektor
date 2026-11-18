package com.slothiesmooth.linksdetektor

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Userinfo cannot contain `#`, `?` or `/`: those start the fragment, query or path, so a later `@` belongs to them.
 */
class UserinfoDelimiterTest {

    /** `#`, `?`, `/` and `\` end the authority, so an `@` after one never makes the earlier text userinfo. */
    private val confusingInputs: ImmutableList<String> = persistentListOf(
        "http://evil#@good.com",
        "http://localhost#@good.com",
        "http://evil?x@good.com",
        "http://evil?@good.com",
        "http://evil:80#@good.com",
        "http://evil:80?x@good.com",
        "http://u:p@evil#@good.com",
        "http://evil/@good.com",
        "https://evil\\@good.com",
        "http://evil\\x@good.com",
        "http://evil.org#@good.com",
        "http://evil.org\\@good.com",
    )

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

    @Test
    fun confusingAtSignNeverGivesGoodComUserinfoUnderAnyOption() {
        for (option in LinksDetektorOptions.entries) {
            for (input in confusingInputs) {
                for (url in LinksDetektor(input, option).detect()) {
                    val case = "option=$option input=$input url=${url.fullUrl}"
                    // A later bare "x@good.com" may be detected on its own, but never with the earlier host in it.
                    val written = "${url.username} ${url.originalUrl}"
                    val earlierHostInUserinfo =
                        url.host == "good.com" && (written.contains("evil") || written.contains("localhost"))
                    assertFalse(earlierHostInUserinfo, case)
                }
            }
        }
    }

    @Test
    fun hostBeforeTheDelimiterIsWhatABrowserWouldUse() {
        val expectedHosts = persistentMapOf(
            "http://evil#@good.com" to "evil",
            "http://localhost#@good.com" to "localhost",
            "http://evil?x@good.com" to "evil",
            "http://evil:80#@good.com" to "evil",
            "http://u:p@evil#@good.com" to "evil",
            "https://evil\\@good.com" to "evil",
            "http://evil\\x@good.com" to "evil",
        )
        for ((input, host) in expectedHosts) {
            val url = LinksDetektor(input, LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect().first()

            assertEquals(host, url.host, input)
        }
    }

    @Test
    fun emptyHostBeforeTheDelimiterHasNoUserinfo() {
        for (option in LinksDetektorOptions.entries) {
            for (url in LinksDetektor("http://#@good.com/", option).detect()) {
                assertEquals("", url.username, "option=$option")
            }
        }
    }
}
