package com.parkinglksnext.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkinglksnext.UserProfile

/**
 * Side drawer matching the Figma hamburger menu design.
 * Shows gradient header, user info, nav items, and logout.
 */
@Composable
fun DrawerContent(
    userProfile: UserProfile?,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        // ─── Header: gradient orange background with user info ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF6B00), Color(0xFFFF8C00))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "LKS Next Parking",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bienvenido/a",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                Text(
                    text = userProfile?.name ?: userProfile?.email ?: "Usuario",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!userProfile?.licensePlate.isNullOrBlank()) {
                    Text(
                        text = userProfile?.licensePlate ?: "",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Navigation items ────────────────────────────────────
        DrawerNavItem(
            icon = Icons.Default.Home,
            label = "Reservas Activas",
            isSelected = currentRoute == Routes.Dashboard.route,
            onClick = { onNavigate(Routes.Dashboard.route) }
        )
        DrawerNavItem(
            icon = Icons.Default.CalendarMonth,
            label = "Nueva Reserva",
            isSelected = currentRoute == Routes.NewReservation.route,
            onClick = { onNavigate(Routes.NewReservation.route) }
        )
        DrawerNavItem(
            icon = Icons.Default.Schedule,
            label = "Historial",
            isSelected = currentRoute == Routes.History.route,
            onClick = { onNavigate(Routes.History.route) }
        )
        DrawerNavItem(
            icon = Icons.Default.Person,
            label = "Perfil",
            isSelected = currentRoute in listOf(Routes.Profile.route, Routes.EditProfile.route),
            onClick = { onNavigate(Routes.Profile.route) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // ─── Logout ──────────────────────────────────────────────
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
        DrawerNavItem(
            icon = Icons.Outlined.Logout,
            label = "Cerrar Sesión",
            isSelected = false,
            tint = Color(0xFFC5221F),
            onClick = onLogout
        )

        // ─── Footer ──────────────────────────────────────────────
        Text(
            text = "ContinuousNEXT — Innovación en cada paso",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tint: Color = if (isSelected) Color(0xFFFF6B00) else Color(0xFF333333)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) Color(0xFFFFF3E8) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = tint
        )
    }
}
