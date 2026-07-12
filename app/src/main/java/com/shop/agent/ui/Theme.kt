package com.shop.agent.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Palette boutique : vert commerce (confiance + croissance), accent ambre (prix)
val ShopLightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF0D3B12),
    secondary = Color(0xFFFF6F00),
    onSecondary = Color.White,
    tertiary = Color(0xFF1565C0),
    background = Color(0xFFF6F8F6),
    surface = Color.White,
    surfaceVariant = Color(0xFFE7EDE8),
    onSurface = Color(0xFF1B1F1A),
    onSurfaceVariant = Color(0xFF424940),
    error = Color(0xFFC62828),
    outline = Color(0xFFBDC7BF)
)

val ShopDarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF0D3B12),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF3A1E00),
    background = Color(0xFF121612),
    surface = Color(0xFF1B1F1A),
    surfaceVariant = Color(0xFF2A2F29),
    onSurface = Color(0xFFE2E3DE),
    onSurfaceVariant = Color(0xFFC2C9C0)
)
