package com.suzy.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPink
import com.suzy.ai.ui.theme.NeonPurple
import com.suzy.ai.viewmodel.SuzyState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SuzyOrb(
    state: SuzyState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val smoothAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "orb_amp"
    )

    val infinite = rememberInfiniteTransition(label = "orb_loop")

    val primaryAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    SuzyState.SPEAKING -> 2600
                    SuzyState.CONNECTING -> 1600
                    SuzyState.LISTENING -> 4200
                    else -> 7500
                },
                easing = LinearEasing
            )
        ),
        label = "primary_rot"
    )

    val secondaryAngle by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5800, easing = LinearEasing)
        ),
        label = "secondary_rot"
    )

    val breathe by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breathe"
    )

    val primaryColor by animateColorAsState(
        targetValue = when (state) {
            SuzyState.SPEAKING -> NeonPink
            SuzyState.LISTENING -> NeonCyan
            SuzyState.CONNECTING -> Color(0xFF818CF8)
            SuzyState.ERROR -> Color(0xFFEF4444)
            SuzyState.IDLE -> NeonPurple
        },
        animationSpec = tween(450),
        label = "primary_color"
    )

    val secondaryColor by animateColorAsState(
        targetValue = when (state) {
            SuzyState.SPEAKING -> NeonPurple
            SuzyState.LISTENING -> Color(0xFF38BDF8)
            SuzyState.CONNECTING -> NeonCyan
            SuzyState.ERROR -> Color(0xFFF97316)
            SuzyState.IDLE -> NeonPink
        },
        animationSpec = tween(450),
        label = "secondary_color"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(290.dp)
    ) {
        Canvas(modifier = Modifier.size(290.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.27f
            val dynamicRadius = baseRadius * breathe + (smoothAmp * 26f)

            // 1. Deep Ambient Outer Bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.42f + smoothAmp * 0.25f),
                        secondaryColor.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 2.15f
                ),
                radius = dynamicRadius * 2.15f,
                center = center
            )

            // 2. 36-Bar Audio-Reactive Waveform Halo
            val barCount = 36
            val haloRadius = dynamicRadius * 1.32f
            for (i in 0 until barCount) {
                val angleDeg = (i * (360f / barCount)) + (primaryAngle * 0.35f)
                val angleRad = angleDeg * (PI / 180.0)
                val waveFactor = (sin((i * 0.65) + (primaryAngle * 0.08)) + 1.0) * 0.5
                val barLen = if (state == SuzyState.SPEAKING || state == SuzyState.LISTENING) {
                    6f + (smoothAmp * 34f * waveFactor.toFloat())
                } else {
                    4f + (waveFactor.toFloat() * 5f)
                }

                val start = Offset(
                    x = center.x + (cos(angleRad) * haloRadius).toFloat(),
                    y = center.y + (sin(angleRad) * haloRadius).toFloat()
                )
                val end = Offset(
                    x = center.x + (cos(angleRad) * (haloRadius + barLen)).toFloat(),
                    y = center.y + (sin(angleRad) * (haloRadius + barLen)).toFloat()
                )

                drawLine(
                    color = if (i % 2 == 0) primaryColor.copy(alpha = 0.78f) else secondaryColor.copy(alpha = 0.65f),
                    start = start,
                    end = end,
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Outer Counter-Rotating Neon Orbital Ring
            rotate(degrees = primaryAngle, pivot = center) {
                val ringRadius = dynamicRadius * 1.24f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.85f),
                            secondaryColor,
                            Color.White.copy(alpha = 0.9f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
            }

            // 4. Inner Counter-Rotating Plasma Ring
            rotate(degrees = secondaryAngle, pivot = center) {
                val innerRing = dynamicRadius * 1.11f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryColor.copy(alpha = 0.75f),
                            primaryColor,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 40f,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRing, center.y - innerRing),
                    size = Size(innerRing * 2f, innerRing * 2f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            // 5. 3D Glass-Plasma Sphere Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        primaryColor.copy(alpha = 0.95f),
                        secondaryColor.copy(alpha = 0.88f),
                        Color(0xFF0A061A)
                    ),
                    center = Offset(center.x - dynamicRadius * 0.25f, center.y - dynamicRadius * 0.28f),
                    radius = dynamicRadius * 1.25f
                ),
                radius = dynamicRadius,
                center = center
            )

            // 6. Internal Swirling Energy Core
            rotate(degrees = primaryAngle * 1.4f, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f + smoothAmp * 0.25f),
                            primaryColor.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    radius = dynamicRadius * 0.82f,
                    center = center
                )
            }

            // 7. Glossy Glass Rim & Specular Highlight
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.5f)
                    ),
                    start = Offset(center.x - dynamicRadius, center.y - dynamicRadius),
                    end = Offset(center.x + dynamicRadius, center.y + dynamicRadius)
                ),
                radius = dynamicRadius,
                center = center,
                style = Stroke(width = 2.2f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(center.x - dynamicRadius * 0.34f, center.y - dynamicRadius * 0.36f),
                    radius = dynamicRadius * 0.38f
                ),
                radius = dynamicRadius * 0.38f,
                center = Offset(center.x - dynamicRadius * 0.34f, center.y - dynamicRadius * 0.36f)
            )
        }
    }
}
