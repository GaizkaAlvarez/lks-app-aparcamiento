package com.parkinglksnext

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import com.parkinglksnext.ui.theme.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    val nombre: String,
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
    var startReminderMinutes by remember(profile?.id) {
        mutableIntStateOf(profile?.notificationSettings?.startReminderMinutes ?: 15)
    }
    var expiringReminder by remember(profile?.id) {
        mutableStateOf(profile?.notificationSettings?.expiringReminder ?: true)
    }
    var expiringReminderMinutes by remember(profile?.id) {
        mutableIntStateOf(profile?.notificationSettings?.expiringReminderMinutes ?: 15)
    }

    // ── Vehicles ───────────────────────────────────────────────
    val listaVehiculos = remember(profile?.id) {
        mutableStateListOf<VehiculoUI>().also { list ->
            profile?.vehicles?.forEach { v ->
                val tipoDisplay = when (v.type) {
                    "electric" -> "Eléctrico"
                    "motorcycle" -> "Moto"
                    else -> "Común"
                }
                list.add(VehiculoUI(v.id, v.name, v.licensePlate, tipoDisplay))
            }
        }
    }

    var mostrarFormularioAnadir by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }
    var nuevaMatricula by remember { mutableStateOf("") }
    var nuevoTipoVehiculo by remember { mutableStateOf("Común") }

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
            val photoBase64 = profile?.profileImageBase64?.takeIf { it.isNotEmpty() }
            val photoBitmap = remember(photoBase64) {
                photoBase64?.let {
                    try {
                        val bytes = Base64.decode(it, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) { null }
                }
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(ParklyOrangeLight)
                        .clickable { onPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    "Cambiar foto",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onPhotoClick() }
                )
                if (photoBitmap != null) {
                    Text(
                        "Quitar foto",
                        fontSize = 13.sp,
                        color = Color(0xFFC5221F),
                        modifier = Modifier.clickable {
                            viewModel.saveProfilePhotoBase64("")
                        }
                    )
                }
            }

            // ─── SECCIÓN 1: INFORMACIÓN PERSONAL ──────────────────
            Text(
                text = "Información Personal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ParklyTextPrimary,
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

            // ─── SECCIÓN 2: MIS VEHÍCULOS ─────────────────────────
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
                    color = ParklyTextPrimary
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
                        Text("Añadir Vehículo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ParklyTextPrimary, modifier = Modifier.padding(bottom = 12.dp))
                        OutlinedTextField(
                            value = nuevoNombre,
                            onValueChange = { nuevoNombre = it },
                            placeholder = { Text("Nombre (ej: Mi coche)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )
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
                            val opciones = listOf("Común", "Eléctrico", "Moto")
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
                                onClick = { mostrarFormularioAnadir = false; nuevaMatricula = ""; nuevoNombre = "" },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp)
                            ) { Text("Cancelar", color = Color.Gray) }
                            Button(
                                onClick = {
                                    if (nuevaMatricula.isNotBlank()) {
                                        listaVehiculos.add(VehiculoUI(
                                            id = System.currentTimeMillis().toString(),
                                            nombre = nuevoNombre.ifBlank { nuevaMatricula },
                                            matricula = nuevaMatricula,
                                            tipo = nuevoTipoVehiculo
                                        ))
                                        nuevaMatricula = ""; nuevoNombre = ""; mostrarFormularioAnadir = false
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

            listaVehiculos.forEachIndexed { index, vehicle ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ParklyBackground),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drag handle (reorder arrows)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (index > 0) {
                                IconButton(
                                    onClick = {
                                        val item = listaVehiculos.removeAt(index)
                                        listaVehiculos.add(index - 1, item)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("▲", fontSize = 14.sp, color = ParklyTextSecondary)
                                }
                            } else {
                                Spacer(modifier = Modifier.size(28.dp))
                            }
                            if (index < listaVehiculos.size - 1) {
                                IconButton(
                                    onClick = {
                                        val item = listaVehiculos.removeAt(index)
                                        listaVehiculos.add(index + 1, item)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("▼", fontSize = 14.sp, color = ParklyTextSecondary)
                                }
                            } else {
                                Spacer(modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Vehicle type emoji
                        Text(
                            text = when (vehicle.tipo) {
                                "Eléctrico" -> "⚡"
                                "Moto" -> "🏍"
                                else -> "🚗"
                            },
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (vehicle.nombre.isNotBlank()) vehicle.nombre else vehicle.matricula,
                                fontWeight = FontWeight.Bold, color = ParklyTextPrimary
                            )
                            Text(vehicle.matricula, fontSize = 12.sp, color = ParklyTextSecondary)
                            Text(
                                text = vehicle.tipo, fontSize = 12.sp,
                                color = when (vehicle.tipo) {
                                    "Eléctrico" -> Color(0xFF137333)
                                    "Moto" -> Color(0xFF1A73E8)
                                    else -> Color.Gray
                                }
                            )
                            if (index == 0) {
                                Text("Principal", fontSize = 10.sp, color = ParklyOrange, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Delete button
                        IconButton(
                            onClick = {
                                if (listaVehiculos.size > 1) {
                                    viewModel.cancelReservationsForVehicle(vehicle.id)
                                    listaVehiculos.remove(vehicle)
                                }
                            }
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

            // ─── SECCIÓN 3: NOTIFICACIONES ────────────────────────
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notificaciones", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ParklyTextPrimary)
            }

            // Start reminder
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ParklyBackground)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recordatorio de inicio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${startReminderMinutes} minutos antes", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(checked = startReminder, onCheckedChange = { startReminder = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                    }
                    if (startReminder) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10).forEach { mins ->
                                FilterChip(
                                    selected = startReminderMinutes == mins,
                                    onClick = { startReminderMinutes = mins },
                                    label = { Text("${mins} min", fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ParklyOrangeLight,
                                        selectedLabelColor = ParklyOrange
                                    )
                                )
                            }
                        }
                    }
                }
            }
            // Expiry reminder
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ParklyBackground)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alerta de expiración", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${expiringReminderMinutes} minutos antes", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(checked = expiringReminder, onCheckedChange = { expiringReminder = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                    }
                    if (expiringReminder) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10).forEach { mins ->
                                FilterChip(
                                    selected = expiringReminderMinutes == mins,
                                    onClick = { expiringReminderMinutes = mins },
                                    label = { Text("${mins} min", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ParklyOrangeLight,
                                        selectedLabelColor = ParklyOrange
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Error display
            if (uiState.error != null) {
                Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            }

            // ─── SECCIÓN: CAMBIAR CONTRASEÑA ────────────────────
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cambiar Contraseña",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ParklyTextPrimary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("Contraseña actual") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (oldPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { oldPwdVisible = !oldPwdVisible }) {
                        Icon(if (oldPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Nueva contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (newPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { newPwdVisible = !newPwdVisible }) {
                        Icon(if (newPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            )

            val pwdMismatch = newPassword.isNotEmpty() && newPassword != confirmPassword
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar nueva contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (confirmPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPwdVisible = !confirmPwdVisible }) {
                        Icon(if (confirmPwdVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                    }
                },
                isError = pwdMismatch,
                supportingText = if (pwdMismatch) {{ Text("Las contraseñas no coinciden") }} else null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            )

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
                                id = v.id,
                                name = v.nombre,
                                licensePlate = v.matricula,
                                type = when (v.tipo) {
                                    "Eléctrico" -> "electric"; "Moto" -> "motorcycle"; else -> "comun"
                                }
                            )
                        }
                        viewModel.updateProfile(
                            firstName = nombre, lastName = apellidos,
                            vehicles = vehicles,
                            notificationSettings = NotificationSettings(
                                startReminder = startReminder,
                                startReminderMinutes = startReminderMinutes,
                                expiringReminder = expiringReminder,
                                expiringReminderMinutes = expiringReminderMinutes
                            ),
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
