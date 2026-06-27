package com.parkinglksnext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
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
import com.parkinglksnext.viewmodel.NewReservationViewModel
import com.parkinglksnext.viewmodel.ProfileViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReservationScreen(
    viewModel: NewReservationViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToDashboard: () -> Unit = {},
    onOpenMenu: () -> Unit = {}
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

    // Collect one-shot success event (no boolean toggle loop)
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

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (uiState.step == 1) "Nueva Reserva" else "Seleccionar Plaza",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onOpenMenu() }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1Content(
    innerPadding: PaddingValues,
    uiState: NewReservationViewModel.NewReservationUiState,
    viewModel: NewReservationViewModel
) {
    val formatoVisual = remember { DateTimeFormatter.ofPattern("EEE d 'de' MMM", Locale.forLanguageTag("es-ES")) }
    val listaFechas = remember {
        (0..6).map { LocalDate.now().plusDays(it.toLong()) }
    }

    // Initialize date if not set
    LaunchedEffect(Unit) {
        if (uiState.selectedDate == null) {
            viewModel.setDate(listaFechas.first().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        }
        if (uiState.startTime == null) viewModel.setStartTime("08:00")
        if (uiState.endTime == null) viewModel.setEndTime("14:55")
    }

    var fechaSeleccionada by remember { mutableStateOf(listaFechas.first()) }

    val rangoHorasInicio = remember { (6..22).map { stringOf(it) } }
    val rangoHorasFin = remember { (6..22).map { stringOfEnd(it) } }
    var horaInicio by remember(uiState.startTime) { mutableStateOf(uiState.startTime ?: "08:00") }
    var horaFin by remember(uiState.endTime) { mutableStateOf(uiState.endTime ?: "14:55") }
    var expandidoInicio by remember { mutableStateOf(false) }
    var expandidoFin by remember { mutableStateOf(false) }

    // Vehicle selector state
    val vehicles = uiState.userVehicles
    val vehicleLabels = vehicles.map { v ->
        val typeName = v.type.replaceFirstChar { c -> c.uppercase() }
        "${v.licensePlate} ($typeName)"
    }
    val selectedIndex = vehicles.indexOf(uiState.selectedVehicle).coerceAtLeast(0)
    var vehiculoLabel by remember(uiState.selectedVehicle) {
        mutableStateOf(if (vehicles.isNotEmpty()) vehicleLabels[selectedIndex] else "Sin vehículos")
    }
    var expandidoVehiculo by remember { mutableStateOf(false) }

    val horaInicioMin = horaInicio.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
    val horaFinMin = horaFin.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
    val duracionMin = horaFinMin - horaInicioMin
    val horasValidas = duracionMin > 0 && duracionMin <= 540  // max 9h = 540 min

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- VEHÍCULOS ---
        Text("Vehículo para la reserva", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
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
                    onClick = {
                        fechaSeleccionada = fecha1
                        viewModel.setDate(fecha1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    },
                    modifier = Modifier.weight(1f)
                )
                if (i + 1 < listaFechas.size) {
                    val fecha2 = listaFechas[i + 1]
                    BotonDia(
                        texto = fecha2.format(formatoVisual).replaceFirstChar { it.uppercase() },
                        isSelected = fechaSeleccionada == fecha2,
                        onClick = {
                            fechaSeleccionada = fecha2
                            viewModel.setDate(fecha2.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- DESPLEGABLES DE HORAS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        rangoHorasInicio.forEach { hora ->
                            DropdownMenuItem(
                                text = { Text(hora) },
                                onClick = {
                                    horaInicio = hora
                                    viewModel.setStartTime(hora)
                                    expandidoInicio = false
                                }
                            )
                        }
                    }
                }
            }

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
                        rangoHorasFin.forEach { hora ->
                            DropdownMenuItem(
                                text = { Text(hora) },
                                onClick = {
                                    horaFin = hora
                                    viewModel.setEndTime(hora)
                                    expandidoFin = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (!horasValidas) {
            val mensaje = if (duracionMin <= 0) {
                "La hora de fin debe ser posterior a la de inicio."
            } else {
                "La duración máxima es de 9 horas."
            }
            Text(
                text = mensaje,
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
                    Text(
                        uiState.selectedVehicle?.licensePlate ?: "—",
                        color = Color(0xFF1E3A8A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Reserva programada para el ${fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    fontSize = 13.sp,
                    color = Color(0xFF3B82F6)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- BOTÓN CONTINUAR ---
        Button(
            onClick = { viewModel.goToStep2() },
            enabled = horasValidas && uiState.selectedVehicle != null,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Resumen de la Reserva", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F2537))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fecha: ${uiState.selectedDate ?: "—"}", fontSize = 14.sp, color = Color.Gray)
                Text("Horario: ${uiState.startTime ?: "—"} - ${uiState.endTime ?: "—"}", fontSize = 14.sp, color = Color.Gray)
                Text("Vehículo: ${uiState.selectedVehicle?.licensePlate ?: "—"}", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selected spot card
        if (uiState.selectedSpot != null) {
            val spot = uiState.selectedSpot  // non-null after if check
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Plaza ${spot.number}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = spot.type.replaceFirstChar { it.uppercase() },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Spot grid title
        Text(
            "Plazas Disponibles",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF0F2537),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Spot type legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = Color(0xFFFF6B00), label = "Combustión")
            LegendItem(color = Color(0xFF137333), label = "Eléctrico")
            LegendItem(color = Color(0xFF1A73E8), label = "Moto")
        }

        // Spot grid — show loading indicator while Firestore fetches spots
        if (uiState.spotsLoading && uiState.availableSpots.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFF6B00))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando plazas...", fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else if (uiState.availableSpots.isEmpty()) {
            Text(
                "No hay plazas disponibles para este tipo de vehículo.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            val spotsByType = uiState.availableSpots.groupBy { it.type }
            SpotsSection("Combustión", spotsByType["combustion"] ?: emptyList(), uiState.selectedSpot, Color(0xFFFF6B00)) { viewModel.selectSpot(it) }
            SpotsSection("Eléctrico", spotsByType["electric"] ?: emptyList(), uiState.selectedSpot, Color(0xFF137333)) { viewModel.selectSpot(it) }
            SpotsSection("Moto", spotsByType["motorcycle"] ?: emptyList(), uiState.selectedSpot, Color(0xFF1A73E8)) { viewModel.selectSpot(it) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.goBackToStep1() },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Atrás", color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.confirmReservation() },
                enabled = uiState.selectedSpot != null && !uiState.isLoading,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun SpotsSection(
    title: String,
    spots: List<ParkingSpot>,
    selectedSpot: ParkingSpot?,
    accentColor: Color,
    onSelect: (ParkingSpot) -> Unit
) {
    if (spots.isEmpty()) return

    Text(
        title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = Color(0xFF0F2537),
        modifier = Modifier.padding(bottom = 8.dp)
    )

    // Simple grid using Row wrapping
    var rowItems = mutableListOf<ParkingSpot>()
    for (i in spots.indices) {
        rowItems.add(spots[i])
        if (rowItems.size == 5 || i == spots.lastIndex) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { spot ->
                    val isSelected = selectedSpot?.id == spot.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                color = when {
                                    isSelected -> accentColor
                                    spot.type == "electric" -> Color(0xFFE6F4EA)
                                    spot.type == "motorcycle" -> Color(0xFFE8F0FE)
                                    else -> Color(0xFFF0F0F0)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(spot) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${spot.number}",
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> Color.White
                                spot.type == "electric" -> Color(0xFF137333)
                                spot.type == "motorcycle" -> Color(0xFF1A73E8)
                                else -> Color(0xFF495057)
                            }
                        )
                    }
                }
                // Fill remaining space if row is incomplete
                repeat(5 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            rowItems = mutableListOf()
        }
    }
}

// ─── Shared utilities ────────────────────────────────────────────

fun stringOf(hora: Int): String = if (hora < 10) "0$hora:00" else "$hora:00"
fun stringOfEnd(hora: Int): String = if (hora < 10) "0$hora:55" else "$hora:55"

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
