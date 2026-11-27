package com.suzy.ai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
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
    orbSize: Dp = 230.dp,
    orbStyle: String = "plasma",
    primaryColor: Color = NeonPurple,
    secondaryColor: Color = NeonCyan,
    glowIntensity: Float = 1.0f,
    spinSpeed: Float = 1.0f
) {
    val speedFactor = spinSpeed.coerceIn(0.25f, 2.5f)
    val durationMs = (5200 / speedFactor).toInt().coerceAtLeast(1400)

    val transition = rememberInfiniteTransition(label = "orb_transition")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_angle"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breathe"
    )

    val activeAccent = when (state) {
        SuzyState.SPEAKING -> NeonPink
        SuzyState.LISTENING -> secondaryColor
        SuzyState.CONNECTING -> Color(0xFFF59E0B)
        SuzyState.ERROR -> Color(0xFFEF4444)
        SuzyState.IDLE -> primaryColor
    }

    val ampBoost = (amplitude * 0.24f).coerceIn(0f, 0.28f)
    val alphaBoost = (0.28f * glowIntensity).coerceIn(0.10f, 0.65f)

    Canvas(modifier = Modifier.size(orbSize)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)
        val baseR = (size.minDimension / 2f) * 0.72f * (breathe + ampBoost)

        // Outer ambient bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(activeAccent.copy(alpha = alphaBoost), Color.Transparent),
                center = center,
                radius = baseR * 1.38f
            ),
            radius = baseR * 1.38f,
            center = center
        )

        when (orbStyle) {
            "arc_reactor" -> {
                // Jarvis Segmented Arc Reactor
                drawCircle(color = Color(0xFF0A1024), radius = baseR * 0.95f, center = center)
                rotate(angle, pivot = center) {
                    for (i in 0 until 10) {
                        drawArc(
                            color = if (i % 2 == 0) secondaryColor else activeAccent,
                            startAngle = i * 36f + 4f,
                            sweepAngle = 24f,
                            useCenter = false,
                            topLeft = Offset(cx - baseR * 0.88f, cy - baseR * 0.88f),
                            size = Size(baseR * 1.76f, baseR * 1.76f),
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                rotate(-angle * 1.4f, pivot = center) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.85f),
                        radius = baseR * 0.56f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White, secondaryColor, Color.Transparent)),
                    radius = baseR * 0.36f,
                    center = center
                )
            }

            "quantum_rings" -> {
                // Gyroscope Orbital Rings
                for (ring in 1..4) {
                    val r = baseR * (0.35f + ring * 0.16f)
                    rotate(angle * (if (ring % 2 == 0) 1f else -1.2f) + ring * 30f, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, activeAccent, primaryColor)),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(cx - r, cy - r),
                            size = Size(r * 2f, r * 2f),
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                drawCircle(color = activeAccent, radius = baseR * 0.24f, center = center)
            }

            "solar_eclipse" -> {
                // Dark core with fiery corona
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.65f), activeAccent.copy(alpha = 0.25f), Color.Transparent),
                        center = center,
                        radius = baseR * 1.25f
                    ),
                    radius = baseR * 1.25f,
                    center = center
                )
                drawCircle(color = Color(0xFF04040A), radius = baseR * 0.72f, center = center)
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = baseR * 0.73f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            "cyber_dna" -> {
                // Audio-reactive radial equalizer bars
                val bars = 32
                for (i in 0 until bars) {
                    val deg = (i * (360f / bars)) + angle
                    val rad = Math.toRadians(deg.toDouble())
                    val wave = (sin(Math.toRadians((i * 25 + angle * 2).toDouble())).toFloat() * 0.5f + 0.5f)
                    val inner = baseR * 0.52f
                    val outer = baseR * (0.68f + wave * 0.34f + ampBoost)
                    drawLine(
                        color = if (i % 2 == 0) secondaryColor else activeAccent,
                        start = Offset(cx + cos(rad).toFloat() * inner, cy + sin(rad).toFloat() * inner),
                        end = Offset(cx + cos(rad).toFloat() * outer, cy + sin(rad).toFloat() * outer),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(color = primaryColor.copy(alpha = 0.45f), radius = baseR * 0.44f, center = center)
            }

            "breathing_halo" -> {
                // Minimalist Zen Halo
                drawCircle(
                    brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, activeAccent, primaryColor)),
                    radius = baseR * 0.82f,
                    center = center,
                    style = Stroke(width = 5.dp.toPx())
                )
            }

            else -> {
                // Classic Multi-Layer Plasma Sphere
                rotate(angle, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryColor, secondaryColor, activeAccent, primaryColor)
                        ),
                        radius = baseR * 0.92f,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            activeAccent.copy(alpha = 0.72f),
                            primaryColor.copy(alpha = 0.42f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseR * 0.82f
                    ),
                    radius = baseR * 0.82f,
                    center = center
                )
            }
        }
    }
}
