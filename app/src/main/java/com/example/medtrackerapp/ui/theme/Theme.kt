package com.example.medtrackerapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Colores médicos
private val medBlue = Color(0xFF4285F4)
private val medDarkBlue = Color(0xFF3367D6)
private val medLightBlue = Color(0xFF8AB4F8)
private val medGreen = Color(0xFF4CAF50)
private val medAmber = Color(0xFFFFA000)
private val medRed = Color(0xFFEF5350)

// Light Theme
private val LightColors = lightColorScheme(
    primary = medBlue,
    onPrimary = Color.White,
    primaryContainer = medLightBlue.copy(alpha = 0.3f),
    onPrimaryContainer = medDarkBlue,
    secondary = medDarkBlue,
    onSecondary = Color.White,
    tertiary = medGreen,
    background = Color.White,
    surface = Color(0xFFF8F9FA),
    error = medRed,
    onError = Color.White
)

// Dark Theme
private val DarkColors = darkColorScheme(
    primary = medLightBlue,
    onPrimary = Color.Black,
    primaryContainer = medBlue.copy(alpha = 0.3f),
    onPrimaryContainer = medLightBlue,
    secondary = medLightBlue,
    onSecondary = Color.Black,
    tertiary = medGreen,
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF202124),
    error = medRed,
    onError = Color.Black
)

@Composable
fun MedTrackerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Colores para estados de parámetros médicos
 */
object MedicalColors {
    val normal = medGreen
    val watch = medAmber
    val attention = medRed
    val undefined = Color.Gray
}