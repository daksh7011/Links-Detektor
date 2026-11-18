package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.DetectedLink
import kotlinx.collections.immutable.ImmutableList

/** The tag of the string annotation that carries a link's id. */
const val LINK_TAG = "link"

@Immutable
data class HighlightStyle(val link: SpanStyle, val selected: SpanStyle)

/**
 * [text] with one [LINK_TAG] string annotation (the link id) and one span per valid link range. A range is valid when it lies
 * inside the text, is not empty and starts at or after the end of the previous accepted range; other ranges are skipped.
 */
fun buildHighlight(
    text: String,
    links: ImmutableList<DetectedLink>,
    selectedId: Int?,
    style: HighlightStyle,
): AnnotatedString = buildAnnotatedString {
    append(text)
    var accepted = 0
    links.forEach { link ->
        val valid = link.start >= 0 && link.start < link.endExclusive && link.endExclusive <= text.length && link.start >= accepted
        if (valid) {
            addStringAnnotation(LINK_TAG, link.id.toString(), link.start, link.endExclusive)
            addStyle(if (link.id == selectedId) style.selected else style.link, link.start, link.endExclusive)
            accepted = link.endExclusive
        }
    }
}
