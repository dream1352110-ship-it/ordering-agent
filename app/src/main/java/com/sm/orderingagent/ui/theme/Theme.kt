package com.sm.orderingagent.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DemoColorScheme = lightColorScheme(
    primary = DemoAccent,
    onPrimary = Color.White,
    primaryContainer = DemoAccentSoft,
    onPrimaryContainer = DemoNavy,
    secondary = DemoGreen,
    onSecondary = Color.White,
    secondaryContainer = DemoGreenSoft,
    onSecondaryContainer = DemoNavy,
    tertiary = DemoAmber,
    background = DemoPaper,
    onBackground = DemoInk,
    surface = DemoSurface,
    onSurface = DemoInk,
    surfaceVariant = DemoSurfaceWarm,
    onSurfaceVariant = DemoInkSoft,
    outline = DemoLine,
    outlineVariant = DemoLineStrong,
    error = DemoDanger,
    onError = Color.White
)

val DemoShapes = Shapes(
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(13.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SmartorderingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DemoColorScheme,
        shapes = DemoShapes,
        typography = Typography,
        content = content
    )
}
