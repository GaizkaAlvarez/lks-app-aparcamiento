package com.parkinglksnext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit = {} // Lo dejamos vacío por ahora
) {
    // Variables solo para que la UI funcione y se actualice al escribir
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var matricula by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tipoVehiculo by remember { mutableStateOf("Normal") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Añadimos scroll para que quepa todo
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABECERA: Volver al inicio (Como en tu Figma) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Volver al inicio",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }

        // --- LOGO ---
        Image(
            painter = painterResource(id = R.drawable.logo_lks),
            contentDescription = "Logo LKS",
            modifier = Modifier.width(140.dp).padding(bottom = 32.dp)
        )

        // --- TÍTULO Y SUBTÍTULO ---
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Crear Cuenta", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = "Únete a LKS Next Parking", fontSize = 15.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        }

        // --- CAMPOS DE TEXTO CON ICONOS ---
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre Completo") },
            placeholder = { Text("Juan Pérez") },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }, // Icono persona
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("tu@email.com") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) }, // Icono email
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = matricula,
            onValueChange = { matricula = it },
            label = { Text("Matrícula del Vehículo *") },
            placeholder = { Text("1234ABC") },
            leadingIcon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = null) }, // Icono coche
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // --- SELECTOR DE TIPO DE VEHÍCULO ---
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Tipo de Vehículo", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre botones
            ) {
                val opciones = listOf("Normal", "Eléctrico", "Moto")
                opciones.forEach { tipo ->
                    val isSelected = tipoVehiculo == tipo
                    OutlinedButton(
                        onClick = { tipoVehiculo = tipo },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            // Un fondito muy suave si está seleccionado, para darle el toque Figma
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    ) {
                        Text(tipo, fontSize = 13.sp)
                    }
                }
            }
        }

        // --- CONTRASEÑA ---
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            placeholder = { Text("Mínimo 6 caracteres") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) }, // Icono candado
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // --- BOTÓN PRINCIPAL ---
        Button(
            onClick = { /* PENDIENTE: Conectar con Firebase luego */ },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Crear Cuenta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- FOOTER ---
        Row(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(text = "¿Ya tienes cuenta? ", color = Color.Gray)
            Text(
                text = "Inicia sesión",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}