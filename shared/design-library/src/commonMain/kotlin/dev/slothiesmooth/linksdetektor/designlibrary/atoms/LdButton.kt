package dev.slothiesmooth.linksdetektor.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.LdColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldDimens

private val SidePadding = 24.dp
private val HairlineWidth = 1.dp
private const val DISABLED_ALPHA = 0.5f

private fun fill(style: LdButtonStyle, hovered: Boolean, colors: LdColors): Color = when {
    style == LdButtonStyle.Primary -> if (hovered) colors.amberHover else colors.amber
    hovered -> colors.surface
    else -> Color.Transparent
}

/** A pill button, at least 48 dp high. Primary is amber; Ghost is a hairline outline. No elevation. */
@Composable
fun LdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: LdButtonStyle = LdButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.ldColors
    val dimens = MaterialTheme.ldDimens
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val label = if (style == LdButtonStyle.Primary) colors.amberInk else colors.fg
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactions,
        shape = RoundedCornerShape(dimens.radiusPill),
        color = fill(style, hovered, colors),
        contentColor = label,
        border = if (style == LdButtonStyle.Ghost) BorderStroke(HairlineWidth, colors.line) else null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier.ldFocusRing(interactions, dimens.radiusPill),
    ) {
        Box(
            Modifier.heightIn(min = dimens.touch).padding(horizontal = SidePadding),
            contentAlignment = Alignment.Center,
        ) {
            LdText(text = text, style = LdTextStyle.Label, color = label.copy(alpha = if (enabled) 1f else DISABLED_ALPHA))
        }
    }
}
