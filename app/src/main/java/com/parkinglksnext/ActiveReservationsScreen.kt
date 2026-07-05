package com.parkinglksnext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.ui.theme.*
import com.parkinglksnext.viewmodel.ReservationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveReservationsScreen(
    viewModel: ReservationsViewModel,
    onNavigateToNewReservation: () -> Unit = {},
    onNavigateToEditReservation: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("En Curso", "Próximas")

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val shownReservations = when (selectedTab) {
        0    -> uiState.currentReservations
        else -> uiState.futureReservations
    }

    Scaffold(
        containerColor = ParklyBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ParklySurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Mis Reservas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParklyTextPrimary,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ParklySurface,
                    contentColor = ParklyOrange,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            selectedContentColor = ParklyOrange,
                            unselectedContentColor = ParklyTextSecondary
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewReservation,
                containerColor = ParklyOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+ Nueva Reserva", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && shownReservations.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ParklyOrange)
                }
            }
            shownReservations.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🅿", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 0) "Sin reservas en curso" else "Sin reservas próximas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParklyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reserva tu plaza ahora",
                            fontSize = 13.sp,
                            color = ParklyTextSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToNewReservation,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange)
                        ) {
                            Text("Crear Reserva", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shownReservations, key = { it.id }) { reservation ->
                        ParklyReservationCard(
                            reservation = reservation,
                            isActive = selectedTab == 0,
                            onEdit = { onNavigateToEditReservation(reservation.id) },
                            onCancel = { viewModel.cancelReservation(reservation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParklyReservationCard(
    reservation: Reservation,
    isActive: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val spotTypeEmoji = when (reservation.spotType) {
        "electric"   -> "⚡"
        "motorcycle" -> "🏍"
        else         -> "🚗"
    }
    val badgeText  = if (isActive) "En Curso" else "Próxima"
    val badgeBg    = if (isActive) ParklyOrangeLight else ParklyGreenLight
    val badgeColor = if (isActive) ParklyOrange else ParklyGreen

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar reserva") },
            text = { Text("¿Seguro que quieres cancelar la reserva de la plaza ${reservation.spotNumber}?") },
            confirmButton = {
                TextButton(onClick = { showCancelDialog = false; onCancel() }) {
                    Text("Sí, cancelar", color = ParklyRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Volver", color = ParklyTextSecondary)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParklySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Spot badge — larger emoji, no orange background
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp)
            ) {
                Text(
                    text = String.format("%02d", reservation.spotNumber),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ParklyTextPrimary
                )
                Text(spotTypeEmoji, fontSize = 28.sp)
                if (reservation.vehiclePlate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(reservation.vehiclePlate, fontSize = 12.sp, color = ParklyTextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reservation.date,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = ParklyTextPrimary
                )
                Text(
                    text = "${reservation.startTime} – ${reservation.endTime}",
                    fontSize = 13.sp,
                    color = ParklyTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = ParklyGrayLight, modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onEdit,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = ParklyOrange)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Editar", color = ParklyOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = { showCancelDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = ParklyRed)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancelar", color = ParklyRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
