package dev.tasker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Surface0 = Color(0xFF1C1C1E)
val Surface1 = Color(0xFF2A2A2D)
val Accent = Color(0xFF8AB4F8)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF0B1E3F),
    primaryContainer = Color(0xFF2650A8),
    onPrimaryContainer = Color.White,
    background = Surface0,
    surface = Surface0,
    surfaceContainer = Surface1,
    surfaceVariant = Surface1,
)

/** Dark is the default (and only) theme. */
@Composable
fun TaskerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
