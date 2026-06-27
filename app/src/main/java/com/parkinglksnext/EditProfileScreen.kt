package com.parkinglksnext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.viewmodel.ProfileEvent
import com.parkinglksnext.viewmodel.ProfileViewModel

data class VehiculoUI(
    val id: String,
    val matricula: String,
    val tipo: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit = {},
    onPhotoClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.userProfile

    // ── Personal info ──────────────────────────────────────────
    var nombre by remember(profile?.id) { mutableStateOf(profile?.firstName ?: "") }
    var apellidos by remember(profile?.id) { mutableStateOf(profile?.lastName ?: "") }

    // ── Password fields (separate section) ─────────────────────
    var oldPassword by remember { mutableStateOf("") }
    var oldPwdVisible by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var newPwdVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPwdVisible by remember { mutableStateOf(false) }

    // ── Notifications ──────────────────────────────────────────
    var startReminder by remember(profile?.id) {
        mutableStateOf(profile?.notificationSettings?.startReminder ?: true)
    }
    var expiringReminder by remember(profile?.id) {
        mutableStateOf(profile?.notificationSettings?.expiringReminder ?: true)
    }

    // ── Vehicles ───────────────────────────────────────────────
    val listaVehiculos = remember(profile?.id) {
        mutableStateListOf<VehiculoUI>().also { list ->
            profile?.vehicles?.forEach { v ->
                list.add(VehiculoUI(v.id, v.licensePlate, v.type.replaceFirstChar { it.uppercase() }))
            }
            if (list.isEmpty()) {
                list.add(VehiculoUI("vehicle-1", "1234ABC", "Combustión"))
            }
        }
    }

    var mostrarFormularioAnadir by remember { mutableStateOf(false) }
    var nuevaMatricula by remember { mutableStateOf("") }
    var nuevoTipoVehiculo by remember { mutableStateOf("Combustión") }

    // ── Collect one-shot save events ───────────────────────────
    LaunchedEffect(Unit) {
        viewModel.saveEvents.collect { event ->
            when (event) {
                ProfileEvent.SaveSuccess -> onNavigateBack()
            }
        }
    }

    val initials = remember(profile) {
        val first = profile?.firstName?.firstOrNull() ?: 'U'
        val last = profile?.lastName?.firstOrNull() ?: ' '
        "$first$last".trim().uppercase()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ─── PROFILE PHOTO ────────────────────────────────────
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { onPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Camera icon overlay
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Cambiar foto",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                "Cambiar foto",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // ─── SECCIÓN 1: INFORMACIÓN PERSONAL ──────────────────
            Text(
                text = "Información Personal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2537),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            )

            OutlinedTextField(
                value = apellidos,
                onValueChange = { apellidos = it },
                label = { Text("Apellidos") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            )

            // ─── SECCIÓN 2: CAMBIAR CONTRASEÑA ────────────────────
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cambiar Contraseña",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2537),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Old password
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("Contraseña actual") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (oldPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { oldPwdVisible = !oldPwdVisible }) {
                        Icon(
                            imageVector = if (oldPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            )

            // New password
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Nueva contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (newPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { newPwdVisible = !newPwdVisible }) {
                        Icon(
                            imageVector = if (newPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            )

            // Confirm new password
            val pwdMismatch = newPassword.isNotEmpty() && newPassword != confirmPassword
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar nueva contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (confirmPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPwdVisible = !confirmPwdVisible }) {
                        Icon(
                            imageVector = if (confirmPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                isError = pwdMismatch,
                supportingText = if (pwdMismatch) {{ Text("Las contraseñas no coinciden") }} else null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            )

            // ─── SECCIÓN 3: MIS VEHÍCULOS ─────────────────────────
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mis Vehículos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F2537)
                )
                IconButton(
                    onClick = { mostrarFormularioAnadir = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Vehículo", modifier = Modifier.size(20.dp))
                }
            }

            if (mostrarFormularioAnadir) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF3FC)),
                    border = BorderStroke(1.dp, Color(0xFFBAD3F7))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Añadir Vehículo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F2537), modifier = Modifier.padding(bottom = 12.dp))
                        OutlinedTextField(
                            value = nuevaMatricula,
                            onValueChange = { nuevaMatricula = it.uppercase() },
                            placeholder = { Text("Matrícula (ej: 1234ABC)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val opciones = listOf("Combustión", "Eléctrico", "Moto")
                            opciones.forEach { tipo ->
                                val isSelected = nuevoTipoVehiculo == tipo
                                OutlinedButton(
                                    onClick = { nuevoTipoVehiculo = tipo },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        contentColor = if (isSelected) Color.White else Color.Gray
                                    )
                                ) { Text(tipo, fontSize = 12.sp) }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { mostrarFormularioAnadir = false; nuevaMatricula = "" },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp)
                            ) { Text("Cancelar", color = Color.Gray) }
                            Button(
                                onClick = {
                                    if (nuevaMatricula.isNotBlank()) {
                                        listaVehiculos.add(VehiculoUI(id = System.currentTimeMillis().toString(), matricula = nuevaMatricula, tipo = nuevoTipoVehiculo))
                                        nuevaMatricula = ""; mostrarFormularioAnadir = false
                                    }
                                },
                                enabled = nuevaMatricula.isNotBlank(),
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("Añadir") }
                        }
                    }
                }
            }

            listaVehiculos.forEach { vehicle ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color(0xFF6C757D))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(vehicle.matricula, fontWeight = FontWeight.Bold, color = Color(0xFF0F2537))
                                Text(
                                    text = vehicle.tipo, fontSize = 12.sp,
                                    color = when (vehicle.tipo) {
                                        "Eléctrico" -> Color(0xFF137333)
                                        "Moto" -> Color(0xFF1A73E8)
                                        else -> Color.Gray
                                    }
                                )
                            }
                        }
                        IconButton(
                            onClick = { if (listaVehiculos.size > 1) listaVehiculos.remove(vehicle) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete, contentDescription = "Eliminar",
                                tint = if (listaVehiculos.size > 1) Color(0xFFC5221F) else Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── SECCIÓN 4: NOTIFICACIONES ────────────────────────
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notificaciones", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2537))
            }

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recordatorio de inicio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("30 minutos antes", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = startReminder, onCheckedChange = { startReminder = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alerta de expiración", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("15 minutos antes", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = expiringReminder, onCheckedChange = { expiringReminder = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
            }

            // Error display
            if (uiState.error != null) {
                Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            // ─── ACCIONES ──────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onNavigateBack() },
                    modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp)
                ) { Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = {
                        // Validate passwords match if changing
                        if (newPassword.isNotEmpty() && newPassword != confirmPassword) return@Button

                        val vehicles = listaVehiculos.map { v ->
                            Vehicle(
                                id = v.id, licensePlate = v.matricula,
                                type = when (v.tipo) {
                                    "Eléctrico" -> "electric"; "Moto" -> "motorcycle"; else -> "combustion"
                                }
                            )
                        }
                        viewModel.updateProfile(
                            firstName = nombre, lastName = apellidos,
                            vehicles = vehicles,
                            notificationSettings = NotificationSettings(startReminder = startReminder, expiringReminder = expiringReminder),
                            oldPassword = oldPassword, newPassword = newPassword
                        )
                    },
                    modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
