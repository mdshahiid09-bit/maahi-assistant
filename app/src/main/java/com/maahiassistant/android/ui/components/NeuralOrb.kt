package com.maahiassistant.android.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maahiassistant.android.data.api.models.SessionOrbState
import com.maahiassistant.android.ui.theme.OrbCrimson
import com.maahiassistant.android.ui.theme.OrbDarkCoreEnd
import com.maahiassistant.android.ui.theme.OrbDarkCoreStart
import com.maahiassistant.android.ui.theme.OrbGold
import com.maahiassistant.android.ui.theme.OrbGoldBright
import com.maahiassistant.android.ui.theme.OrbMutedSlate
import com.maahiassistant.android.ui.theme.OrbRedGold
import com.maahiassistant.android.ui.theme.OrbRedOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NeuralOrb(
    state: SessionOrbState,
    audioInputLevel: Float,
    audioOutputLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite")

    // Idle breathing animation
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Orbital rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == SessionOrbState.CONNECTING) 2500 else 12000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Smooth audio levels
    val smoothInputLevel by animateFloatAsState(
        targetValue = audioInputLevel.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "smoothInput"
    )
    val smoothOutputLevel by animateFloatAsState(
        targetValue = audioOutputLevel.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "smoothOutput"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.72f

            // Dynamic scale factor based on state & audio level
            val dynamicScale = when (state) {
                SessionOrbState.IDLE -> breathingScale
                SessionOrbState.CONNECTING -> 1.0f + (breathingScale - 0.94f) * 0.5f
                SessionOrbState.LISTENING -> 1.0f + (smoothInputLevel * 0.14f)
                SessionOrbState.SPEAKING -> 1.0f + (smoothOutputLevel * 0.22f)
                SessionOrbState.MUTED -> 0.96f
                SessionOrbState.ERROR -> 1.0f + (breathingScale - 0.94f) * 0.8f
            }

            val currentRadius = baseRadius * dynamicScale

            // 1. External Aura / Glow
            val glowColor = when (state) {
                SessionOrbState.IDLE -> OrbGold.copy(alpha = 0.12f)
                SessionOrbState.CONNECTING -> OrbGoldBright.copy(alpha = 0.25f)
                SessionOrbState.LISTENING -> OrbRedGold.copy(alpha = 0.28f + smoothInputLevel * 0.2f)
                SessionOrbState.SPEAKING -> OrbRedOrange.copy(alpha = 0.35f + smoothOutputLevel * 0.35f)
                SessionOrbState.MUTED -> OrbMutedSlate.copy(alpha = 0.10f)
                SessionOrbState.ERROR -> OrbCrimson.copy(alpha = 0.35f)
            }
            drawCircle(
                color = glowColor,
                radius = currentRadius * 1.25f,
                center = center
            )

            // 2. Outer Red-Gold Ring
            val ringColors = when (state) {
                SessionOrbState.IDLE -> listOf(OrbGold.copy(alpha = 0.6f), OrbRedGold.copy(alpha = 0.4f), OrbGold.copy(alpha = 0.6f))
                SessionOrbState.CONNECTING -> listOf(OrbGoldBright, OrbGold, OrbGoldBright)
                SessionOrbState.LISTENING -> listOf(OrbRedOrange, OrbGoldBright, OrbRedGold, OrbRedOrange)
                SessionOrbState.SPEAKING -> listOf(OrbRedOrange, OrbGoldBright, Color.White, OrbRedOrange)
                SessionOrbState.MUTED -> listOf(OrbMutedSlate.copy(alpha = 0.4f), OrbMutedSlate.copy(alpha = 0.7f))
                SessionOrbState.ERROR -> listOf(OrbCrimson, Color(0xFFFF8A80), OrbCrimson)
            }
            val ringStrokeWidth = when (state) {
                SessionOrbState.SPEAKING -> 4.dp.toPx() + (smoothOutputLevel * 6.dp.toPx())
                SessionOrbState.LISTENING -> 3.5.dp.toPx() + (smoothInputLevel * 3.dp.toPx())
                else -> 3.dp.toPx()
            }
            drawCircle(
                brush = Brush.sweepGradient(ringColors, center),
                radius = currentRadius * 1.02f,
                center = center,
                style = Stroke(width = ringStrokeWidth, cap = StrokeCap.Round)
            )

            // 3. Dark Core
            val coreBrush = Brush.radialGradient(
                colors = listOf(
                    when (state) {
                        SessionOrbState.ERROR -> Color(0xFF2B0A0D)
                        SessionOrbState.MUTED -> Color(0xFF1E222A)
                        else -> OrbDarkCoreEnd
                    },
                    when (state) {
                        SessionOrbState.ERROR -> Color(0xFF150406)
                        SessionOrbState.MUTED -> Color(0xFF0F1218)
                        else -> OrbDarkCoreStart
                    }
                ),
                center = center,
                radius = currentRadius
            )
            drawCircle(
                brush = coreBrush,
                radius = currentRadius,
                center = center
            )

            // 4. Golden Neural Nodes & Synapses
            val nodeCount = 10
            val nodeOffsets = ArrayList<Offset>(nodeCount)
            val radAngle = (rotationAngle * PI / 180.0).toFloat()

            for (i in 0 until nodeCount) {
                val nodeBaseAngle = (i.toFloat() / nodeCount) * 2f * PI.toFloat()
                val currentAngle = nodeBaseAngle + (if (i % 2 == 0) radAngle else -radAngle * 0.8f)
                val distanceRatio = 0.35f + (0.45f * ((i % 3) / 2f))
                val nodeDist = currentRadius * distanceRatio * (1f + (if (state == SessionOrbState.SPEAKING) smoothOutputLevel * 0.15f else 0f))
                val nx = center.x + nodeDist * cos(currentAngle)
                val ny = center.y + nodeDist * sin(currentAngle)
                nodeOffsets.add(Offset(nx, ny))
            }

            // Draw neural synapses (connecting lines)
            val synapseColor = when (state) {
                SessionOrbState.ERROR -> OrbCrimson.copy(alpha = 0.35f)
                SessionOrbState.MUTED -> OrbMutedSlate.copy(alpha = 0.2f)
                SessionOrbState.CONNECTING -> OrbGoldBright.copy(alpha = 0.5f)
                SessionOrbState.LISTENING -> OrbGold.copy(alpha = 0.45f + smoothInputLevel * 0.3f)
                SessionOrbState.SPEAKING -> OrbGoldBright.copy(alpha = 0.55f + smoothOutputLevel * 0.4f)
                SessionOrbState.IDLE -> OrbGold.copy(alpha = 0.25f)
            }

            for (i in 0 until nodeCount) {
                val next = (i + 1) % nodeCount
                drawLine(
                    color = synapseColor,
                    start = nodeOffsets[i],
                    end = nodeOffsets[next],
                    strokeWidth = 1.2.dp.toPx()
                )
                // Connect some nodes to center core
                if (i % 2 == 0) {
                    drawLine(
                        color = synapseColor.copy(alpha = synapseColor.alpha * 0.7f),
                        start = center,
                        end = nodeOffsets[i],
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Draw golden node dots
            val nodeDotColor = when (state) {
                SessionOrbState.ERROR -> OrbCrimson
                SessionOrbState.MUTED -> OrbMutedSlate
                SessionOrbState.CONNECTING -> OrbGoldBright
                SessionOrbState.LISTENING -> OrbGoldBright
                SessionOrbState.SPEAKING -> Color.White
                SessionOrbState.IDLE -> OrbGold
            }

            for (i in 0 until nodeCount) {
                val nodeRadius = (2.4.dp.toPx()) * (if (i % 2 == 0) 1.3f else 1.0f) *
                        (1f + (if (state == SessionOrbState.SPEAKING) smoothOutputLevel * 0.6f else 0f))
                drawCircle(
                    color = nodeDotColor,
                    radius = nodeRadius,
                    center = nodeOffsets[i]
                )
            }

            // 5. Center Golden Core Spark
            val centerSparkColor = when (state) {
                SessionOrbState.ERROR -> OrbCrimson
                SessionOrbState.MUTED -> OrbMutedSlate.copy(alpha = 0.5f)
                SessionOrbState.CONNECTING -> OrbGoldBright
                SessionOrbState.LISTENING -> OrbGold
                SessionOrbState.SPEAKING -> Color.White
                SessionOrbState.IDLE -> OrbGold.copy(alpha = 0.7f)
            }
            val centerSparkRadius = (4.dp.toPx()) * (1f + (if (state == SessionOrbState.SPEAKING) smoothOutputLevel * 0.8f else 0f))
            drawCircle(
                color = centerSparkColor,
                radius = centerSparkRadius,
                center = center
            )
        }
    }
}
