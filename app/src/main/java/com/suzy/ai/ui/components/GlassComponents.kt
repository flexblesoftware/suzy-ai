package com.suzy.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.suzy.ai.data.SuzyCatalog
import com.suzy.ai.ui.theme.NeonCyan
import com.suzy.ai.ui.theme.NeonPurple

object ActiveUiStyleHolder {
    var skinId: String = "cyberpunk"
    var cornerRadiusDp: Int = 20
    var backdropPreset: String = "starfield"
}

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val skin = SuzyCatalog.uiThemeSkins.firstOrNull { it.id == ActiveUiStyleHolder.skinId }
        ?: SuzyCatalog.uiThemeSkins.first()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(skin.bgTop, skin.bgBottom, skin.bgTop)
                )
            )
    ) {
        if (ActiveUiStyleHolder.backdropPreset != "amoled" && skin.id != "amoled") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = skin.primary.copy(alpha = 0.08f),
                    radius = w * 0.45f,
                    center = Offset(w * 0.2f, h * 0.25f)
                )
                drawCircle(
                    color = skin.secondary.copy(alpha = 0.07f),
                    radius = w * 0.4f,
                    center = Offset(w * 0.85f, h * 0.72f)
                )
            }
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(ActiveUiStyleHolder.cornerRadiusDp.dp),
    accentBorder: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val skin = SuzyCatalog.uiThemeSkins.firstOrNull { it.id == ActiveUiStyleHolder.skinId }
        ?: SuzyCatalog.uiThemeSkins.first()

    val borderBrush = if (accentBorder != null) {
        Brush.linearGradient(listOf(accentBorder.copy(alpha = 0.78f), accentBorder.copy(alpha = 0.25f)))
    } else {
        Brush.linearGradient(listOf(skin.primary.copy(alpha = 0.26f), skin.secondary.copy(alpha = 0.14f)))
    }

    Box(
        modifier = modifier
            .background(
                color = Color(0xFF101326).copy(alpha = if (skin.id == "amoled") 0.88f else 0.72f),
                shape = shape
            )
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        content = content
    )
}
