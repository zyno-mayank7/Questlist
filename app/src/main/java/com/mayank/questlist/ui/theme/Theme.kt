package com.mayank.questlist.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val SystemCutShape = CutCornerShape(10.dp)

private val DarkColorScheme = darkColorScheme(
    primary = BrightAccent,
    onPrimary = VoidBackground,
    primaryContainer = FaintBlueFill,
    onPrimaryContainer = TextBlueWhite,
    secondary = MainGlow,
    onSecondary = TextBlueWhite,
    background = VoidBackground,
    onBackground = TextBlueWhite,
    surface = PanelBackground,
    onSurface = TextBlueWhite,
    surfaceVariant = PanelBackgroundSolid,
    onSurfaceVariant = TextSecondaryBlue,
    error = StatusError,
    onError = TextBlueWhite
)

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun QuestListTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = VoidBackground.toArgb()
                window.navigationBarColor = VoidBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SystemTypography,
        shapes = Shapes(
            small = CutCornerShape(6.dp),
            medium = CutCornerShape(10.dp),
            large = CutCornerShape(14.dp)
        ),
        content = content
    )
}