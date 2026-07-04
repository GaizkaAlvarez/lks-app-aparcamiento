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
import com.parkinglksnext.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.viewmodel.ReservationsViewModel
import java.time.LocalDate
import java.time.LocalTime

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

    // Find the reservation by ID (search in both current and future)
    val reservation = remember(uiState.currentReservations, uiState.futureReservations, idReserva) {
        uiState.currentReservations.find { it.id == idReserva }
            ?: uiState.futureReservations.find { it.id == idReserva }
    }

    // Load spot reservations when the reservation is found
    LaunchedEffect(reservation) {
        reservation?.let {
            viewModel.loadSpotReservations(it.spotId, it.date)
        }
    }

    // Collect one-shot update events (no infinite loop)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.parkinglksnext.viewmodel.ReservationEvent.UpdateSuccess -> onNavigateBack()
                else -> {}
            }
        }
    }

    // Determine if editing today's reservation
    val esHoy = try {
        LocalDate.parse(reservation?.date) == LocalDate.now()
    } catch (_: Exception) { false }

    val ahora = remember { LocalTime.now() }
    val ahoraMinutos = ahora.hour * 60 + ahora.minute

    // Minimum valid start hour for today
    val horaMinimaInicio = if (esHoy) {
        val horaActualRedondeada = if (ahora.minute > 0) ahora.hour + 1 else ahora.hour
        maxOf(horaActualRedondeada, 6)
    } else {
        6
    }

    val rangoHorasInicio = remember(horaMinimaInicio) {
        (horaMinimaInicio..22).map { stringOfHora(it) }
    }
    val rangoHorasFin = remember { (6..22).map { stringOfEndHora(it) } }

    // Initialize from reservation data
    var horaInicio by remember(reservation) { mutableStateOf(reservation?.startTime ?: "08:00") }
    var horaFin by remember(reservation) { mutableStateOf(reservation?.endTime ?: "14:55") }

    // If today and saved start time is invalid, reset to first available
    LaunchedEffect(horaMinimaInicio, reservation) {
        val savedStart = reservation?.startTime ?: "08:00"
        val savedStartHour = savedStart.split(":").getOrNull(0)?.toIntOrNull() ?: 8
        if (esHoy && savedStartHour < horaMinimaInicio) {
            val nuevaHora = rangoHorasInicio.first()
            horaInicio = nuevaHora
        }
    }

    var expandidoInicio by remember { mutableStateOf(false) }
    var expandidoFin by remember { mutableStateOf(false) }

    val startMin = horaInicio.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
    val endMin = horaFin.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
    val duracion = endMin - startMin
    val duracionHoras = duracion / 60
    val duracionMinutos = duracion % 60
    val isValidDuration = duracion > 0 && duracion <= 540  // max 9h = 540 min
    val horaPasada = esHoy && rangoHorasInicio.isEmpty()
    val edicionValida = isValidDuration && !horaPasada

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
                            colors = listOf(ParklyOrange, Color(0xFFFF8C00))
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
                colors = CardDefaults.cardColors(containerColor = ParklySurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = ParklyOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modificar Horario", fontWeight = FontWeight.Bold, color = ParklyTextPrimary, fontSize = 16.sp)
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
                                    rangoHorasInicio.forEach { hora ->
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
                                    rangoHorasFin.forEach { hora ->
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

                    // Duration / validation message
                    Spacer(modifier = Modifier.height(16.dp))
                    val (msgColor, msgText) = when {
                        horaPasada -> Pair(Color(0xFFC5221F), "Ya no puedes modificar esta reserva. La franja horaria de hoy ha pasado.")
                        !isValidDuration && duracion <= 0 -> Pair(Color(0xFFC5221F), "La hora de fin debe ser posterior a la de inicio")
                        !isValidDuration -> Pair(Color(0xFFC5221F), "La duración máxima es de 9 horas")
                        else -> Pair(Color(0xFF137333), "Duración: ${duracionHoras}h ${duracionMinutos}min")
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (edicionValida) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msgText,
                            color = msgColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Reservas activas en esta plaza ───────────────────
            val otrasReservas = uiState.spotReservations.filter { it.id != idReserva }
            if (otrasReservas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Reservas en esta plaza para ese día:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF8D6E00)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        otrasReservas.forEach { r ->
                            Text(
                                "• ${r.startTime} – ${r.endTime}",
                                fontSize = 14.sp,
                                color = Color(0xFF8D6E00),
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                        if (edicionValida) {
                            viewModel.updateReservation(idReserva, horaInicio, horaFin)
                        }
                    },
                    enabled = edicionValida,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange)
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun stringOfHora(hora: Int): String = if (hora < 10) "0$hora:00" else "$hora:00"
private fun stringOfEndHora(hora: Int): String = if (hora < 10) "0$hora:55" else "$hora:55"
