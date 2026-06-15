package com.parkinglksnext

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.viewmodel.ReservationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReservationScreen(
    idReserva: String = "res-demo",
    viewModel: ReservationsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    LaunchedEffect(idReserva) {
        Log.d("EditReservation", "Cargando datos para la reserva: $idReserva")
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Find the reservation by ID
    val reservation = remember(uiState.activeReservations, idReserva) {
        uiState.activeReservations.find { it.id == idReserva }
    }

    // Navigate back on successful update
    LaunchedEffect(uiState.isUpdateSuccess) {
        if (uiState.isUpdateSuccess) {
            viewModel.clearUpdateSuccess()
            onNavigateBack()
        }
    }

    val rangoHoras = remember { (6..22).map { stringOfHora(it) } }

    // Initialize from reservation data
    var horaInicio by remember(reservation) { mutableStateOf(reservation?.startTime ?: "08:00") }
    var horaFin by remember(reservation) { mutableStateOf(reservation?.endTime ?: "14:00") }

    var expandidoInicio by remember { mutableStateOf(false) }
    var expandidoFin by remember { mutableStateOf(false) }

    val startInt = horaInicio.split(":")[0].toInt()
    val endInt = horaFin.split(":")[0].toInt()
    val duracion = endInt - startInt
    val isValidDuration = duracion in 1..9

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar Reserva", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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

            // Reservation info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B00), Color(0xFFFF8C00))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Plaza ${reservation?.spotNumber ?: "—"}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reservation?.date ?: "—",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Time editing card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF6B00))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modificar Horario", fontWeight = FontWeight.Bold, color = Color(0xFF0F2537), fontSize = 16.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hora de Inicio", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                            ExposedDropdownMenuBox(
                                expanded = expandidoInicio,
                                onExpandedChange = { expandidoInicio = !expandidoInicio }
                            ) {
                                OutlinedTextField(
                                    value = horaInicio,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoInicio) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(expanded = expandidoInicio, onDismissRequest = { expandidoInicio = false }) {
                                    rangoHoras.forEach { hora ->
                                        DropdownMenuItem(
                                            text = { Text(hora) },
                                            onClick = {
                                                horaInicio = hora
                                                expandidoInicio = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hora de Fin", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                            ExposedDropdownMenuBox(
                                expanded = expandidoFin,
                                onExpandedChange = { expandidoFin = !expandidoFin }
                            ) {
                                OutlinedTextField(
                                    value = horaFin,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoFin) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    isError = !isValidDuration
                                )
                                ExposedDropdownMenu(expanded = expandidoFin, onDismissRequest = { expandidoFin = false }) {
                                    rangoHoras.forEach { hora ->
                                        DropdownMenuItem(
                                            text = { Text(hora) },
                                            onClick = {
                                                horaFin = hora
                                                expandidoFin = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Duration message
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isValidDuration) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isValidDuration) {
                                "Duración: $duracion hora${if (duracion != 1) "s" else ""}"
                            } else if (duracion <= 0) {
                                "La hora de fin debe ser posterior a la de inicio"
                            } else {
                                "La duración máxima es de 9 horas"
                            },
                            color = if (isValidDuration) Color(0xFF137333) else Color(0xFFC5221F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error display
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateBack() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (isValidDuration) {
                            viewModel.updateReservation(idReserva, horaInicio, horaFin)
                        }
                    },
                    enabled = isValidDuration,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun stringOfHora(hora: Int): String = if (hora < 10) "0$hora:00" else "$hora:00"
