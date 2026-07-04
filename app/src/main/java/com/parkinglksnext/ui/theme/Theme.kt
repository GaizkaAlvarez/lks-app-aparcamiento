package com.parkinglksnext.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary             = ParklyOrange,
    onPrimary           = Color.White,
    primaryContainer    = ParklyOrangeLight,
    onPrimaryContainer  = ParklyOrangeDark,
    secondary           = ParklyOrange,
    onSecondary         = Color.White,
    background          = ParklyBackground,
    onBackground        = ParklyTextPrimary,
    surface             = ParklySurface,
    onSurface           = ParklyTextPrimary,
    surfaceVariant      = ParklyGrayLight,
    onSurfaceVariant    = ParklyTextSecondary,
    error               = ParklyRed,
    onError             = Color.White
)

@Composable
fun ParkingLKSNextTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}