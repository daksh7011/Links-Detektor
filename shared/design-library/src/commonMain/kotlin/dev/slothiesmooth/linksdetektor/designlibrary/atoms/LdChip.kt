package dev.slothiesmooth.linksdetektor.designlibrary.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldDimens

private val ChipHeight = 40.dp
private val ChipSidePadding = 16.dp
private val HairlineWidth = 1.dp

/** A pill toggle: a hairline outline, or amber when [selected]. Announced as a checkbox. */
@Composable
fun LdChip(text: String, selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.ldColors
    val shape = RoundedCornerShape(MaterialTheme.ldDimens.radiusPill)
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .ldFocusRing(interactions, MaterialTheme.ldDimens.radiusPill)
            .heightIn(min = ChipHeight)
            .clip(shape)
            .background(if (selected) colors.amber else Color.Transparent)
            .border(HairlineWidth, colors.line, shape)
            .toggleable(
                value = selected,
                interactionSource = interactions,
                indication = null,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = ChipSidePadding),
        contentAlignment = Alignment.Center,
    ) {
        LdText(text = text, style = LdTextStyle.Label, color = if (selected) colors.amberInk else colors.fg)
    }
}
