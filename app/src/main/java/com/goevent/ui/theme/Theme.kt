package com.goevent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
class CosmicColors(
    val isDark: Boolean,
    val background: Color, val secondary: Color, val tertiary: Color,
    val card: Color, val elevated: Color,
    val primary: Color, val violet: Color, val deepViolet: Color,
    val pink: Color, val blue: Color, val cyan: Color,
    val textMain: Color, val textSecondary: Color, val textMuted: Color,
    val border: Color, val success: Color, val warning: Color, val error: Color
)

val DarkCosmic = CosmicColors(
    isDark = true,
    background = Color(0xFF070616), secondary = Color(0xFF0B0A1F), tertiary = Color(0xFF12102A),
    card = Color(0xFF171431), elevated = Color(0xFF1E1940),
    primary = Color(0xFF8B5CF6), violet = Color(0xFFA78BFA), deepViolet = Color(0xFF6D28D9),
    pink = Color(0xFFEC4899), blue = Color(0xFF38BDF8), cyan = Color(0xFF67E8F9),
    textMain = Color(0xFFF8F7FF), textSecondary = Color(0xFFB9B5D0), textMuted = Color(0xFF817B9F),
    border = Color(0xFF30294F), success = Color(0xFF34D399), warning = Color(0xFFFBBF24), error = Color(0xFFFB7185)
)

val LightCosmic = CosmicColors(
    isDark = false,
    background = Color(0xFFF7F5FF), secondary = Color(0xFFF0ECFF), tertiary = Color(0xFFE8E0FF),
    card = Color(0xFFFFFFFF), elevated = Color(0xFFFDFBFF),
    primary = Color(0xFF6D28D9), violet = Color(0xFF8B5CF6), deepViolet = Color(0xFF5B21B6),
    pink = Color(0xFFDB2777), blue = Color(0xFF0284C7), cyan = Color(0xFF0891B2),
    textMain = Color(0xFF21183C), textSecondary = Color(0xFF625A78), textMuted = Color(0xFF9189A6),
    border = Color(0xFFE2DAF2), success = Color(0xFF059669), warning = Color(0xFFD97706), error = Color(0xFFE11D48)
)

val LocalCosmic = staticCompositionLocalOf { DarkCosmic }

/** Shortcut: `cosmic.card`, `cosmic.textMain` ... works in any composable. */
val cosmic: CosmicColors
    @Composable @ReadOnlyComposable get() = LocalCosmic.current

object Galaxy {
    val galaxy = Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6), Color(0xFFEC4899)))
    val nebula = Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF7C3AED), Color(0xFFC026D3), Color(0xFFEC4899)))
    val cosmicBlue = Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1D4ED8), Color(0xFF38BDF8)))
    val sunrise = Brush.linearGradient(listOf(Color(0xFF7C2D12), Color(0xFFEA580C), Color(0xFFF59E0B), Color(0xFFFDE68A)))
    val glass = Brush.linearGradient(listOf(Color(0x388B5CF6), Color(0x1AEC4899)))
}

val GoTypography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun GoEventTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkCosmic else LightCosmic
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = Color.White, secondary = c.pink, tertiary = c.blue,
            background = c.background, onBackground = c.textMain,
            surface = c.card, onSurface = c.textMain,
            surfaceVariant = c.elevated, onSurfaceVariant = c.textSecondary,
            outline = c.border, error = c.error
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = Color.White, secondary = c.pink, tertiary = c.blue,
            background = c.background, onBackground = c.textMain,
            surface = c.card, onSurface = c.textMain,
            surfaceVariant = c.elevated, onSurfaceVariant = c.textSecondary,
            outline = c.border, error = c.error
        )
    }
    CompositionLocalProvider(LocalCosmic provides c) {
        MaterialTheme(colorScheme = scheme, typography = GoTypography, content = content)
    }
}
