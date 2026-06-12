package com.parkinglksnext

import androidx.compose.foundation.BorderStroke // 1. IMPORTACIÓN CORREGIDA
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.Delete // 2. ICONO DE PAPELERA CORREGIDO
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Estructura de datos idéntica a tu tipo Vehicle de TypeScript
data class VehiculoUI(
    val id: String,
    val matricula: String,
    val tipo: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit = {}
) {
    var nombre by remember { mutableStateOf("Gaizka") }
    var apellidos by remember { mutableStateOf("Álvarez") }

    // Notificaciones
    var startReminder by remember { mutableStateOf(true) }
    var expiringReminder by remember { mutableStateOf(true) }

    // --- GESTIÓN DE VEHÍCULOS (Mapeado de EditProfile.tsx) ---
    val listaVehiculos = remember {
        mutableStateListOf(
            VehiculoUI("1", "1234ABC", "Eléctrico"),
            VehiculoUI("2", "5678XYZ", "Normal")
        )
    }

    var mostrarFormularioAnadir by remember { mutableStateOf(false) }
    var nuevaMatricula by remember { mutableStateOf("") }
    var nuevoTipoVehiculo by remember { mutableStateOf("Normal") }

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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- SECCIÓN 1: INFORMACIÓN PERSONAL ---
            Text(
                text = "Información Personal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2537),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = apellidos,
                onValueChange = { apellidos = it },
                label = { Text("Apellidos") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // --- SECCIÓN 2: MIS VEHÍCULOS ---
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
                // Botón más (+) para desplegar el formulario
                IconButton(
                    onClick = { mostrarFormularioAnadir = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Vehículo", modifier = Modifier.size(20.dp))
                }
            }

            // Formulario dinámico azul para Añadir Vehículo
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

                        // Selector triple de tipo de vehículo
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val opciones = listOf("Normal", "Eléctrico", "Moto")
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
                                ) {
                                    Text(tipo, fontSize = 12.sp)
                                }
                            }
                        }

                        // Acciones del formulario interno
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    mostrarFormularioAnadir = false
                                    nuevaMatricula = ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Cancelar", color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    if (nuevaMatricula.isNotBlank()) {
                                        listaVehiculos.add(VehiculoUI(id = System.currentTimeMillis().toString(), matricula = nuevaMatricula, tipo = nuevoTipoVehiculo))
                                        nuevaMatricula = ""
                                        mostrarFormularioAnadir = false
                                    }
                                },
                                enabled = nuevaMatricula.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Añadir")
                            }
                        }
                    }
                }
            }

            // Listado reactivo de vehículos con botón de eliminar
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
                            Icon(Icons.Default.CarRepair, contentDescription = null, tint = Color(0xFF6C757D))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                // 3. ERROR DE PARÁMETRO 'grandfather' CORREGIDO AQUÍ:
                                Text(vehicle.matricula, fontWeight = FontWeight.Bold, color = Color(0xFF0F2537))
                                Text(
                                    text = vehicle.tipo,
                                    fontSize = 12.sp,
                                    color = if (vehicle.tipo == "Eléctrico") Color(0xFF137333) else if (vehicle.tipo == "Moto") Color(0xFF1A73E8) else Color.Gray
                                )
                            }
                        }

                        // Botón de papelera para borrar
                        IconButton(
                            onClick = {
                                if (listaVehiculos.size > 1) {
                                    listaVehiculos.remove(vehicle)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete, // CORREGIDO A 'Icons.Default.Delete'
                                contentDescription = "Eliminar",
                                tint = if (listaVehiculos.size > 1) Color(0xFFC5221F) else Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SECCIÓN 3: CONFIGURACIÓN DE NOTIFICACIONES ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configuración de Notificaciones",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F2537)
                )
            }

            // Interruptor 1
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recordatorio de inicio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("30 minutos antes de la reserva", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = startReminder,
                        onCheckedChange = { startReminder = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Interruptor 2
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alerta de expiración", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("15 minutos antes de que expire", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = expiringReminder,
                        onCheckedChange = { expiringReminder = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // --- ACCIONES ACCESIBLES ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateBack() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { /* Próximamente guardaremos la lista en Firebase */ },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}