package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.DetectedLink
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.LinkParts
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HighlightTest {
    private val style =
        HighlightStyle(link = SpanStyle(fontWeight = FontWeight.Bold), selected = SpanStyle(fontWeight = FontWeight.Black))

    private fun link(id: Int, start: Int, end: Int, host: String = "h"): DetectedLink {
        val parts = LinkParts(host, "http", "", "", host, 80, "", "", "")
        return DetectedLink(id, start, end, parts, parts)
    }

    @Test
    fun `each link gets one span and one annotation carrying its id`() {
        val text = "see a.com and b.com"
        val out = buildHighlight(text, persistentListOf(link(0, 4, 9), link(1, 14, 19)), selectedId = 1, style)
        assertEquals(text, out.text)
        val tags = out.getStringAnnotations(LINK_TAG, 0, text.length).toImmutableList()
        assertEquals(
            persistentListOf("0" to (4 to 9), "1" to (14 to 19)),
            tags.map { it.item to (it.start to it.end) }.toImmutableList(),
        )
        assertEquals(style.link, out.spanStyles[0].item)
        assertEquals(style.selected, out.spanStyles[1].item)
    }

    @Test
    fun `ranges outside the text are skipped`() {
        val links = persistentListOf(link(0, 2, 99), link(1, -1, 3), link(2, 3, 3), link(3, 1, 4))
        val out = buildHighlight("short", links, null, style)
        val tags = out.getStringAnnotations(LINK_TAG, 0, 5).toImmutableList()
        assertEquals(persistentListOf("3"), tags.map { it.item }.toImmutableList())
    }

    @Test
    fun `an overlapping range is skipped and the earlier one kept`() {
        val links = persistentListOf(link(0, 0, 5), link(1, 3, 8), link(2, 5, 9))
        val out = buildHighlight("0123456789", links, null, style)
        val tags = out.getStringAnnotations(LINK_TAG, 0, 10).toImmutableList()
        assertEquals(persistentListOf("0", "2"), tags.map { it.item }.toImmutableList())
    }

    @Test
    fun `no links gives plain text`() {
        val out = buildHighlight("plain", persistentListOf(), null, style)
        assertEquals(0, out.spanStyles.size)
    }

    @Test
    fun `linkIdAt hits inside a link and its last character but not its end or the gaps`() {
        val out = buildHighlight("see a.com and b.com", persistentListOf(link(0, 4, 9), link(1, 14, 19)), null, style)
        assertEquals(0, out.linkIdAt(4))
        assertEquals(0, out.linkIdAt(6))
        assertEquals(0, out.linkIdAt(8))
        assertNull(out.linkIdAt(9))
        assertNull(out.linkIdAt(2))
        assertEquals(1, out.linkIdAt(18))
    }
}
