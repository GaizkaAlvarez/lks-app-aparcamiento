package com.parkinglksnext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.ui.theme.*
import com.parkinglksnext.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit = {}
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nombreCoche by remember { mutableStateOf("") }
    var matricula by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var tipoVehiculo by remember { mutableStateOf("Común") }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate back on successful registration (user is now authenticated, auth flow handles rest)
    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) {
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParklyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABECERA: Volver al inicio ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
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

        // --- LOGO ---
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ParklyOrange),
            contentAlignment = Alignment.Center
        ) {
            Text("P", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- TÍTULO Y SUBTÍTULO ---
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Crear Cuenta", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ParklyTextPrimary)
            Text(text = "Únete a LKS Next Parking", fontSize = 14.sp, color = ParklyTextSecondary, modifier = Modifier.padding(top = 4.dp))
        }

        // --- CAMPOS DE TEXTO ---
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre Completo") },
            placeholder = { Text("Juan Pérez") },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("tu@email.com") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        )

        OutlinedTextField(
            value = nombreCoche,
            onValueChange = { nombreCoche = it },
            label = { Text("Nombre del Vehículo") },
            placeholder = { Text("Ej: Mi coche, El Tesla...") },
            leadingIcon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        )

        OutlinedTextField(
            value = matricula,
            onValueChange = { matricula = it.uppercase() },
            label = { Text("Matrícula del Vehículo *") },
            placeholder = { Text("1234ABC") },
            leadingIcon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        )

        // --- SELECTOR DE TIPO DE VEHÍCULO ---
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Tipo de Vehículo", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val opciones = listOf("Común", "Eléctrico", "Moto")
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
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        )

        // Error message
        if (uiState.error != null) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // --- BOTÓN PRINCIPAL ---
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    val vehicleType = when (tipoVehiculo) {
                        "Eléctrico" -> "electric"
                        "Moto" -> "motorcycle"
                        else -> "comun"
                    }
                    val profile = UserProfile(
                        firstName = nombre.split(" ").firstOrNull() ?: nombre,
                        lastName = nombre.split(" ").drop(1).joinToString(" "),
                        name = nombre,
                        email = email,
                        licensePlate = matricula,
                        vehicleType = vehicleType,
                        vehicles = listOf(
                            Vehicle(
                                id = "vehicle-1",
                                name = nombreCoche.ifBlank { matricula },
                                licensePlate = matricula,
                                type = vehicleType
                            )
                        ),
                        notificationSettings = NotificationSettings()
                    )
                    viewModel.register(email, password, profile)
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Crear Cuenta", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- FOOTER ---
        Row(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(text = "¿Ya tienes cuenta? ", color = ParklyTextSecondary, fontSize = 14.sp)
            Text(
                text = "Inicia sesión",
                color = ParklyOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onNavigateBack() }
            )
        }
    }
}
