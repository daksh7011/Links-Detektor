// The single raw-ARGB file of the design library: the dark, amber-on-warm-black identity of DESIGN.md.
package dev.slothiesmooth.linksdetektor.designlibrary.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class LdColors(
    /** The page ground. */
    val stage: Color,
    /** Panels and sheets. */
    val surface: Color,
    /** Raised rows one step above [surface]. */
    val surface2: Color,
    /** Hairlines and borders. */
    val line: Color,
    /** Primary text. */
    val fg: Color,
    /** Secondary text. */
    val dim: Color,
    /** A confirmed or found state. */
    val ok: Color,
    /** The one accent: actions, selection and highlights. */
    val amber: Color,
    val amberHover: Color,
    /** Text and icons on [amber]. */
    val amberInk: Color,
    /** The halo drawn around a focused field. */
    val focusHalo: Color,
    val scrim: Color,
)

private val Stage = Color(0xFF0D0C0A)
private val Amber = Color(0xFFFFB547)
private const val HALO_ALPHA = 0.25f
private const val SCRIM_ALPHA = 0.7f

val LdColorsDark = LdColors(
    stage = Stage,
    surface = Color(0xFF161411),
    surface2 = Color(0xFF1E1B17),
    line = Color(0xFF2A2722),
    fg = Color(0xFFF5F1EA),
    dim = Color(0xFFA8A296),
    ok = Color(0xFF8BD7A2),
    amber = Amber,
    amberHover = Color(0xFFFFC670),
    amberInk = Color(0xFF1A1206),
    focusHalo = Amber.copy(alpha = HALO_ALPHA),
    scrim = Stage.copy(alpha = SCRIM_ALPHA),
)
