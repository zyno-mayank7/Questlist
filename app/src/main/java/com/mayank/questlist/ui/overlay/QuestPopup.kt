package com.mayank.questlist.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mayank.questlist.ui.components.*
import com.mayank.questlist.ui.theme.*
import com.mayank.questlist.util.TimeUtils
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun QuestPopup(
    taskTitle: String,
    goalText: String? = null,
    endMinutes: Int = 1439,
    warningMessage: String = "Failure to complete the daily quest will result in a penalty.",
    onComplete: () -> Unit = {},
    onSnooze: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var isTicked by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }

    // Live countdown timer to end of window
    var remainingSeconds by remember {
        val nowMin = TimeUtils.getCurrentMinutes()
        val secs = if (endMinutes > nowMin) (endMinutes - nowMin) * 60 else 3600
        mutableStateOf(secs)
    }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }

    val hours = remainingSeconds / 3600
    val mins = (remainingSeconds % 3600) / 60
    val secs = remainingSeconds % 60
    val timeLeftStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)

    // 35% black scrim background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x59000000))
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            exit = fadeOut(animationSpec = tween(300))
        ) {
            CircuitFrame(
                modifier = Modifier.fillMaxWidth(),
                pulseGlow = true
            ) {
                // Header
                HeaderBox(title = "NOTIFICATION")

                Spacer(modifier = Modifier.height(14.dp))

                // Countdown Timer
                Text(
                    text = "TIME LEFT $timeLeftStr",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightAccent,
                    letterSpacing = 1.5.sp,
                    style = TextStyle(shadow = TextGlowShadow)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bracketed Quest Text
                Text(
                    text = buildAnnotatedString {
                        append("[Daily Quest: ")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                color = BrightAccent
                            )
                        ) {
                            append(taskTitle)
                        }
                        append(" has arrived.]")
                    },
                    fontFamily = OrbitronFontFamily,
                    fontSize = 15.sp,
                    color = TextBlueWhite,
                    lineHeight = 22.sp,
                    style = TextStyle(shadow = TextGlowShadow)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // GOAL Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(6.dp))
                        .background(PanelBackgroundSolid)
                        .border(1.dp, MainGlow.copy(alpha = 0.5f), CutCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "GOAL",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        color = BrightAccent
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = goalText.takeIf { !it.isNullOrBlank() } ?: taskTitle,
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isTicked) StatusSuccess else TextBlueWhite
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // WARNING Section
                Text(
                    text = "WARNING: $warningMessage",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = StatusError,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 44dp Bottom-Center Checkbox
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CutCornerShape(6.dp))
                            .background(if (isTicked) StatusSuccess.copy(alpha = 0.25f) else Color.Transparent)
                            .border(2.dp, if (isTicked) StatusSuccess else BrightAccent, CutCornerShape(6.dp))
                            .clickable {
                                isTicked = true
                                isVisible = false
                                onComplete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTicked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete",
                                tint = StatusSuccess,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Side-by-side action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlowButton(
                            text = "SNOOZE 5 MIN",
                            onClick = onSnooze,
                            modifier = Modifier.weight(1f)
                        )

                        GlowButton(
                            text = "DISMISS",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            borderColor = TextSecondaryBlue,
                            textColor = TextSecondaryBlue
                        )
                    }
                }
            }
        }
    }
}