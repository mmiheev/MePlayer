package com.zeon.meplayer.presentation.theme

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Composable effect that sets the colors of the system bars.
 * Call this effect at the root of your theme.
 *
 * @param statusBarColor Color of the status bar (in Compose Color format).
 * @param navigationBarColor Color of the navigation bar (in Compose Color format).
 * @param isLightIcons Whether to use light icons on the bars. If null, automatically
 *                     determined by the luminance of the status bar color.
 */
@Composable
fun SystemBarsEffect(
    statusBarColor: androidx.compose.ui.graphics.Color,
    navigationBarColor: androidx.compose.ui.graphics.Color,
    isLightIcons: Boolean? = null
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val statusBarColorInt = remember(statusBarColor) { statusBarColor.toArgb() }
    val navigationBarColorInt = remember(navigationBarColor) { navigationBarColor.toArgb() }

    val lightIcons = isLightIcons ?: run {
        val luminance = calculateLuminance(statusBarColorInt)
        luminance < 0.5
    }

    SideEffect {
        val window = (view.context as Activity).window
        setSystemBarColors(
            window = window,
            statusBarColor = statusBarColorInt,
            navigationBarColor = navigationBarColorInt,
            lightIcons = lightIcons
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}

/**
 * Sets the system bar colors and icon appearance, taking the Android version into account.
 * On API 30+ it also configures the light/dark mode of the status and navigation icons.
 */
@Suppress("DEPRECATION")
private fun setSystemBarColors(
    window: Window,
    statusBarColor: Int,
    navigationBarColor: Int,
    lightIcons: Boolean
) {

    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = window.insetsController
        controller?.let {
            it.setSystemBarsAppearance(
                if (!lightIcons) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
            it.setSystemBarsAppearance(
                if (!lightIcons) 0 else WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        }
    } else {
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !lightIcons
        insetsController.isAppearanceLightNavigationBars = !lightIcons
    }

    window.statusBarColor = statusBarColor
    window.navigationBarColor = navigationBarColor
}

/**
 * Calculates the luminance of a color using the standard formula
 * (0.299*R + 0.587*G + 0.114*B) / 255.
 *
 * @return A value from 0.0 (black) to 1.0 (white).
 */
private fun calculateLuminance(@ColorInt color: Int): Double {
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
}