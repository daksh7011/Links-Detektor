package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.LinksDetektor
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * JVM-only: host lowercasing must not depend on the default locale (Turkish maps `I` to a dotless i).
 */
class TurkishLocaleTest {

    @Test
    fun hostLowercasingIsLocaleIndependent() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("title.com", HostNormalizer("TITLE.com").normalizedHost)
            assertEquals(
                "title.com",
                LinksDetektor("HTTP://TITLE.COM/I", LinksDetektorOptions.Default).detect().single().normalize().host
            )
        } finally {
            Locale.setDefault(previous)
        }
    }
}
