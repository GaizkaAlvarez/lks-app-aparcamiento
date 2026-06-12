package com.parkinglksnext

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule // 1. ICONO DE RELOJ OFICIAL IMPORTADO
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReservationScreen(
    idReserva: String = "res-demo", // Recibe el ID de la reserva a editar
    onNavigateBack: () -> Unit = {}
) {
    // 2. Lógica para usar el parámetro idReserva y solucionar el "never used"
    LaunchedEffect(idReserva) {
        Log.d("EditReservation", "Cargando datos para la reserva: $idReserva")
    }

    // Rango de horas idéntico de 6:00 a 22:00
    val rangoHoras = remember { (6..22).map { stringOfHora(it) } }

    // Estados iniciales cargados (Simulando los valores previos de la reserva)
    var horaInicio by remember { mutableStateOf("08:00") }
    var horaFin by remember { mutableStateOf("14:00") }

    var expandidoInicio by remember { mutableStateOf(false) }
    var expandidoFin by remember { mutableStateOf(false) }

    // Cálculo dinámico de la duración (Lógica de tu archivo .tsx)
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

            // --- TARJETA DE INFO CON DEGRADADO (Figma Clone) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B00), Color(0xFFFF8C00)) // Tus colores exactos de Figma
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Plaza 42", // Hardcoded para el mock visual
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "15 de Junio, 2026", // Fecha simulada de ejemplo
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- SECCIÓN DE HORARIOS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        // REEMPLAZADO EL ICONO AQUÍ:
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF6B00))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modificar Horario", fontWeight = FontWeight.Bold, color = Color(0xFF0F2537), fontSize = 16.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Desplegable: Hora de Inicio
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

                        // Desplegable: Hora de Fin
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

                    // Mensaje dinámico de Estado/Error
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

            Spacer(modifier = Modifier.weight(1f))

            // --- ACCIONES (CANCELAR / GUARDAR) ---
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
                    onClick = { /* Pendiente actualizar en Firestore collection */ },
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