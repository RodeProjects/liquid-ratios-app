package de.rodeprojects.liquidratios.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

internal val Background = Color.Black
internal val Ink = Color(0xFFF5F7FA)
internal val SecondaryInk = Color(0xFFBDC1C7)
internal val Divider = Color(0xFF747E87)
internal val Sky = Color(0xFF63BAF7)
internal val Panel = Color(0xFF121619)

internal data class LiquidColors(val accent: Color, val tint: Color, val light: Color, val deep: Color)

internal val liquidColors = listOf(
    LiquidColors(Color(0xFF2CA9FA), Color(0xFF061525), Color(0xFF349FFF), Color(0xFF0677E7)),
    LiquidColors(Color(0xFF00D790), Color(0xFF031B12), Color(0xFF0DD7A3), Color(0xFF008C6C)),
    LiquidColors(Color(0xFFC04AF7), Color(0xFF1A0A25), Color(0xFFB654FC), Color(0xFF811CF1)),
    LiquidColors(Color(0xFFFFAF40), Color(0xFF251607), Color(0xFFFFBA51), Color(0xFFE47715)),
)

private val palette = darkColorScheme(
    primary = Sky,
    onPrimary = Color(0xFF001A2B),
    background = Background,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    onSurfaceVariant = SecondaryInk,
    surfaceContainerHigh = Panel,
    outline = Divider,
    error = Color(0xFFFFB4AB),
)

@Composable
internal fun LiquidRatiosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = palette) {
        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp), content)
    }
}
