package dev.slothiesmooth.linksdetektor.designlibrary.tokens

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

private const val LINEAR_CUTOFF = 0.03928
private const val LINEAR_DIVISOR = 12.92
private const val GAMMA_OFFSET = 0.055
private const val GAMMA_SCALE = 1.055
private const val GAMMA = 2.4
private const val RED_WEIGHT = 0.2126
private const val GREEN_WEIGHT = 0.7152
private const val BLUE_WEIGHT = 0.0722
private const val FLARE = 0.05

private fun linear(channel: Float): Double {
    val c = channel.toDouble()
    return if (c <= LINEAR_CUTOFF) c / LINEAR_DIVISOR else ((c + GAMMA_OFFSET) / GAMMA_SCALE).pow(GAMMA)
}

private fun luminance(color: Color): Double =
    RED_WEIGHT * linear(color.red) + GREEN_WEIGHT * linear(color.green) + BLUE_WEIGHT * linear(color.blue)

/** The WCAG 2 contrast ratio of [foreground] on [background], from 1.0 (identical) to 21.0 (black on white). */
fun contrastRatio(foreground: Color, background: Color): Double {
    val a = luminance(foreground)
    val b = luminance(background)
    return (maxOf(a, b) + FLARE) / (minOf(a, b) + FLARE)
}
