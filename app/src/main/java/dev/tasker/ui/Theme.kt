package dev.tasker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.tasker.domain.TaskLogic

private val Scheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0B1E3F),
    primaryContainer = Color(0xFF2A4FA8),
    onPrimaryContainer = Color(0xFFE3ECFF),
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF242427),
    surfaceContainerHigh = Color(0xFF2C2C30),
)

@Composable
fun TaskerTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)

/** Amber and faint at priority 1, through orange, to saturated red at priority 10. */
fun urgencyColor(priority: Int): Color {
    val u = TaskLogic.urgency(priority)
    return Color.hsv(hue = 50f * (1f - u), saturation = 0.35f + 0.65f * u, value = 0.65f + 0.35f * u)
}
