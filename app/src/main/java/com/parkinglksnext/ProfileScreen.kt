package com.parkinglksnext

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkinglksnext.ui.theme.*
import com.parkinglksnext.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.userProfile

    val initials = remember(profile) {
        val first = profile?.firstName?.firstOrNull() ?: 'U'
        val last  = profile?.lastName?.firstOrNull()  ?: ' '
        "$first$last".trim().uppercase()
    }

    Scaffold(
        containerColor = ParklyBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ParklySurface)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Mi Perfil",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ParklyTextPrimary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ParklyOrange)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ── Avatar ───────────────────────────────────────────
            val photoBase64 = profile?.profileImageBase64?.takeIf { it.isNotEmpty() }
            val photoBitmap = remember(photoBase64) {
                photoBase64?.let {
                    try {
                        val bytes = Base64.decode(it, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) { null }
                }
            }

            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(ParklyOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParklyOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = profile?.name ?: "Usuario",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ParklyTextPrimary
            )
            Text(
                text = profile?.email ?: "",
                fontSize = 14.sp,
                color = ParklyTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Info card ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ParklySurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileInfoRow(
                        icon = Icons.Outlined.Person,
                        titulo = "Nombre Completo",
                        valor = profile?.name ?: "—"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = ParklyGrayLight
                    )
                    ProfileInfoRow(
                        icon = Icons.Outlined.Email,
                        titulo = "Correo Corporativo",
                        valor = profile?.email ?: "—"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = ParklyGrayLight
                    )
                    val primaryVehicle = profile?.vehicles?.firstOrNull()
                    ProfileInfoRow(
                        icon = Icons.Outlined.DirectionsCar,
                        titulo = "Vehículo Principal",
                        valor = if (primaryVehicle != null) {
                            "${primaryVehicle.licensePlate} (${primaryVehicle.type.replaceFirstChar { it.uppercase() }})"
                        } else "—"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Edit button ───────────────────────────────────────
            Button(
                onClick = onNavigateToEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ParklyOrange),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Editar Perfil",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Logout ────────────────────────────────────────────
            TextButton(
                onClick = onLogout,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Cerrar Sesión",
                    color = ParklyRed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, titulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ParklyGrayLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ParklyTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = titulo, fontSize = 12.sp, color = ParklyTextSecondary)
            Text(text = valor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ParklyTextPrimary)
        }
    }
}
