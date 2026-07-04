package com.parkinglksnext

// Modelo de Vehículo compatible con la estructura que tienes en TypeScript
data class Vehicle(
    val id: String = "",
    val name: String = "",       // nombre descriptivo (ej: "Mi Tesla")
    val licensePlate: String = "",
    val type: String = "comun"   // "comun", "electric", "motorcycle"
)

// Modelo de Notificaciones de perfil
data class NotificationSettings(
    val startReminder: Boolean = true,
    val expiringReminder: Boolean = true
)

// Modelo de Usuario para Firestore
data class UserProfile(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val name: String = "",
    val email: String = "",
    val licensePlate: String = "",
    val vehicleType: String = "comun",
    val vehicles: List<Vehicle> = emptyList(),
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val profileImageBase64: String = ""
)

// Modelo de Reserva para Firestore
data class Reservation(
    val id: String = "",
    val userId: String = "",
    val vehicleId: String = "",
    val spotId: String = "",
    val spotNumber: Int = 0,
    val spotType: String = "comun",
    val date: String = "", // Formato "yyyy-MM-dd"
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "active", // "active", "completed", "cancelled"
    val createdAt: String = ""
)

// Modelo de Plaza de Parking
data class ParkingSpot(
    val id: String = "",
    val number: Int = 0,
    val type: String = "comun",
    val available: Boolean = true
)