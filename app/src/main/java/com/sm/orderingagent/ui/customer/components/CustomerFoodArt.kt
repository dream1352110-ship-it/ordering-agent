package com.sm.orderingagent.ui.customer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BurgerArt(modifier: Modifier = Modifier.size(52.dp, 43.dp)) {
    Canvas(modifier = modifier) {
        val strokeColor = Color(0xFF3A2417)
        val strokeWidth = 1.4.dp.toPx()

        // Top Bun
        val topBunPath = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.40f)
            cubicTo(
                size.width * 0.14f, size.height * 0.05f,
                size.width * 0.86f, size.height * 0.05f,
                size.width * 0.88f, size.height * 0.40f
            )
            close()
        }
        drawPath(topBunPath, color = Color(0xFFF2C45A))
        drawPath(topBunPath, color = strokeColor, style = Stroke(strokeWidth))

        // Lettuce (Green layer)
        val lettucePath = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.42f)
            lineTo(size.width * 0.86f, size.height * 0.42f)
            lineTo(size.width * 0.81f, size.height * 0.56f)
            lineTo(size.width * 0.19f, size.height * 0.56f)
            close()
        }
        drawPath(lettucePath, color = Color(0xFF5B8F47))
        drawPath(lettucePath, color = strokeColor, style = Stroke(strokeWidth))

        // Patty (Dark red/brown layer)
        val pattyPath = Path().apply {
            moveTo(size.width * 0.17f, size.height * 0.60f)
            lineTo(size.width * 0.83f, size.height * 0.60f)
            lineTo(size.width * 0.77f, size.height * 0.70f)
            lineTo(size.width * 0.23f, size.height * 0.70f)
            close()
        }
        drawPath(pattyPath, color = Color(0xFFB84B2D))
        drawPath(pattyPath, color = strokeColor, style = Stroke(strokeWidth))

        // Bottom Bun (Gold layer)
        val bottomBunPath = Path().apply {
            moveTo(size.width * 0.20f, size.height * 0.74f)
            lineTo(size.width * 0.80f, size.height * 0.74f)
            lineTo(size.width * 0.74f, size.height * 0.84f)
            lineTo(size.width * 0.26f, size.height * 0.84f)
            close()
        }
        drawPath(bottomBunPath, color = Color(0xFFE7A541))
        drawPath(bottomBunPath, color = strokeColor, style = Stroke(strokeWidth))
    }
}

@Composable
fun WrapArt(modifier: Modifier = Modifier.size(52.dp, 43.dp)) {
    Canvas(modifier = modifier) {
        val strokeColor = Color(0xFF3A2417)
        val strokeWidth = 1.4.dp.toPx()

        // Wrap Body (Tortilla tan)
        val wrapPath = Path().apply {
            moveTo(size.width * 0.20f, size.height * 0.70f)
            cubicTo(
                size.width * 0.25f, size.height * 0.42f,
                size.width * 0.47f, size.height * 0.23f,
                size.width * 0.76f, size.height * 0.25f
            )
            lineTo(size.width * 0.68f, size.height * 0.65f)
            lineTo(size.width * 0.34f, size.height * 0.94f)
            close()
        }
        drawPath(wrapPath, color = Color(0xFFE8D2A8))
        drawPath(wrapPath, color = strokeColor, style = Stroke(strokeWidth))

        // Green filling stripe
        val greenStripe = Path().apply {
            moveTo(size.width * 0.33f, size.height * 0.60f)
            cubicTo(
                size.width * 0.45f, size.height * 0.46f,
                size.width * 0.61f, size.height * 0.40f,
                size.width * 0.80f, size.height * 0.40f
            )
        }
        drawPath(greenStripe, color = Color(0xFF5B8F47), style = Stroke(3.dp.toPx()))

        // Red/Orange sauce stripe
        val redStripe = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.70f)
            cubicTo(
                size.width * 0.48f, size.height * 0.62f,
                size.width * 0.59f, size.height * 0.58f,
                size.width * 0.70f, size.height * 0.60f
            )
        }
        drawPath(redStripe, color = Color(0xFFBD542F), style = Stroke(2.dp.toPx()))
    }
}
