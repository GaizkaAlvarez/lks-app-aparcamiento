package com.parkinglksnext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.parkinglksnext.ui.theme.ParkingLKSNextTheme

// ELIMINAMOS los imports innecesarios de R y de LoginScreen
// ya que están en el mismo paquete/carpeta

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParkingLKSNextTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Ahora detectará LoginScreen() automáticamente
                    EditProfileScreen()
                }
            }
        }
    }
}