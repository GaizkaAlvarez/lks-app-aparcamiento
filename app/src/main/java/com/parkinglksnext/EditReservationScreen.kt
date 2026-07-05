package com.parkinglksnext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    idReserva: String = "",
    viewModel: ReservationsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val reservation = remember(uiState.currentReservations, uiState.futureReservations, idReserva) {
        uiState.currentReservations.find { it.id == idReserva }
            ?: uiState.futureReservations.find { it.id == idReserva }
    }

    LaunchedEffect(reservation) {
        reservation?.let { viewModel.loadSpotReservations(it.spotId, it.date) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.parkinglksnext.viewmodel.ReservationEvent.UpdateSuccess -> onNavigateBack()
                else -> {}
            }
        }
    }

    val esHoy = try { LocalDate.parse(reservation?.date) == LocalDate.now() } catch (_: Exception) { false }
    val ahora = LocalTime.now()

    // Parse saved times
    val savedStart = reservation?.startTime ?: "08:00"
    val savedEnd = reservation?.endTime ?: "09:00"
    val savedStartH = savedStart.split(":").getOrNull(0)?.toIntOrNull() ?: 8
    val savedStartM = savedStart.split(":").getOrNull(1)?.toIntOrNull() ?: 0

    // Min start time (same logic as NewReservation)
    val minStartTotalMinutes = if (esHoy) {
        val currentSlot = (ahora.hour * 60 + ahora.minute) / 15
        val nextSlot = currentSlot + 1
        maxOf(nextSlot * 15, 6 * 60)
    } else 6 * 60
    val minStartH = minStartTotalMinutes / 60
    val minStartM = minStartTotalMinutes % 60

    var startHour by remember { mutableIntStateOf(savedStartH) }
    var startMinute by remember { mutableIntStateOf(savedStartM) }
    val startTime = remember(startHour, startMinute) { LocalTime.of(startHour, startMinute) }
    var endTime by remember {
        val parsed = try { LocalTime.parse(savedEnd) } catch (_: Exception) { LocalTime.of(9, 0) }
        mutableStateOf(parsed)
    }

    val horaPasada = esHoy && minStartTotalMinutes >= 23 * 60
    val startTotalMinutes = startHour * 60 + startMinute
    val startValid = !esHoy || (startTotalMinutes >= minStartTotalMinutes / 15 * 15)
    val duracionMin = (endTime.hour * 60 + endTime.minute) - (startHour * 60 + startMinute)
    val isValidDuration = duracionMin > 0 && duracionMin <= 480
    val edicionValida = isValidDuration && startValid && !horaPasada

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar Reserva", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ParklySurface)
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

            // Reservation info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ParklyOrangeLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, ParklyOrange)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Plaza ${reservation?.spotNumber ?: "—"}",
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ParklyOrange
                    )
                    Text(
                        reservation?.date ?: "—",
                        color = ParklyTextSecondary, fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Start time picker
            Text("Hora de Inicio", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            ParkingTimePicker(
                minHour = minStartH.coerceAtMost(22),
                minMinute = minStartM,
                initialHour = savedStartH,
                initialMinute = savedStartM,
                onTimeChanged = { hour, minute ->
                    if (hour >= 0) startHour = hour
                    if (minute >= 0) startMinute = minute
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Duration slider
            ParkingDurationSlider(
                startTime = startTime,
                onEndTimeSelected = { newEnd -> endTime = newEnd }
            )

            // Validation
            if (horaPasada) {
                Text("Ya no puedes modificar esta reserva.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            } else if (!startValid) {
                Text("Selecciona una hora futura.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            } else if (duracionMin > 480) {
                Text("La duración máxima es de 8 horas.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            // Other reservations on this spot
            val otrasReservas = uiState.spotReservations.filter { it.id != idReserva }
            if (otrasReservas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Reservas en esta plaza:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF8D6E00))
                        Spacer(modifier = Modifier.height(4.dp))
                        otrasReservas.forEach { r ->
                            Text("• ${r.startTime} – ${r.endTime}", fontSize = 14.sp, color = Color(0xFF8D6E00), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Error
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = {
                        if (edicionValida) {
                            val start = String.format("%02d:%02d", startHour, startMinute)
                            val end = String.format("%02d:%02d", endTime.hour, endTime.minute)
                            viewModel.updateReservation(idReserva, start, end)
                        }
                    },
                    enabled = edicionValida,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange)
                ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
