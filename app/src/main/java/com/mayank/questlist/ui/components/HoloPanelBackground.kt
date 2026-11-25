package com.mayank.questlist.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mayank.questlist.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ScratchLine(
    val relX: Float,
    val relY: Float,
    val lengthDp: Float,
    val alpha: Float,
    val strokeWidthDp: Float
)

private data class CircuitTrace(
    val p1Rel: Offset,
    val p2Rel: Offset,
    val p3Rel: Offset,
    val alpha: Float
)

private data class SpeckleDot(
    val relX: Float,
    val relY: Float,
    val alpha: Float,
    val radiusPx: Float
)

private data class SmokeOrb(
    val relX: Float,
    val relY: Float,
    val radiusFactor: Float,
    val color: Color,
    val phaseOffset: Float
)

@Composable
fun HoloPanelBackground(
    modifier: Modifier = Modifier,
    isLowPower: Boolean = false,
    cornerCut: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = CutCornerShape(cornerCut)

    val geometry = remember {
        val rand = Random(42L)

        val scratches = List(60) {
            ScratchLine(
                relX = rand.nextFloat(),
                relY = rand.nextFloat(),
                lengthDp = 40f + rand.nextFloat() * 180f,
                alpha = 0.04f + rand.nextFloat() * 0.06f,
                strokeWidthDp = 0.5f + rand.nextFloat() * 0.5f
            )
        }

        val circuitTraces = List(12) {
            val isTop = rand.nextBoolean()
            val startX = if (rand.nextBoolean()) rand.nextFloat() * 0.3f else 0.7f + rand.nextFloat() * 0.3f
            val startY = if (isTop) rand.nextFloat() * 0.2f else 0.8f + rand.nextFloat() * 0.2f
            val midX = startX + (if (rand.nextBoolean()) 0.08f else -0.08f)
            val endY = startY + (if (isTop) 0.08f else -0.08f)

            CircuitTrace(
                p1Rel = Offset(startX, startY),
                p2Rel = Offset(midX, startY),
                p3Rel = Offset(midX, endY),
                alpha = 0.06f + rand.nextFloat() * 0.04f
            )
        }

        val speckles = List(200) {
            SpeckleDot(
                relX = rand.nextFloat(),
                relY = rand.nextFloat(),
                alpha = 0.03f + rand.nextFloat() * 0.05f,
                radiusPx = 1f + rand.nextFloat() * 1.5f
            )
        }

        val smokeOrbs = listOf(
            SmokeOrb(0.1f, 0.15f, 0.45f, MainGlow, 0f),
            SmokeOrb(0.9f, 0.2f, 0.4f, BrightAccent, 1.2f),
            SmokeOrb(0.08f, 0.8f, 0.5f, BrightAccent, 2.5f),
            SmokeOrb(0.92f, 0.85f, 0.45f, MainGlow, 3.8f),
            SmokeOrb(0.5f, 0.5f, 0.6f, MainGlow, 5.1f)
        )

        Triple(scratches, circuitTraces, Pair(speckles, smokeOrbs))
    }

    val (scratches, circuitTraces, specklesAndOrbs) = geometry
    val (speckles, smokeOrbs) = specklesAndOrbs

    val infiniteTransition = rememberInfiniteTransition(label = "HoloDrift")
    val driftAnim by if (!isLowPower) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28318f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "DriftPhase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val maxDim = maxOf(w, h)

                val scanlineStep = 3.dp.toPx()

                onDrawBehind {
                    // 1. Base solid background #03060D
                    drawRect(Color(0xFF03060D))

                    // 2. Smoke/glow radial gradients
                    val orbAlphaMultiplier = if (isLowPower) 0.6f else 1.0f
                    smokeOrbs.forEach { orb ->
                        val driftX = if (!isLowPower) sin(driftAnim.toDouble() + orb.phaseOffset).toFloat() * 0.025f else 0f
                        val driftY = if (!isLowPower) cos(driftAnim.toDouble() + orb.phaseOffset).toFloat() * 0.025f else 0f

                        val center = Offset((orb.relX + driftX) * w, (orb.relY + driftY) * h)
                        val radius = maxDim * orb.radiusFactor

                        val orbBrush = Brush.radialGradient(
                            colors = listOf(
                                orb.color.copy(alpha = 0.12f * orbAlphaMultiplier),
                                orb.color.copy(alpha = 0.04f * orbAlphaMultiplier),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        )
                        drawRect(brush = orbBrush)
                    }

                    // 3. Scratches (thin diagonal lines ~45 deg)
                    scratches.forEach { scratch ->
                        val strokePx = scratch.strokeWidthDp.dp.toPx()
                        val lenPx = scratch.lengthDp.dp.toPx()
                        val start = Offset(scratch.relX * w, scratch.relY * h)
                        val end = Offset(start.x + lenPx * 0.707f, start.y + lenPx * 0.707f)
                        val scratchAlpha = if (isLowPower) scratch.alpha * 0.5f else scratch.alpha

                        drawLine(
                            color = BrightAccent.copy(alpha = scratchAlpha),
                            start = start,
                            end = end,
                            strokeWidth = strokePx
                        )
                    }

                    // 4. Circuit traces
                    circuitTraces.forEach { trace ->
                        val p1 = Offset(trace.p1Rel.x * w, trace.p1Rel.y * h)
                        val p2 = Offset(trace.p2Rel.x * w, trace.p2Rel.y * h)
                        val p3 = Offset(trace.p3Rel.x * w, trace.p3Rel.y * h)
                        val traceAlpha = if (isLowPower) trace.alpha * 0.5f else trace.alpha

                        drawLine(BrightAccent.copy(alpha = traceAlpha), p1, p2, strokeWidth = 1.dp.toPx())
                        drawLine(BrightAccent.copy(alpha = traceAlpha), p2, p3, strokeWidth = 1.dp.toPx())

                        drawCircle(BrightAccent.copy(alpha = traceAlpha * 1.5f), radius = 2.5.dp.toPx(), center = p2)
                        drawCircle(BrightAccent.copy(alpha = traceAlpha * 1.5f), radius = 2.5.dp.toPx(), center = p3)
                    }

                    // 5. Noise speckles
                    speckles.forEach { dot ->
                        val speckleAlpha = if (isLowPower) dot.alpha * 0.5f else dot.alpha
                        drawCircle(
                            color = TextBlueWhite.copy(alpha = speckleAlpha),
                            radius = dot.radiusPx,
                            center = Offset(dot.relX * w, dot.relY * h)
                        )
                    }

                    // 6. Vignette gradient
                    val vignetteBrush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x99000000)
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = maxDim * 0.7f
                    )
                    drawRect(vignetteBrush)

                    // 7. Scanlines
                    var sy = 0f
                    val scanlineAlpha = if (isLowPower) 0.015f else 0.03f
                    while (sy < h) {
                        drawLine(
                            color = BrightAccent.copy(alpha = scanlineAlpha),
                            start = Offset(0f, sy),
                            end = Offset(w, sy),
                            strokeWidth = 1.dp.toPx()
                        )
                        sy += scanlineStep
                    }

                    // 8. Inner frame (thin 1dp #CFE8FF at 60% alpha inset 12dp)
                    val inset = 12.dp.toPx()
                    if (w > inset * 2 && h > inset * 2) {
                        val innerPath = Path().apply {
                            addRect(Rect(inset, inset, w - inset, h - inset))
                        }
                        drawPath(
                            path = innerPath,
                            color = TextBlueWhite.copy(alpha = 0.6f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // 9. Outer frame
                    val railAlpha = if (isLowPower) 0.5f else 0.8f

                    drawLine(
                        color = BrightAccent.copy(alpha = railAlpha),
                        start = Offset(0f, h * 0.15f),
                        end = Offset(0f, h * 0.85f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = BrightAccent.copy(alpha = railAlpha),
                        start = Offset(w, h * 0.15f),
                        end = Offset(w, h * 0.85f),
                        strokeWidth = 2.dp.toPx()
                    )

                    val barThickness = 4.dp.toPx()
                    drawRect(
                        color = MainGlow.copy(alpha = railAlpha),
                        topLeft = Offset(w * 0.15f, 0f),
                        size = Size(w * 0.7f, barThickness)
                    )
                    drawRect(
                        color = MainGlow.copy(alpha = railAlpha),
                        topLeft = Offset(w * 0.15f, h - barThickness),
                        size = Size(w * 0.7f, barThickness)
                    )

                    val notchPathTop = Path().apply {
                        moveTo(w * 0.82f, 0f)
                        lineTo(w * 0.88f, 0f)
                        lineTo(w * 0.91f, 8.dp.toPx())
                        lineTo(w * 0.95f, 8.dp.toPx())
                    }
                    drawPath(
                        path = notchPathTop,
                        color = BrightAccent.copy(alpha = railAlpha),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val notchPathBottom = Path().apply {
                        moveTo(w * 0.18f, h)
                        lineTo(w * 0.12f, h)
                        lineTo(w * 0.09f, h - 8.dp.toPx())
                        lineTo(w * 0.05f, h - 8.dp.toPx())
                    }
                    drawPath(
                        path = notchPathBottom,
                        color = BrightAccent.copy(alpha = railAlpha),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            .padding(18.dp)
    ) {
        Column {
            content()
        }
    }
}