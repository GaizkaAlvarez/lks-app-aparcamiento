package com.parkinglksnext

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera idéntica a la de Registro
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Volver al inicio",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateBack() }
            )
        }

        // Logo centrado
        Image(
            painter = painterResource(id = R.drawable.logo_lks),
            contentDescription = "Logo LKS",
            modifier = Modifier.width(140.dp).padding(bottom = 40.dp)
        )

        // Título y descripción de tu Figma
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "¿Olvidaste tu contraseña?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(
                text = "Te enviaremos un enlace para restablecer tu contraseña",
                fontSize = 15.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Input de Email corporativo
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("tu@email.com") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Botón Enviar Enlace (Naranja LKS)
        Button(
            onClick = { /* Lógica de Firebase en el futuro */ },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Enviar Enlace", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}