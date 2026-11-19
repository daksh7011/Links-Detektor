package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.DetectedLink
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.LinkParts
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetailRowsTest {
    @Test
    fun `rows compare original and normalized and mark changes`() {
        val rows = detailRows(
            DetectedLink(
                0,
                0,
                0,
                original = LinkParts("HTTP://Example.COM/a/../b", "http", "", "", "Example.COM", 80, "/a/../b", "", ""),
                normalized = LinkParts("http://example.com/b", "http", "", "", "example.com", 80, "/b", "", ""),
            ),
        )
        assertEquals(persistentListOf("Full URL", "Scheme", "Host", "Port", "Path"), rows.map { it.label }.toImmutableList())
        assertEquals(PartRow("Host", "Example.COM", "example.com", changed = true), rows[2])
        assertEquals(PartRow("Port", "80", "80", changed = false), rows[3])
        assertEquals(PartRow("Scheme", "http", "http", changed = false), rows[1])
    }

    @Test
    fun `a link that could not be normalized has no normalized column`() {
        val rows = detailRows(
            DetectedLink(0, 0, 0, LinkParts("x.com", "http", "", "", "x.com", 80, "", "", ""), normalized = null),
        )
        assertTrue(rows.all { it.normalized == null && !it.changed })
    }
}
