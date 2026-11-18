package dev.slothiesmooth.linksdetektor.designlibrary

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdButtonStyle
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.buttonFill
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.LdColorsDark
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColorScheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun ColorScheme.roles(): ImmutableList<Pair<String, Color>> = persistentListOf(
    "primary" to primary, "onPrimary" to onPrimary, "primaryContainer" to primaryContainer,
    "onPrimaryContainer" to onPrimaryContainer, "inversePrimary" to inversePrimary,
    "secondary" to secondary, "onSecondary" to onSecondary, "secondaryContainer" to secondaryContainer,
    "onSecondaryContainer" to onSecondaryContainer, "tertiary" to tertiary, "onTertiary" to onTertiary,
    "tertiaryContainer" to tertiaryContainer, "onTertiaryContainer" to onTertiaryContainer,
    "background" to background, "onBackground" to onBackground, "surface" to surface, "onSurface" to onSurface,
    "surfaceVariant" to surfaceVariant, "onSurfaceVariant" to onSurfaceVariant, "surfaceTint" to surfaceTint,
    "inverseSurface" to inverseSurface, "inverseOnSurface" to inverseOnSurface, "error" to error,
    "onError" to onError, "errorContainer" to errorContainer, "onErrorContainer" to onErrorContainer,
    "outline" to outline, "outlineVariant" to outlineVariant, "scrim" to scrim,
    "surfaceBright" to surfaceBright, "surfaceDim" to surfaceDim, "surfaceContainer" to surfaceContainer,
    "surfaceContainerHigh" to surfaceContainerHigh, "surfaceContainerHighest" to surfaceContainerHighest,
    "surfaceContainerLow" to surfaceContainerLow, "surfaceContainerLowest" to surfaceContainerLowest,
    "primaryFixed" to primaryFixed, "primaryFixedDim" to primaryFixedDim, "onPrimaryFixed" to onPrimaryFixed,
    "onPrimaryFixedVariant" to onPrimaryFixedVariant, "secondaryFixed" to secondaryFixed,
    "secondaryFixedDim" to secondaryFixedDim, "onSecondaryFixed" to onSecondaryFixed,
    "onSecondaryFixedVariant" to onSecondaryFixedVariant, "tertiaryFixed" to tertiaryFixed,
    "tertiaryFixedDim" to tertiaryFixedDim, "onTertiaryFixed" to onTertiaryFixed,
    "onTertiaryFixedVariant" to onTertiaryFixedVariant,
)

class LdSchemeTest {
    private val c = LdColorsDark
    private val scheme = ldColorScheme(c)

    @Test
    fun `every Material role is one of the design tokens, never a stock default`() {
        val palette = persistentSetOf(
            c.stage, c.surface, c.surface2, c.line, c.fg, c.dim, c.ok, c.amber, c.amberHover, c.amberInk,
            c.scrim, Color.Transparent,
        )
        val strays = scheme.roles().filter { it.second !in palette }.map { it.first }.toImmutableList()
        assertTrue(strays.isEmpty(), "roles still on a Material default: $strays")
    }

    @Test
    fun `the previously leaking roles are pinned`() {
        assertEquals(c.amber, scheme.errorContainer)
        assertEquals(c.amberInk, scheme.onErrorContainer)
        assertEquals(c.fg, scheme.inverseSurface)
        assertEquals(c.stage, scheme.inverseOnSurface)
        assertEquals(c.amber, scheme.inversePrimary)
        assertEquals(c.amber, scheme.primaryFixed)
        assertEquals(c.amberInk, scheme.onPrimaryFixed)
    }

    @Test
    fun `a disabled primary button fades its fill`() {
        assertEquals(c.amber, buttonFill(LdButtonStyle.Primary, hovered = false, enabled = true, colors = c))
        val off = buttonFill(LdButtonStyle.Primary, hovered = false, enabled = false, colors = c)
        assertTrue(off.alpha < 1f)
    }
}
