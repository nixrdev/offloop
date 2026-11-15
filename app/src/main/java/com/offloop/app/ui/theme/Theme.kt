package com.offloop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OffloopColors = darkColorScheme(
    primary = Color(0xFFC8F56A),
    onPrimary = Color(0xFF172000),
    secondary = Color(0xFFB8C4A8),
    onSecondary = Color(0xFF15200E),
    background = Color(0xFF0C0F0A),
    onBackground = Color(0xFFE6E9DE),
    surface = Color(0xFF141810),
    onSurface = Color(0xFFE6E9DE),
    surfaceVariant = Color(0xFF20251B),
    onSurfaceVariant = Color(0xFFC3C8B8),
    outline = Color(0xFF8C9482)
)

@Composable
fun OffloopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OffloopColors,
        content = content
    )
}
