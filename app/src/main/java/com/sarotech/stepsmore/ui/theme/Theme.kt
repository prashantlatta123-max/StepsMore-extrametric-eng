package com.sarotech.stepsmore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal data class Palette(
    val bg: Color, val card: Color, val badge: Color, val fab: Color,
    val primary: Color, val secondary: Color, val muted: Color,
    val inactive: Color, val disabled: Color, val disabledSub: Color,
    val divider: Color, val dividerList: Color, val chartPast: Color,
    val chartToday: Color, val chartSelected: Color, val chartDim: Color,
    val target: Color, val input: Color, val accent: Color, val onAccent: Color
)

object StepsMoreColors {
    private var palette by mutableStateOf(defaultPalette())
    internal fun update(value: Palette) { palette = value }
    val BgPrimary get() = palette.bg
    val SurfaceCard get() = palette.card
    val SurfaceBadge get() = palette.badge
    val SurfaceFab get() = palette.fab
    val TextPrimary get() = palette.primary
    val TextSecondary get() = palette.secondary
    val TextMuted get() = palette.muted
    val TextCalendarInactive get() = palette.inactive
    val TextDisabled get() = palette.disabled
    val TextDisabledSub get() = palette.disabledSub
    val DividerMajor get() = palette.divider
    val DividerList get() = palette.dividerList
    val ChartBarPast get() = palette.chartPast
    val ChartBarToday get() = palette.chartToday
    val ChartBarSelected get() = palette.chartSelected
    val ChartBarDimmed get() = palette.chartDim
    val ChartTargetLine get() = palette.target
    val SwitchTrackOn get() = palette.accent
    val SwitchTrackOff get() = palette.badge
    val SwitchBorderOff get() = palette.muted
    val SwitchThumbActive get() = palette.primary
    val SwitchTrackDisabled get() = palette.badge
    val SwitchBorderDisabled get() = palette.divider
    val SwitchThumbDisabled get() = palette.disabled
    val InputOutline get() = palette.input
    val Accent get() = palette.accent
    val OnAccent get() = palette.onAccent
}

private fun defaultPalette() = makePalette("Black", "#7C83FF")

private fun parseAccent(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF7C83FF))

private fun makePalette(mode: String, accentHex: String): Palette {
    val accent = parseAccent(accentHex)
    val white = mode.equals("Snow White", true)
    val glass = mode.equals("Glass", true)
    return if (white) Palette(
        Color(0xFFF7F8FA), Color(0xFFFFFFFF), Color(0xFFF0F2F5), Color(0xFFE6E9EF),
        Color(0xFF17191D), Color(0xFF454A54), Color(0xFF747B87), Color(0xFFB5BBC5),
        Color(0xFFB8BEC8), Color(0xFFD0D5DD), Color(0xFFDDE1E7), Color(0xFFE9ECF1),
        Color(0xFF8C929C), Color(0xFFB9BEC7), accent, Color(0xFFD2D5DC),
        Color(0xFFD7DBE2), Color(0xFF7C8491), accent, Color.White
    ) else if (glass) Palette(
        Color(0xFF10131B), Color(0xCC252B3A), Color(0xAA343D50), Color(0xFF343D50),
        Color(0xFFF4F6FF), Color(0xFFD0D6E8), Color(0xFFA2ADC5), Color(0xFF6E7890),
        Color(0xFF69738A), Color(0xFF50596E), Color(0x665D6B86), Color(0x553D4960),
        Color(0xFFAAB5CA), Color(0xFF737F98), accent, Color(0xFF4D5870),
        Color(0xFF4B5872), Color(0xFF8E9AB1), accent, Color.White
    ) else Palette(
        Color.Black, Color(0xFF0E0E0E), Color(0xFF141414), Color(0xFF303030),
        Color(0xFFECEEF4), Color(0xFFB8B8B8), Color(0xFF8E8E8E), Color(0xFF5A5A5A),
        Color(0xFF555555), Color(0xFF424242), Color(0xFF2E2E2E), Color(0xFF1A1A1A),
        Color(0xFFC4C4C4), Color(0xFF666666), accent, Color(0xFF444444),
        Color(0xFF3A3A3A), Color(0xFF8E8E8E), accent, Color.White
    )
}

private val StepsMoreTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 50.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp)
)

@Composable
fun StepsMoreTheme(themeMode: String = "Black", accentColorHex: String = "#7C83FF", content: @Composable () -> Unit) {
    val palette = makePalette(themeMode, accentColorHex)
    SideEffect { StepsMoreColors.update(palette) }
    val scheme = if (themeMode.equals("Snow White", true)) lightColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent, background = palette.bg,
        onBackground = palette.primary, surface = palette.card, onSurface = palette.primary,
        surfaceVariant = palette.badge, onSurfaceVariant = palette.muted, outline = palette.input
    ) else darkColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent, background = palette.bg,
        onBackground = palette.primary, surface = palette.card, onSurface = palette.primary,
        surfaceVariant = palette.badge, onSurfaceVariant = palette.muted, outline = palette.input
    )
    MaterialTheme(colorScheme = scheme, typography = StepsMoreTypography, content = content)
}
