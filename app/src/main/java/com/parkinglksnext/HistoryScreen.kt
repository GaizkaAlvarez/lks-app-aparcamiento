package com.parkinglksnext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Clase de datos auxiliar para simular el historial antes de conectar la BD
data class ReservaPasada(
    val id: String,
    val fecha: String,
    val horas: String,
    val matricula: String,
    val completada: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    // Simulamos un listado de datos históricos
    val historialMock = remember {
        listOf(
            ReservaPasada("1", "Jueves, 4 de Junio", "08:00 - 17:00", "1234ABC", true),
            ReservaPasada("2", "Martes, 2 de Junio", "09:30 - 14:00", "5678XYZ", true),
            ReservaPasada("3", "Lunes, 25 de Mayo", "08:00 - 18:30", "1234ABC", true),
            ReservaPasada("4", "Miércoles, 20 de Mayo", "07:00 - 15:00", "1234ABC", false) // Cancelada
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Historial", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tus reservas pasadas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2537),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // LazyColumn es el equivalente pro al RecyclerView. Solo pinta lo que se ve.
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(historialMock) { reserva ->
                    TarjetaHistorial(reserva = reserva)
                }
            }
        }
    }
}

@Composable
fun TarjetaHistorial(reserva: ReservaPasada) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA) // Gris suave corporativo
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = reserva.fecha,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F2537)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Horario: ${reserva.horas}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Vehículo: ${reserva.matricula}",
                    fontSize = 14.sp,
                    color = Color(0xFF1E3A8A), // Azul oscuro LKS
                    fontWeight = FontWeight.Medium
                )
            }

            // Etiqueta de Estado (Completada verde / Cancelada roja)
            Box(
                modifier = Modifier
                    .background(
                        color = if (reserva.completada) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (reserva.completada) "Completada" else "Cancelada",
                    color = if (reserva.completada) Color(0xFF137333) else Color(0xFFC5221F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}