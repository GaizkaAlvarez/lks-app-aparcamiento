package com.parkinglksnext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.parkinglksnext.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenMenu: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val allReservations = uiState.currentReservations + uiState.futureReservations + uiState.pastReservations

    Scaffold(
        containerColor = ParklyBackground,
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
                        text = "Historial",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParklyTextPrimary,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ParklyOrange)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Error",
                        color = ParklyRed,
                        fontSize = 14.sp
                    )
                }
            }
            allReservations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🅿", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sin historial",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParklyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tus reservas aparecerán aquí",
                            fontSize = 13.sp,
                            color = ParklyTextSecondary
                        )
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
                    if (uiState.currentReservations.isNotEmpty()) {
                        item {
                            HistorySectionHeader(title = "En Curso", color = ParklyOrange)
                        }
                        items(uiState.currentReservations, key = { it.id }) { reserva ->
                            HistoryCard(reservation = reserva, statusOverride = "En Curso", statusColor = ParklyOrange, statusBg = ParklyOrangeLight)
                        }
                    }
                    if (uiState.futureReservations.isNotEmpty()) {
                        item {
                            HistorySectionHeader(title = "Próximas", color = ParklyGreen)
                        }
                        items(uiState.futureReservations, key = { it.id }) { reserva ->
                            HistoryCard(reservation = reserva, statusOverride = "Próxima", statusColor = ParklyGreen, statusBg = ParklyGreenLight)
                        }
                    }
                    if (uiState.pastReservations.isNotEmpty()) {
                        item {
                            HistorySectionHeader(title = "Historial", color = ParklyTextSecondary)
                        }
                        items(uiState.pastReservations, key = { it.id }) { reserva ->
                            val (statusText, statusColor, statusBg) = when (reserva.status) {
                                "cancelled" -> Triple("Cancelada", ParklyRed, ParklyRedLight)
                                else        -> Triple("Completada", ParklyGreen, ParklyGreenLight)
                            }
                            HistoryCard(reservation = reserva, statusOverride = statusText, statusColor = statusColor, statusBg = statusBg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ParklyTextPrimary
        )
    }
}

@Composable
private fun HistoryCard(
    reservation: Reservation,
    statusOverride: String,
    statusColor: Color,
    statusBg: Color
) {
    val spotTypeEmoji = when (reservation.spotType) {
        "electric"   -> "⚡"
        "motorcycle" -> "🏍"
        else         -> "🚗"
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(spotTypeEmoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
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
                    Text(
                        text = "Level 1",
                        fontSize = 12.sp,
                        color = ParklyTextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = statusOverride,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
