package com.parkinglksnext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReservationScreen() {
    // 1. Lógica de Fechas Dinámicas (Corregido el Locale con forLanguageTag)
    val formatoVisual = remember { DateTimeFormatter.ofPattern("EEE d 'de' MMM", Locale.forLanguageTag("es-ES")) }
    val listaFechas = remember {
        (0..6).map { LocalDate.now().plusDays(it.toLong()) }
    }
    var fechaSeleccionada by remember { mutableStateOf(listaFechas.first()) }

    // 2. Lógica de Horas Desplegables (6:00 a 22:00)
    val rangoHoras = remember { (6..22).map { stringOf(it) } }
    var horaInicio by remember { mutableStateOf("08:00") }
    var horaFin by remember { mutableStateOf("14:00") }

    var expandidoInicio by remember { mutableStateOf(false) }
    var expandidoFin by remember { mutableStateOf(false) }

    // 3. Lógica de Selector de Vehículos
    val misVehiculos = listOf("1234ABC (Normal)", "5678XYZ (Eléctrico)", "9101KFC (Moto)")
    var vehiculoSeleccionado by remember { mutableStateOf(misVehiculos.first()) }
    var expandidoVehiculo by remember { mutableStateOf(false) }

    // Validación de negocio
    val horaInicioInt = horaInicio.split(":")[0].toInt()
    val horaFinInt = horaFin.split(":")[0].toInt()
    val horasValidas = horaInicioInt < horaFinInt

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nueva Reserva", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* Abrir menú lateral */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
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

            // --- VEHÍCULOS (menuAnchor Actualizado) ---
            Text("Vehículo para la reserva", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            ExposedDropdownMenuBox(
                expanded = expandidoVehiculo,
                onExpandedChange = { expandidoVehiculo = !expandidoVehiculo },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                OutlinedTextField(
                    value = vehiculoSeleccionado,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoVehiculo) },
                    // Corregido: añadimos MenuAnchorType.PrimaryNotEditable
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandidoVehiculo,
                    onDismissRequest = { expandidoVehiculo = false }
                ) {
                    misVehiculos.forEach { vehiculo ->
                        DropdownMenuItem(
                            text = { Text(vehiculo) },
                            onClick = {
                                vehiculoSeleccionado = vehiculo
                                expandidoVehiculo = false
                            }
                        )
                    }
                }
            }

            // --- FECHA (DINÁMICA) ---
            Text("Fecha", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            for (i in listaFechas.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fecha1 = listaFechas[i]
                    BotonDia(
                        texto = fecha1.format(formatoVisual).replaceFirstChar { it.uppercase() },
                        isSelected = fechaSeleccionada == fecha1,
                        onClick = { fechaSeleccionada = fecha1 },
                        modifier = Modifier.weight(1f)
                    )

                    if (i + 1 < listaFechas.size) {
                        val fecha2 = listaFechas[i + 1]
                        BotonDia(
                            texto = fecha2.format(formatoVisual).replaceFirstChar { it.uppercase() },
                            isSelected = fechaSeleccionada == fecha2,
                            onClick = { fechaSeleccionada = fecha2 },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DESPLEGABLES DE HORAS (menuAnchor Actualizados) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hora Inicio
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hora de Inicio", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
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

                // Hora Fin
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hora de Fin", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
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
                            isError = !horasValidas
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

            if (!horasValidas) {
                Text(
                    text = "La hora de fin debe ser posterior a la de inicio.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CAJA DE RESUMEN INFO ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F4FA), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row {
                        Text("Resumen: ", fontWeight = FontWeight.Bold, color = Color(0xFF0F2537))
                        Text(vehiculoSeleccionado.split(" ")[0], color = Color(0xFF1E3A8A), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Reserva programada para el ${fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", fontSize = 13.sp, color = Color(0xFF3B82F6))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- BOTÓN CONTINUAR ---
            Button(
                onClick = { /* Continuar flujo */ },
                enabled = horasValidas,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB07A)
                )
            ) {
                Text("Continuar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

fun stringOf(hora: Int): String = if (hora < 10) "0$hora:00" else "$hora:00"

@Composable
fun BotonDia(texto: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(45.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF495057)
        )
    ) {
        Text(texto, fontSize = 11.sp, maxLines = 1)
    }
}