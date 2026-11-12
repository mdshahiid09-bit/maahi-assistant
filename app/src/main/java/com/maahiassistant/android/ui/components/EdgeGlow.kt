package com.maahiassistant.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maahiassistant.android.data.api.models.SessionOrbState
import com.maahiassistant.android.ui.theme.OrbCrimson
import com.maahiassistant.android.ui.theme.OrbGold
import com.maahiassistant.android.ui.theme.OrbGoldBright
import com.maahiassistant.android.ui.theme.OrbMutedSlate
import com.maahiassistant.android.ui.theme.OrbRedGold

@Composable
fun EdgeGlow(
    isSessionActive: Boolean,
    orbState: SessionOrbState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "edge_glow_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edge_pulse"
    )

    AnimatedVisibility(
        visible = isSessionActive,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(400)),
        modifier = modifier
    ) {
        val baseColor = when (orbState) {
            SessionOrbState.IDLE -> OrbGold
            SessionOrbState.CONNECTING -> OrbGoldBright
            SessionOrbState.LISTENING -> OrbRedGold
            SessionOrbState.SPEAKING -> OrbGoldBright
            SessionOrbState.MUTED -> OrbMutedSlate
            SessionOrbState.ERROR -> OrbCrimson
        }

        val glowColor = baseColor.copy(alpha = pulseAlpha * 0.45f)
        val edgeThickness = 12.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val thicknessPx = edgeThickness.toPx()
            val w = size.width
            val h = size.height

            // Top edge glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    startY = 0f,
                    endY = thicknessPx
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, thicknessPx)
            )

            // Bottom edge glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, glowColor),
                    startY = h - thicknessPx,
                    endY = h
                ),
                topLeft = Offset(0f, h - thicknessPx),
                size = Size(w, thicknessPx)
            )

            // Left edge glow
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    startX = 0f,
                    endX = thicknessPx
                ),
                topLeft = Offset(0f, 0f),
                size = Size(thicknessPx, h)
            )

            // Right edge glow
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, glowColor),
                    startX = w - thicknessPx,
                    endX = w
                ),
                topLeft = Offset(w - thicknessPx, 0f),
                size = Size(thicknessPx, h)
            )
        }
    }
}
