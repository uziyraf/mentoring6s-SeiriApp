package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondarySlate,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = IndustrialNavy,
    tertiary = StatusAmber,
    onTertiary = Color.White,
    tertiaryContainer = StatusAmberContainer,
    onTertiaryContainer = Color(0xFF78350F),
    background = BackgroundLight,
    onBackground = IndustrialNavy,
    surface = SurfaceLight,
    onSurface = IndustrialNavy,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = SecondarySlate,
    outline = OutlineLight,
    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedContainer,
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = IndustrialNavy,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = IndustrialNavy,
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFFFBBF24),
    onTertiary = IndustrialNavy,
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun SeiriTheme(
    // Ubah default menjadi isSystemInDarkTheme() agar otomatis ikut HP
    darkTheme: Boolean = isSystemInDarkTheme(), 
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Memanggil file Type.kt yang baru
        content = content
    )
}