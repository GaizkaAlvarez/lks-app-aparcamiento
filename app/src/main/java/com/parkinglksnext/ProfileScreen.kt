package com.parkinglksnext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- AVATAR CIRCULAR CON INICIALES ---
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GA", // Iniciales simuladas (ej: Gaizka Alvarez)
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Gaizka Álvarez",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2537)
            )
            Text(
                text = "Empleado LKS Next",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- BLOQUE DE INFORMACIÓN (CAMPOS DE DETALLE) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FilaDetallePerfil(icon = Icons.Outlined.Person, titulo = "Nombre Completo", valor = "Gaizka Álvarez")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))

                    FilaDetallePerfil(icon = Icons.Outlined.Email, titulo = "Correo Corporativo", valor = "gaizka@lksnext.com")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))

                    FilaDetallePerfil(
                        icon = Icons.Outlined.DirectionsCar,
                        titulo = "Vehículo Principal",
                        valor = "1234ABC (Eléctrico)" // Indicamos el tipo directamente
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Empuja los botones hacia el final de la pantalla

            // --- BOTÓN EDICIÓN ---
            Button(
                onClick = { onNavigateToEditProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Editar Perfil", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- BOTÓN CERRAR SESIÓN ---
            TextButton(
                onClick = { /* Próximamente: auth.signOut() */ },
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text("Cerrar Sesión", color = Color(0xFFC5221F), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// Componente auxiliar para maquetar cada fila del perfil de forma elegante
@Composable
fun FilaDetallePerfil(icon: ImageVector, titulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF6C757D), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = titulo, fontSize = 12.sp, color = Color.Gray)
            Text(text = valor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F2537))
        }
    }
}