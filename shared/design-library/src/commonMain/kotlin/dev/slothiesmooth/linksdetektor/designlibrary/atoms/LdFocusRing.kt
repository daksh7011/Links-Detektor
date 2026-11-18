package dev.slothiesmooth.linksdetektor.designlibrary.atoms

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors

private val RingStroke = 2.dp
private val RingGap = 2.dp
private const val HALF = 2f

/**
 * A 2 dp amber ring, offset 2 dp clear of the control, while the control that owns [interactionSource] has keyboard focus
 * (never for a touch or a click).
 */
@Composable
fun Modifier.ldFocusRing(interactionSource: InteractionSource, cornerRadius: Dp): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val color = MaterialTheme.ldColors.amber
    if (!focused || !keyboard) return this
    return drawWithContent {
        drawContent()
        val inset = -(RingGap.toPx() + RingStroke.toPx() / HALF)
        val radius = (cornerRadius.toPx() - inset).coerceAtLeast(0f)
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * HALF, size.height - inset * HALF),
            cornerRadius = CornerRadius(radius),
            style = Stroke(RingStroke.toPx()),
        )
    }
}
