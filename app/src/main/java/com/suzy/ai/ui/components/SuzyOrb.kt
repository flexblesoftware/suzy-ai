package com.suzy.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.viewmodel.SuzyState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SuzyOrb(
    state: SuzyState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "orb_anim")

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    SuzyState.SPEAKING -> 3200
                    SuzyState.CONNECTING -> 2200
                    SuzyState.LISTENING -> 6000
                    else -> 10000
                },
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    val breathe by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val smoothAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(90),
        label = "amp"
    )

    val primaryColor = when (state) {
        SuzyState.SPEAKING -> NeonPink
        SuzyState.LISTENING -> NeonCyan
        SuzyState.CONNECTING -> NeonPurple
        SuzyState.ERROR -> Color(0xFFEF4444)
        SuzyState.IDLE -> NeonPurple
    }

    val secondaryColor = when (state) {
        SuzyState.SPEAKING -> NeonPurple
        SuzyState.LISTENING -> NeonPurple
        SuzyState.CONNECTING -> NeonCyan
        SuzyState.ERROR -> Color(0xFFF97316)
        SuzyState.IDLE -> NeonCyan
    }

    Canvas(modifier = modifier.size(270.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.30f
        val dynamicScale = breathe + (smoothAmp * 0.28f)
        val orbRadius = baseRadius * dynamicScale

        // Outer ambient halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.42f + smoothAmp * 0.25f),
                    secondaryColor.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = center,
                radius = orbRadius * 1.95f
            ),
            radius = orbRadius * 1.95f,
            center = center
        )

        // Orbiting plasma rings
        rotate(degrees = rotation, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.65f),
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = orbRadius * 1.28f,
                center = center,
                style = Stroke(width = 3.5f + smoothAmp * 5f)
            )
        }

        rotate(degrees = -rotation * 1.3f, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        secondaryColor.copy(alpha = 0.7f),
                        NeonPink.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = orbRadius * 1.45f,
                center = center,
                style = Stroke(width = 2.2f + smoothAmp * 3f)
            )
        }

        // Core glowing sphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    primaryColor.copy(alpha = 0.95f),
                    secondaryColor.copy(alpha = 0.85f),
                    Color(0xFF0E1020)
                ),
                center = Offset(center.x - orbRadius * 0.2f, center.y - orbRadius * 0.2f),
                radius = orbRadius * 1.15f
            ),
            radius = orbRadius,
            center = center
        )

        // Subtle orbiting energy particles
        val particleCount = 6
        for (i in 0 until particleCount) {
            val angleRad = Math.toRadians((rotation + i * (360.0 / particleCount)))
            val dist = orbRadius * (1.35f + (i % 2) * 0.14f)
            val px = center.x + (cos(angleRad) * dist).toFloat()
            val py = center.y + (sin(angleRad) * dist).toFloat()
            drawCircle(
                color = if (i % 2 == 0) primaryColor else secondaryColor,
                radius = 3.5f + smoothAmp * 3f,
                center = Offset(px, py)
            )
        }
    }
}
