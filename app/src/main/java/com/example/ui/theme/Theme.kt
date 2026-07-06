package com.planca.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkAccent,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkOnPrimary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightAccent,
    background = LightBg,
    surface = LightSurface,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnPrimary,
    onBackground = LightOnBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextMuted
)

private val RetroColorScheme = lightColorScheme(
    primary = Color(0xFFC2410C),
    secondary = Color(0xFF0F766E),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFFDF6E3),
    surface = Color(0xFFF5E6D3),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF3E2723),
    onSurface = Color(0xFF3E2723),
    surfaceVariant = Color(0xFFEAD8C3),
    onSurfaceVariant = Color(0xFF5D4037)
)

private val NeonColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF),
    secondary = Color(0xFFFF007F),
    tertiary = Color(0xFF39FF14),
    background = Color(0xFF0D021A),
    surface = Color(0xFF1B053A),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFFE0E0FF),
    onSurface = Color(0xFFE0E0FF),
    surfaceVariant = Color(0xFF2C0B5E),
    onSurfaceVariant = Color(0xFF9E86FF)
)

private val UltraDarkColorScheme = darkColorScheme(
    primary = Color(0xFF94A3B8),
    secondary = Color(0xFF64748B),
    tertiary = Color(0xFFE2E8F0),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFF94A3B8),
    onSurface = Color(0xFF94A3B8),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFF475569)
)

private val NatureColorScheme = lightColorScheme(
    primary = Color(0xFF047857),
    secondary = Color(0xFF065F46),
    tertiary = Color(0xFF10B981),
    background = Color(0xFFF0FDF4),
    surface = Color(0xFFE6F4EA),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF064E3B),
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFD1E7DD),
    onSurfaceVariant = Color(0xFF355E3B)
)

private val PembeColorScheme = lightColorScheme(
    primary = Color(0xFFDB2777),
    secondary = Color(0xFFF472B6),
    tertiary = Color(0xFFE11D48),
    background = Color(0xFFFFF1F2),
    surface = Color(0xFFFFE4E6),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF4C0519),
    onSurface = Color(0xFF4C0519),
    surfaceVariant = Color(0xFFFECDD3),
    onSurfaceVariant = Color(0xFF9F1239)
)

private val RetroTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp)
)

private val NeonTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 1.sp)
)

private val NatureTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.4.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp)
)

private val UltraDarkTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.8).sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 15.sp, letterSpacing = 0.8.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 1.5.sp)
)

@Composable
fun MyApplicationTheme(
    themePreference: String = "Açık",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val (colorScheme, typography) = when (themePreference.lowercase()) {
        "retro" -> Pair(RetroColorScheme, RetroTypography)
        "neon" -> Pair(NeonColorScheme, NeonTypography)
        "ultra dark", "ultra_dark" -> Pair(UltraDarkColorScheme, UltraDarkTypography)
        "nature" -> Pair(NatureColorScheme, NatureTypography)
        "pembe" -> Pair(PembeColorScheme, Typography())
        "koyu" -> Pair(DarkColorScheme, Typography())
        else -> {
            if (darkTheme) Pair(DarkColorScheme, Typography()) else Pair(LightColorScheme, Typography())
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
