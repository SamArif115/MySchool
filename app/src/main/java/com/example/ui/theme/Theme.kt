package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.ThemePreset

data class SchoolBrandColors(
    val preset: ThemePreset = ThemePreset.BLUE_TEAL,
    val primary: Color = Color(ThemePreset.BLUE_TEAL.primaryHex),
    val secondary: Color = Color(ThemePreset.BLUE_TEAL.secondaryHex),
    val accent: Color = Color(ThemePreset.BLUE_TEAL.accentHex),
    val containerLight: Color = Color(ThemePreset.BLUE_TEAL.lightBgHex)
) {
    val headerBrush: Brush
        get() = Brush.linearGradient(colors = listOf(primary, secondary))

    val splashBrush: Brush
        get() = Brush.verticalGradient(colors = listOf(primary, secondary, accent))
}

val LocalBrandColors = staticCompositionLocalOf { SchoolBrandColors() }

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: ThemePreset = ThemePreset.BLUE_TEAL,
    content: @Composable () -> Unit
) {
    val primaryColor = Color(themePreset.primaryHex)
    val secondaryColor = Color(themePreset.secondaryHex)
    val accentColor = Color(themePreset.accentHex)
    val lightContainer = Color(themePreset.lightBgHex)

    val brandColors = SchoolBrandColors(
        preset = themePreset,
        primary = primaryColor,
        secondary = secondaryColor,
        accent = accentColor,
        containerLight = lightContainer
    )

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = accentColor,
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF283440),
            onSecondaryContainer = Color(0xFFD0DCE5),
            tertiary = SneatInfo,
            onTertiary = Color.White,
            background = SneatDarkBg,
            onBackground = SneatDarkTextPrimary,
            surface = SneatDarkSurface,
            onSurface = SneatDarkTextPrimary,
            surfaceVariant = SneatDarkSurface,
            onSurfaceVariant = SneatDarkTextMuted,
            outline = SneatDarkCardBorder,
            error = SneatDanger,
            onError = Color.White,
            errorContainer = Color(0xFF4D2424),
            onErrorContainer = Color(0xFFFFB4AB)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = lightContainer,
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = SneatSecondaryLight,
            onSecondaryContainer = SneatSecondary,
            tertiary = SneatInfo,
            onTertiary = Color.White,
            background = SneatLightBg,
            onBackground = SneatLightTextPrimary,
            surface = SneatLightSurface,
            onSurface = SneatLightTextPrimary,
            surfaceVariant = Color(0xFFF3EFE7),
            onSurfaceVariant = SneatLightTextMuted,
            outline = SneatLightCardBorder,
            error = SneatDanger,
            onError = Color.White,
            errorContainer = SneatDangerLight,
            onErrorContainer = SneatDanger
        )
    }

    CompositionLocalProvider(LocalBrandColors provides brandColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
