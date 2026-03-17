package com.kamenrider.simulator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFFE91E63),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF880E4F),
    secondary        = Color(0xFF42A5F5),
    onSecondary      = Color.White,
    tertiary         = Color(0xFFFFD700),
    background       = Color(0xFF0A0A1A),
    onBackground     = Color.White,
    surface          = Color(0xFF1A1A2E),
    onSurface        = Color.White,
    surfaceVariant   = Color(0xFF16213E),
    error            = Color(0xFFCF6679)
)

@Composable
fun KamenRiderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = androidx.compose.material3.Typography(),
        content     = content
    )
}
