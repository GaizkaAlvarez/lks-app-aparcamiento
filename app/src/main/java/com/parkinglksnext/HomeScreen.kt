package com.parkinglksnext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.ui.theme.*
import com.parkinglksnext.viewmodel.ProfileViewModel
import com.parkinglksnext.viewmodel.ReservationsViewModel

@Composable
fun HomeScreen(
    reservationsViewModel: ReservationsViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToNewReservation: () -> Unit = {},
    onNavigateToReservations: () -> Unit = {},
    onNavigateToChatBot: () -> Unit = {}
) {
    val reservationsState by reservationsViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val profile = profileState.userProfile

    val upcoming = reservationsState.currentReservations.firstOrNull()
        ?: reservationsState.futureReservations.firstOrNull()

    val firstName = profile?.firstName?.ifBlank { profile.name.split(" ").firstOrNull() } ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParklyBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo_lks),
                    contentDescription = "LKS Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LKS Parking",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ParklyTextPrimary
                )
            }
            if (firstName.isNotBlank()) {
                Text(
                    text = "Hola, $firstName 👋",
                    fontSize = 13.sp,
                    color = ParklyTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Hero ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = ParklyTextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        )
                    ) { append("Aparca fácil.\n\n") }
                    withStyle(
                        SpanStyle(
                            color = ParklyOrange,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        )
                    ) { append("Ahorra tiempo.") }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Reserva tu plaza con antelación y\nolvídate de dar vueltas.",
                fontSize = 14.sp,
                color = ParklyTextSecondary,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Main CTA button ──────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Button(
                onClick = onNavigateToChatBot,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✦", fontSize = 18.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Buscar Plaza",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "La IA encuentra tu mejor opción",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Find & Reserve card ──────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNewReservation() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ParklySurface),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(ParklyOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = ParklyOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Reservar Plaza",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = ParklyTextPrimary
                            )
                            Text(
                                text = "Elige tus preferencias",
                                fontSize = 12.sp,
                                color = ParklyTextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ParklyTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Upcoming Reservation section ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Próxima Reserva",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ParklyTextPrimary
            )
            TextButton(onClick = onNavigateToReservations) {
                Text(
                    text = "Ver todas",
                    color = ParklyOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (upcoming != null) {
            HomeUpcomingCard(
                reservation = upcoming,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ParklySurface),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sin reservas próximas",
                        fontSize = 15.sp,
                        color = ParklyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onNavigateToNewReservation) {
                        Text(
                            text = "Crear reserva",
                            color = ParklyOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HomeUpcomingCard(
    reservation: Reservation,
    modifier: Modifier = Modifier
) {
    val spotTypeEmoji = when (reservation.spotType) {
        "electric"   -> "⚡"
        "motorcycle" -> "🏍"
        else         -> "🚗"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ParklySurface),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Spot number badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d", reservation.spotNumber),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ParklyTextPrimary
                    )
                    Text(spotTypeEmoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
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
                        text = "Level 1 • Main Garage",
                        fontSize = 12.sp,
                        color = ParklyTextSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ParklyGreenLight)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Próxima",
                    color = ParklyGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
