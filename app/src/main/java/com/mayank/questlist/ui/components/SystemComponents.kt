package com.mayank.questlist.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mayank.questlist.ui.theme.*

@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scanlineSpacing = 3.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color(0x0A3FA9FF),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += scanlineSpacing
        }
    }
}

@Composable
fun GlowPanel(
    modifier: Modifier = Modifier,
    borderAlpha: Float = 1f,
    content: @Composable ColumnScope.() -> Unit
) {
    HoloPanelBackground(
        modifier = modifier,
        isLowPower = true,
        content = content
    )
}

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    textColor: Color = TextBlueWhite,
    borderColor: Color = BrightAccent
) {
    val shape = CutCornerShape(10.dp)
    val bg = if (isPrimary) FaintBlueFill else Color.Transparent

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) bg else Color.Transparent)
            .border(
                1.dp,
                if (enabled) borderColor else TextMutedBlue.copy(alpha = 0.4f),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = if (enabled) textColor else TextMutedBlue,
            style = TextStyle(shadow = if (enabled) TextGlowShadow else null)
        )
    }
}

@Composable
fun CircuitFrame(
    modifier: Modifier = Modifier,
    pulseGlow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    HoloPanelBackground(
        modifier = modifier,
        isLowPower = !pulseGlow,
        content = content
    )
}

@Composable
fun SystemCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "CheckboxScale"
    )

    Box(
        modifier = modifier
            .size(30.dp)
            .scale(scale)
            .clickable(onClick = onCheckedChange),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .clip(CutCornerShape(4.dp))
                .border(1.5.dp, GlitchRedOffset, CutCornerShape(4.dp))
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CutCornerShape(4.dp))
                .background(if (checked) StatusSuccess.copy(alpha = 0.25f) else Color.Transparent)
                .border(
                    1.5.dp,
                    if (checked) StatusSuccess else BrightAccent,
                    CutCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Checked",
                    tint = StatusSuccess,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HeaderBox(
    title: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {
        Text(
            text = "!",
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextBlueWhite,
            style = TextStyle(shadow = TextGlowShadow)
        )
    }
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CutCornerShape(6.dp))
                .background(PanelBackground)
                .border(1.dp, TextBlueWhite.copy(alpha = 0.8f), CutCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(CutCornerShape(6.dp))
                .background(PanelBackground)
                .border(1.dp, TextBlueWhite.copy(alpha = 0.8f), CutCornerShape(6.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title.uppercase(),
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                color = TextBlueWhite,
                style = TextStyle(shadow = TextGlowShadow)
            )
        }
    }
}