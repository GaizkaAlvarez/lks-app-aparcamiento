package com.parkinglksnext.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// In Theme.kt
private val DarkColorScheme = darkColorScheme(
    primary = LksOrange,      // Use your defined colors here
    secondary = LksOrange,
    tertiary = LksOrange
)

private val LightColorScheme = lightColorScheme(
    primary = LksOrange,
    onPrimary = LksWhite,
    secondary = LksOrange,
    onSecondary = LksWhite,
    background = LksBackground,
    onBackground = LksTextPrimary,
    surface = LksSurface,
    onSurface = LksTextPrimary,
    error = LksError,
    onError = LksWhite
)

@Composable
fun ParkingLKSNextTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // CAMBIA ESTO A FALSE para que no use los colores del móvil
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Al poner dynamicColor a false arriba, esto ya no se ejecutará
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme // Aquí podrías poner también LightColorScheme si quieres que siempre sea blanca
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}