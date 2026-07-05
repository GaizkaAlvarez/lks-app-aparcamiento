package com.parkinglksnext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.ui.theme.*
import com.parkinglksnext.viewmodel.NewReservationViewModel
import com.parkinglksnext.viewmodel.ProfileViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReservationScreen(
    viewModel: NewReservationViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    // Feed user's vehicles from profile into the reservation ViewModel
    LaunchedEffect(profileUiState.userProfile) {
        profileUiState.userProfile?.vehicles?.let { vehicles ->
            if (vehicles.isNotEmpty()) {
                viewModel.setUserVehicles(vehicles)
                // Pre-select first vehicle if none selected
                if (uiState.selectedVehicle == null) {
                    viewModel.setVehicle(vehicles.first())
                }
            }
        }
    }

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Collect one-shot success event
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.parkinglksnext.viewmodel.NewReservationEvent.Success -> {
                    viewModel.resetState()
                    onNavigateToDashboard()
                }
            }
        }
    }

    Scaffold(
        containerColor = ParklyBackground,
        topBar = {
            Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .background(ParklySurface)
            ) {
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (uiState.step == 1) onNavigateBack() else viewModel.goBackToStep1() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ParklyTextPrimary
                        )
                    }
                    Text(
                        text = if (uiState.step == 1) "Nueva Reserva" else "Seleccionar Plaza",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParklyTextPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.step == 1) {
            Step1Content(
                innerPadding = innerPadding,
                uiState = uiState,
                viewModel = viewModel
            )
        } else {
            Step2Content(
                innerPadding = innerPadding,
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

// ─── STEP 1: Vehicle, Date, Time selection ────────────────────────

data class DurationOption(val label: String, val minutes: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1Content(
    innerPadding: PaddingValues,
    uiState: NewReservationViewModel.NewReservationUiState,
    viewModel: NewReservationViewModel
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val today = remember { LocalDate.now() }

    // Calendar state
    var displayedMonth by remember { mutableStateOf(today) }
    var selectedDate by remember(uiState.selectedDate) {
        mutableStateOf(
            if (uiState.selectedDate != null) LocalDate.parse(uiState.selectedDate)
            else today
        )
    }
    val esHoy = selectedDate == today

    // Calculate minimum start time
    val ahora = remember { LocalTime.now() }
    val minStartTotalMinutes = if (esHoy) {
        val nowMinutes = ahora.hour * 60 + ahora.minute
        val withBuffer = nowMinutes
        val nextSlot = (withBuffer + 14) / 15
        maxOf(nextSlot * 15, 6 * 60)
    } else 6 * 60
    val minStartH = minStartTotalMinutes / 60
    val minStartM = minStartTotalMinutes % 60
    val horaPasada = esHoy && minStartTotalMinutes >= 23 * 60

    // Start time — resets when date changes (esHoy toggles)
    val savedStartH = uiState.startTime?.split(":")?.getOrNull(0)?.toIntOrNull()
    val savedStartM = uiState.startTime?.split(":")?.getOrNull(1)?.toIntOrNull()
    var startHour by remember(esHoy) {
        mutableIntStateOf(savedStartH ?: if (esHoy) minStartH else 8)
    }
    var startMinute by remember(esHoy) {
        mutableIntStateOf(savedStartM ?: if (esHoy) minStartM else 0)
    }

    val startTime = remember(startHour, startMinute) { LocalTime.of(startHour, startMinute) }
    val startTotalMinutes = startHour * 60 + startMinute
    val startValid = !esHoy || (startTotalMinutes >= minStartTotalMinutes / 15 * 15)

    // End time — recalculates when startTime changes
    var endTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    LaunchedEffect(startTime) {
        val savedEnd = uiState.endTime
        val initialEnd = if (savedEnd != null) {
            try { LocalTime.parse(savedEnd) } catch (_: Exception) { startTime.plusMinutes(60) }
        } else startTime.plusMinutes(60)
        endTime = initialEnd
        viewModel.setEndTime(String.format("%02d:%02d", initialEnd.hour, initialEnd.minute))
    }


    // Initialize defaults in ViewModel
    LaunchedEffect(Unit) {
        if (uiState.selectedDate == null) {
            viewModel.setDate(today.format(dateFormatter))
        }
        if (uiState.startTime == null) {
            viewModel.setStartTime(String.format("%02d:%02d", startHour, startMinute))
        }
        if (uiState.endTime == null) {
            viewModel.setEndTime(String.format("%02d:%02d", endTime.hour, endTime.minute))
        }
    }

    val duracionMin = (endTime.hour * 60 + endTime.minute) - (startHour * 60 + startMinute)
    val horasValidas = duracionMin > 0 && duracionMin <= 540
    val reservaValida = horasValidas && startValid && !horaPasada

    // Vehicle selector
    val vehicles = uiState.userVehicles
    val vehicleLabels = vehicles.map { v ->
        "${v.licensePlate} (${vehicleTypeLabel(v.type)})"
    }
    val selectedIndex = vehicles.indexOf(uiState.selectedVehicle).coerceAtLeast(0)
    var vehiculoLabel by remember(uiState.selectedVehicle) {
        mutableStateOf(if (vehicles.isNotEmpty()) vehicleLabels[selectedIndex] else "Sin vehículos")
    }
    var expandidoVehiculo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- VEHÍCULOS ---
        Text("Vehículo", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        ExposedDropdownMenuBox(
            expanded = expandidoVehiculo,
            onExpandedChange = { expandidoVehiculo = !expandidoVehiculo },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            OutlinedTextField(
                value = vehiculoLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoVehiculo) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            ExposedDropdownMenu(
                expanded = expandidoVehiculo,
                onDismissRequest = { expandidoVehiculo = false }
            ) {
                vehicles.forEachIndexed { index, vehicle ->
                    DropdownMenuItem(
                        text = { Text(vehicleLabels[index]) },
                        onClick = {
                            vehiculoLabel = vehicleLabels[index]
                            viewModel.setVehicle(vehicle)
                            expandidoVehiculo = false
                        }
                    )
                }
            }
        }

        // --- CALENDARIO ---
        Text("Fecha", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        TwoMonthCalendar(
            today = today,
            displayedMonth = displayedMonth,
            selectedDate = selectedDate,
            onMonthChange = { displayedMonth = it },
            onDateSelected = { date ->
                selectedDate = date
                viewModel.setDate(date.format(dateFormatter))
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- HORA DE INICIO (Reloj digital) ---
        Text(
            "Hora de Inicio",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ParkingTimePicker(
            minHour = minStartH,
            minMinute = minStartM,
            initialHour = startHour,
            initialMinute = startMinute,
            onTimeChanged = { hour, minute ->
                if (hour >= 0) startHour = hour
                if (minute >= 0) startMinute = minute
                val formatted = String.format("%02d:%02d", startHour, startMinute)
                viewModel.setStartTime(formatted)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- DURACIÓN (Slider) ---
        ParkingDurationSlider(
            startTime = startTime,
            onEndTimeSelected = { newEnd ->
                endTime = newEnd
                val formatted = String.format("%02d:%02d", newEnd.hour, newEnd.minute)
                viewModel.setEndTime(formatted)
            }
        )

        // Validation messages
        if (horaPasada) {
            Text(
                text = "Ya no puedes reservar para hoy. Selecciona otro día.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (!startValid) {
            Text(
                text = "Selecciona una hora futura para hoy.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (!horasValidas && duracionMin > 0) {
            Text(
                text = "La duración máxima es de 9 horas.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- CAJA DE RESUMEN ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F4FA), shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Row {
                    Text("Resumen: ", fontWeight = FontWeight.Bold, color = ParklyTextPrimary)
                    Text(
                        uiState.selectedVehicle?.licensePlate ?: "—",
                        color = Color(0xFF1E3A8A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Reserva programada para el ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    fontSize = 13.sp,
                    color = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "De ${String.format("%02d:%02d", startHour, startMinute)} a ${String.format("%02d:%02d", endTime.hour, endTime.minute)}",
                    fontSize = 13.sp,
                    color = Color(0xFF3B82F6)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- BOTÓN CONTINUAR ---
        Button(
            onClick = { viewModel.goToStep2() },
            enabled = reservaValida && uiState.selectedVehicle != null,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text("Continuar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── STEP 2: Spot selection grid ──────────────────────────────────

@Composable
private fun Step2Content(
    innerPadding: PaddingValues,
    uiState: NewReservationViewModel.NewReservationUiState,
    viewModel: NewReservationViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ParklySurface),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Resumen", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ParklyTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fecha: ${uiState.selectedDate ?: "—"}", fontSize = 13.sp, color = ParklyTextSecondary)
                Text("Horario: ${uiState.startTime ?: "—"} - ${uiState.endTime ?: "—"}", fontSize = 13.sp, color = ParklyTextSecondary)
                Text("Vehículo: ${uiState.selectedVehicle?.licensePlate ?: "—"}", fontSize = 13.sp, color = ParklyTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selected spot card
        if (uiState.selectedSpot != null) {
            val spot = uiState.selectedSpot  // non-null after if check
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ParklyOrangeLight),
                border = androidx.compose.foundation.BorderStroke(2.dp, ParklyOrange)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ParklyOrange,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Plaza ${spot.number} seleccionada",
                            color = ParklyOrange,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = spotTypeLabel(spot.type),
                            color = ParklyOrangeDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendItem(color = SpotAvailable, label = "Disponible")
            LegendItem(color = ParklyOrange, label = "Ocupada")
            LegendItem(color = SpotSelected, label = "Seleccionada")
        }

        // Loading
        if (uiState.spotsLoading && uiState.allCompatibleSpots.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ParklyOrange)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando plazas...", fontSize = 14.sp, color = ParklyTextSecondary)
                }
            }
        } else {
            // Visual parking lot
            ParkingLotView(
                allCompatibleSpots = uiState.allCompatibleSpots,
                conflictingIds = uiState.conflictingSpotIds,
                selectedSpot = uiState.selectedSpot,
                onSelectSpot = { spot -> if (spot.id !in uiState.conflictingSpotIds) viewModel.selectSpot(spot) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.confirmReservation() },
                enabled = uiState.selectedSpot != null && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirmar Reserva", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            OutlinedButton(
                onClick = { viewModel.goBackToStep1() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4ED)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ParklyTextSecondary)
            ) {
                Text("Atrás", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, color = ParklyTextSecondary)
    }
}

// ─── Visual Parking Lot ─────────────────────────────────────────

@Composable
private fun ParkingLotView(
    allCompatibleSpots: List<ParkingSpot>,
    conflictingIds: Set<String>,
    selectedSpot: ParkingSpot?,
    onSelectSpot: (ParkingSpot) -> Unit
) {
    val spotsByNumber = allCompatibleSpots.associateBy { it.number }

    // Define the parking lot layout rows with section headers
    data class SpotSlot(val number: Int, val type: String)
    data class ParkingRow(val label: String?, val spots: List<SpotSlot>)

    val parkingRows = listOf(
        ParkingRow("🚗 Común (1-21)", listOf(
            SpotSlot(1, "comun"), SpotSlot(2, "comun"), SpotSlot(3, "comun"),
            SpotSlot(4, "comun"), SpotSlot(5, "comun"), SpotSlot(6, "comun"),
            SpotSlot(7, "comun")
        )),
        ParkingRow(null, listOf(
            SpotSlot(8, "comun"), SpotSlot(9, "comun"), SpotSlot(10, "comun"),
            SpotSlot(11, "comun"), SpotSlot(12, "comun"), SpotSlot(13, "comun"),
            SpotSlot(14, "comun")
        )),
        ParkingRow(null, listOf(
            SpotSlot(15, "comun"), SpotSlot(16, "comun"), SpotSlot(17, "comun"),
            SpotSlot(18, "comun"), SpotSlot(19, "comun"), SpotSlot(20, "comun"),
            SpotSlot(21, "comun")
        )),
        ParkingRow("⚡ Con cargador (22-28)", listOf(
            SpotSlot(22, "electric"), SpotSlot(23, "electric"), SpotSlot(24, "electric"),
            SpotSlot(25, "electric"), SpotSlot(26, "electric"), SpotSlot(27, "electric"),
            SpotSlot(28, "electric")
        )),
        ParkingRow("🏍 Moto (29-35)", listOf(
            SpotSlot(29, "motorcycle"), SpotSlot(30, "motorcycle"), SpotSlot(31, "motorcycle"),
            SpotSlot(32, "motorcycle"), SpotSlot(33, "motorcycle"), SpotSlot(34, "motorcycle"),
            SpotSlot(35, "motorcycle")
        )),
    )

    // Parking lot container — white card with dark border
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFFCCCCCC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Entry indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("⬇ ENTRADA ⬇", color = ParklyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            parkingRows.forEachIndexed { rowIndex, parkingRow ->
                // Section label
                if (parkingRow.label != null) {
                    if (rowIndex > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFDDDDDD))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        parkingRow.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ParklyTextPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Spot row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    parkingRow.spots.forEach { slot ->
                        val spot = spotsByNumber[slot.number]
                        val isOccupied = spot != null && spot.id in conflictingIds
                        val isSelected = selectedSpot?.number == slot.number
                        val isAvailable = spot != null && spot.id !in conflictingIds
                        val isIncompatible = spot == null

                        ParkingSpotBox(
                            number = slot.number,
                            type = slot.type,
                            isOccupied = isOccupied,
                            isSelected = isSelected,
                            isAvailable = isAvailable,
                            isIncompatible = isIncompatible,
                            onClick = {
                                if (isAvailable && !isOccupied) {
                                    onSelectSpot(spot!!)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val remaining = 7 - parkingRow.spots.size
                    repeat(remaining) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ParkingSpotBox(
    number: Int,
    type: String,
    isOccupied: Boolean,
    isSelected: Boolean,
    isAvailable: Boolean,
    isIncompatible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> ParklyOrange
        isAvailable -> Color.White
        isOccupied -> Color(0xFFFFF5F0)    // Light warm white for occupied
        else -> Color(0xFFF0F0F0)           // Light gray for incompatible
    }
    val borderColor = when {
        isSelected -> ParklyOrange
        isOccupied -> ParklyOrange
        isAvailable -> SpotAvailable
        else -> Color(0xFFCCCCCC)
    }
    val borderWidth = if (isSelected) 2.5.dp else 1.5.dp
    val textColor = when {
        isSelected -> Color.White
        isAvailable -> ParklyTextPrimary
        isOccupied -> ParklyTextPrimary
        else -> Color(0xFFBBBBBB)
    }

    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.7f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isOccupied) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚗", fontSize = 18.sp)
                    Text(
                        String.format("%02d", number),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParklyOrange
                    )
                }
            } else if (isIncompatible) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format("%02d", number), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFCCCCCC))
                }
            } else {
                // Available or selected
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format("%02d", number),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = textColor
                    )
                    val typeIcon = when (type) {
                        "electric" -> "⚡"
                        "motorcycle" -> "🏍"
                        else -> ""
                    }
                    if (typeIcon.isNotEmpty()) {
                        Text(typeIcon, fontSize = 11.sp)
                    }
                    if (isSelected) {
                        Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Calendar Component ─────────────────────────────────────────

@Composable
private fun TwoMonthCalendar(
    today: LocalDate,
    displayedMonth: LocalDate,
    selectedDate: LocalDate,
    onMonthChange: (LocalDate) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = YearMonth.from(displayedMonth)
    val nextMonth = currentMonth.plusMonths(1)
    val dayNames = remember { listOf("L", "M", "X", "J", "V", "S", "D") }

    Column {
        // Month header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(displayedMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior", tint = ParklyTextSecondary)
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es"))).replaceFirstChar { it.uppercase() },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ParklyTextPrimary
            )
            IconButton(onClick = { onMonthChange(displayedMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente", tint = ParklyTextSecondary)
            }
        }

        // Single month
        MonthGrid(
            month = currentMonth,
            today = today,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startDayOfWeek = (firstDay.dayOfWeek.value - 1) // Monday = 0

    Column(modifier = modifier) {
        // Month label
        Text(
            text = month.month.name.replaceFirstChar { it.uppercase() }.take(3),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ParklyTextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { name ->
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = ParklyTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Days grid
        val totalCells = startDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - startDayOfWeek + 1
                    if (day in 1..daysInMonth) {
                        val date = month.atDay(day)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val isPast = date.isBefore(today)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isSelected -> ParklyOrange
                                        isToday -> ParklyOrangeLight
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = !isPast) { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$day",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> Color.White
                                    isPast -> Color(0xFFCCCCCC)
                                    else -> ParklyTextPrimary
                                }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─── Digital Clock Wheel Picker ─────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ParkingTimePicker(
    minHour: Int,
    minMinute: Int,
    initialHour: Int,
    initialMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit
) {
    // Clamp to valid range — minHour could be >22 late at night
    val safeMinHour = minHour.coerceAtMost(22)
    val hours = remember(safeMinHour) { (safeMinHour..22).toList() }
    val allMinutes = remember { listOf(0, 15, 30, 45) }

    val firstHourMinMinutes = remember(safeMinHour, minMinute) {
        allMinutes.filter { it >= minMinute }
    }

    var selectedHour by remember { mutableIntStateOf(initialHour.coerceIn(safeMinHour, 22)) }
    var selectedMinute by remember {
        mutableIntStateOf(
            if (initialHour == safeMinHour) initialMinute.coerceAtLeast(minMinute)
            else initialMinute
        )
    }

    val minutes = remember(selectedHour, safeMinHour, minMinute) {
        if (selectedHour == safeMinHour) firstHourMinMinutes else allMinutes
    }

    // Initialize hour index
    val initialHourIndex = hours.indexOf(selectedHour).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParklySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelColumn(
                items = hours,
                initialIndex = initialHourIndex,
                labelFormatter = { String.format("%02d", it) },
                onItemSelected = { hour ->
                    selectedHour = hour
                    onTimeChanged(hour, -1)
                },
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = ParklyOrange,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            WheelColumn(
                items = minutes,
                initialIndex = minutes.indexOf(selectedMinute).coerceAtLeast(0),
                labelFormatter = { String.format("%02d", it) },
                onItemSelected = { minute ->
                    selectedMinute = minute
                    onTimeChanged(-1, minute)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelColumn(
    items: List<T>,
    initialIndex: Int,
    labelFormatter: (T) -> String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val currentIndex = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) 0
            else {
                val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItemsInfo.minByOrNull {
                    kotlin.math.abs((it.offset + it.size / 2) - center)
                }?.index ?: 0
            }
        }
    }

    LaunchedEffect(currentIndex.value) {
        if (currentIndex.value in items.indices) {
            onItemSelected(items[currentIndex.value])
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val isSelected = index == currentIndex.value
                Text(
                    text = labelFormatter(items[index]),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = if (isSelected) 26.sp else 18.sp
                    ),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) ParklyOrange else ParklyTextSecondary,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .alpha(if (isSelected) 1f else 0.35f)
                )
            }
        }
    }
}

// ─── Duration Selector (Chips + end time) ───────────────────────

// ─── Duration Slider (15-min steps up to 3h, then 1h steps up to 8h) ──

@Composable
fun ParkingDurationSlider(
    startTime: LocalTime,
    onEndTimeSelected: (LocalTime) -> Unit
) {
    val limitTime = remember { LocalTime.of(23, 0) }

    // Build non-linear steps: 15min increments up to 3h, then 1h up to 8h
    val durationSteps = remember {
        val steps = mutableListOf<Int>()
        // 15min steps: 15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180
        var m = 15
        while (m <= 180) {
            steps.add(m)
            m += 15
        }
        // 1h steps: 240, 300, 360, 420, 480
        m = 240
        while (m <= 480) {
            steps.add(m)
            m += 60
        }
        steps
    }

    // Filter steps that fit within the available time until 23:00
    val maxAvailableMinutes = remember(startTime) {
        ChronoUnit.MINUTES.between(startTime, limitTime).toInt()
    }
    val validSteps = remember(durationSteps, maxAvailableMinutes) {
        durationSteps.filter { it <= maxAvailableMinutes }
    }

    if (validSteps.isEmpty()) {
        Text(
            "No es posible reservar después de las 23:00",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    val maxIndex = validSteps.size - 1
    // Default to 1h (index of 60 = 3)
    val defaultIndex = validSteps.indexOf(60).coerceAtLeast(0)
    var selectedIndex by remember(startTime) { mutableStateOf(defaultIndex.toFloat()) }

    val totalSelectedMinutes = validSteps.getOrElse(selectedIndex.toInt()) { validSteps.last() }
    val endTime = startTime.plusMinutes(totalSelectedMinutes.toLong())

    val hoursSelected = totalSelectedMinutes / 60
    val minutesSelected = totalSelectedMinutes % 60

    val durationText = buildString {
        if (hoursSelected > 0) append("${hoursSelected}h ")
        if (minutesSelected > 0 || hoursSelected == 0) append("${minutesSelected} min")
    }

    LaunchedEffect(endTime) {
        onEndTimeSelected(endTime)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParklySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Duración del estacionamiento",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ParklyTextPrimary
            )

            // Duration + end time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Duración", fontSize = 11.sp, color = ParklyTextSecondary)
                    Text(
                        text = durationText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ParklyOrange
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Salida estimada", fontSize = 11.sp, color = ParklyTextSecondary)
                    Text(
                        text = String.format("%02d:%02d", endTime.hour, endTime.minute),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParklyTextPrimary
                    )
                }
            }

            // Slider
            Slider(
                value = selectedIndex,
                onValueChange = { selectedIndex = it },
                valueRange = 0f..maxIndex.toFloat(),
                steps = (maxIndex - 1).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = ParklyOrange,
                    activeTrackColor = ParklyOrange,
                    inactiveTrackColor = ParklyOrangeLight,
                )
            )

            // Range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatStepMinutes(validSteps.first()),
                    fontSize = 11.sp,
                    color = ParklyTextSecondary
                )
                Text(
                    text = formatStepMinutes(validSteps.last()),
                    fontSize = 11.sp,
                    color = ParklyTextSecondary
                )
            }
        }
    }
}

fun formatStepMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return buildString {
        if (h > 0) append("${h}h")
        if (m > 0) {
            if (h > 0) append(" ")
            append("${m}min")
        }
    }
}

// ─── Label helpers (Spanish) ────────────────────────────────────

private fun vehicleTypeLabel(type: String): String = when (type) {
    "electric" -> "Eléctrico"
    "motorcycle" -> "Moto"
    else -> "Común"
}

private fun spotTypeLabel(type: String): String = when (type) {
    "electric" -> "Con cargador"
    "motorcycle" -> "Moto"
    else -> "Común"
}

/**
 * Round end time to :10, :25, :40, or :55 so there's always
 * a 5-minute gap before the next reservation slot.
 *
 * Algorithm: subtract 5 min → round down to nearest :15 → add 10 min
 *   13:15 → 13:10 → 13:00 → 13:10 ✓
 *   13:30 → 13:25 → 13:15 → 13:25 ✓
 *   13:45 → 13:40 → 13:30 → 13:40 ✓
 *   14:00 → 13:55 → 13:45 → 13:55 ✓
 */
fun roundEndTime(raw: LocalTime): LocalTime {
    val totalMinutes = raw.hour * 60 + raw.minute
    val adjusted = totalMinutes - 5
    val roundedDown = (adjusted / 15) * 15
    val result = roundedDown + 10
    val hour = (result / 60) % 24
    val minute = result % 60
    return LocalTime.of(hour, minute)
}
