package com.maahiassistant.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ClayDayColorScheme = lightColorScheme(
    primary = ClayBlueAccent,
    onPrimary = ClaySurface,
    primaryContainer = ClayBlueContainer,
    onPrimaryContainer = ClayOnBlueContainer,
    secondary = ClayBlueDark,
    onSecondary = ClaySurface,
    secondaryContainer = ClaySurfaceVariant,
    onSecondaryContainer = ClayTextPrimary,
    tertiary = OrbGold,
    background = ClayBackground,
    onBackground = ClayTextPrimary,
    surface = ClaySurface,
    onSurface = ClayTextPrimary,
    surfaceVariant = ClaySurfaceVariant,
    onSurfaceVariant = ClayTextSecondary,
    outline = ClayBorder
)

@Composable
fun MaahiTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ClayDayColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
