package dev.slothiesmooth.linksdetektor.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldDimens
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldType

private val HaloWidth = 3.dp
private val HairlineWidth = 1.dp
private const val DOUBLE = 2f

/**
 * A text field with its [label] above it. Focus draws an amber border and a 3 dp halo. [modifier] sits on the whole field with its
 * label; [fieldModifier] on the editable box itself (for example a test tag a text-input action can find).
 */
@Composable
fun LdTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    fieldModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.ldColors
    val dimens = MaterialTheme.ldDimens
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val shape = RoundedCornerShape(dimens.radiusField)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        // The field itself carries the label for screen readers; the visible text must not announce it again.
        LdText(text = label, style = LdTextStyle.Label, modifier = Modifier.clearAndSetSemantics {})
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier
                .semantics { contentDescription = label }
                .drawBehind {
                    if (focused) {
                        val halo = HaloWidth.toPx()
                        drawRoundRect(
                            color = colors.focusHalo,
                            topLeft = Offset(-halo, -halo),
                            size = Size(size.width + halo * DOUBLE, size.height + halo * DOUBLE),
                            cornerRadius = CornerRadius(dimens.radiusField.toPx() + halo),
                        )
                    }
                }
                .background(colors.stage, shape)
                .border(BorderStroke(HairlineWidth, if (focused) colors.amber else colors.line), shape)
                .heightIn(min = dimens.fieldMinHeight),
            textStyle = MaterialTheme.ldType.body.copy(color = colors.fg),
            cursorBrush = SolidColor(colors.amber),
            interactionSource = interactions,
            singleLine = maxLines == 1,
            minLines = minLines,
            maxLines = maxLines,
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = dimens.space4, vertical = dimens.space3)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        LdText(text = placeholder, color = colors.dim)
                    }
                    inner()
                }
            },
        )
    }
}
