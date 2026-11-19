package dev.slothiesmooth.linksdetektor.designlibrary.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldType

/** Text in one of the design system's styles; a [LdTextStyle.Title] is announced as a heading. */
@Composable
fun LdText(
    text: String,
    modifier: Modifier = Modifier,
    style: LdTextStyle = LdTextStyle.Body,
    color: Color = MaterialTheme.ldColors.fg,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = if (style == LdTextStyle.Title) modifier.semantics { heading() } else modifier,
        color = color,
        style = MaterialTheme.ldType.of(style),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}
