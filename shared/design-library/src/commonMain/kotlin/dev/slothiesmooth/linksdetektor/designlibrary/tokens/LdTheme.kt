package dev.slothiesmooth.linksdetektor.designlibrary.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.Res
import dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.geist_bold
import dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.geist_medium
import dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.geist_mono_medium
import dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.geist_mono_regular
import dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.geist_regular
import dev.slothiesmooth.linksdetektor.shared.designlibrary.resources.geist_semibold
import org.jetbrains.compose.resources.Font as ResourceFont

@Immutable
data class LdType(
    val title: TextStyle,
    val body: TextStyle,
    val bodySm: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    /** Link text and URL parts: Geist Mono. */
    val code: TextStyle,
    val codeSm: TextStyle,
)

/** Spacing keylines, corner radii and the sizes touch targets and fields share. */
@Immutable
data class LdDimens(
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space6: Dp = 24.dp,
    val space8: Dp = 32.dp,
    val radiusFocus: Dp = 6.dp,
    val radiusField: Dp = 12.dp,
    val radiusPanel: Dp = 20.dp,
    val radiusSheet: Dp = 24.dp,
    val radiusPill: Dp = 99.dp,
    val fieldMinHeight: Dp = 52.dp,
    val touch: Dp = 48.dp,
)

private fun style(family: FontFamily, size: Int, line: Int, weight: FontWeight) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
)

private fun typeFor(sans: FontFamily, mono: FontFamily) = LdType(
    title = style(sans, 24, 26, FontWeight.Bold).copy(letterSpacing = (-0.02).em),
    body = style(sans, 18, 28, FontWeight.Normal),
    bodySm = style(sans, 16, 24, FontWeight.Normal),
    label = style(sans, 15, 20, FontWeight.SemiBold),
    caption = style(sans, 13, 18, FontWeight.Normal),
    code = style(mono, 14, 23, FontWeight.Normal),
    codeSm = style(mono, 11, 18, FontWeight.Normal),
)

/** The bundled Geist family (sans). */
@Composable
private fun geistFamily() = FontFamily(
    ResourceFont(Res.font.geist_regular, FontWeight.Normal),
    ResourceFont(Res.font.geist_medium, FontWeight.Medium),
    ResourceFont(Res.font.geist_semibold, FontWeight.SemiBold),
    ResourceFont(Res.font.geist_bold, FontWeight.Bold),
)

/** The bundled Geist Mono family (link text and parts). */
@Composable
private fun geistMonoFamily() = FontFamily(
    ResourceFont(Res.font.geist_mono_regular, FontWeight.Normal),
    ResourceFont(Res.font.geist_mono_medium, FontWeight.Medium),
    ResourceFont(Res.font.geist_mono_medium, FontWeight.SemiBold),
    ResourceFont(Res.font.geist_mono_medium, FontWeight.Bold),
)

private val LocalLdColors = staticCompositionLocalOf { LdColorsDark }
private val LocalLdType = staticCompositionLocalOf { typeFor(FontFamily.Default, FontFamily.Monospace) }
private val LocalLdDimens = staticCompositionLocalOf { LdDimens() }

val MaterialTheme.ldColors: LdColors
    @Composable @ReadOnlyComposable
    get() = LocalLdColors.current

val MaterialTheme.ldType: LdType
    @Composable @ReadOnlyComposable
    get() = LocalLdType.current

val MaterialTheme.ldDimens: LdDimens
    @Composable @ReadOnlyComposable
    get() = LocalLdDimens.current

private fun ldColorScheme(colors: LdColors) = darkColorScheme(
    primary = colors.amber,
    onPrimary = colors.amberInk,
    primaryContainer = colors.amber,
    onPrimaryContainer = colors.amberInk,
    secondary = colors.amber,
    onSecondary = colors.amberInk,
    secondaryContainer = colors.amber,
    onSecondaryContainer = colors.amberInk,
    tertiary = colors.amber,
    onTertiary = colors.amberInk,
    tertiaryContainer = colors.amber,
    onTertiaryContainer = colors.amberInk,
    background = colors.stage,
    onBackground = colors.fg,
    surface = colors.surface,
    onSurface = colors.fg,
    surfaceVariant = colors.surface2,
    onSurfaceVariant = colors.dim,
    surfaceTint = Color.Transparent,
    surfaceDim = colors.stage,
    surfaceBright = colors.surface2,
    surfaceContainerLowest = colors.surface,
    surfaceContainerLow = colors.surface,
    surfaceContainer = colors.surface,
    surfaceContainerHigh = colors.surface,
    surfaceContainerHighest = colors.surface2,
    outline = colors.line,
    outlineVariant = colors.line,
    scrim = colors.scrim,
    // No red exists in this system: errors read as the amber accent.
    error = colors.amber,
    onError = colors.amberInk,
)

private fun ldTypography(sans: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = sans),
        displayMedium = base.displayMedium.copy(fontFamily = sans),
        displaySmall = base.displaySmall.copy(fontFamily = sans),
        headlineLarge = base.headlineLarge.copy(fontFamily = sans),
        headlineMedium = base.headlineMedium.copy(fontFamily = sans),
        headlineSmall = base.headlineSmall.copy(fontFamily = sans),
        titleLarge = base.titleLarge.copy(fontFamily = sans),
        titleMedium = base.titleMedium.copy(fontFamily = sans),
        titleSmall = base.titleSmall.copy(fontFamily = sans),
        bodyLarge = base.bodyLarge.copy(fontFamily = sans),
        bodyMedium = base.bodyMedium.copy(fontFamily = sans),
        bodySmall = base.bodySmall.copy(fontFamily = sans),
        labelLarge = base.labelLarge.copy(fontFamily = sans),
        labelMedium = base.labelMedium.copy(fontFamily = sans),
        labelSmall = base.labelSmall.copy(fontFamily = sans),
    )
}

/** The app theme: the one dark scheme, mapped onto Material 3 so stock widgets match the Ld components. */
@Composable
fun LdTheme(content: @Composable () -> Unit) {
    val colors = LdColorsDark
    val sans = geistFamily()
    CompositionLocalProvider(
        LocalLdColors provides colors,
        LocalLdType provides typeFor(sans, geistMonoFamily()),
        LocalLdDimens provides LdDimens(),
    ) {
        MaterialTheme(colorScheme = ldColorScheme(colors), typography = ldTypography(sans), content = content)
    }
}
