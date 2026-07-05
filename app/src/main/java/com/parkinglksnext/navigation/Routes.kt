package com.parkinglksnext.navigation

/**
 * Sealed class defining all navigation routes for the app.
 */
sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object Register : Routes("register")
    data object ForgotPassword : Routes("forgot_password")
    data object Home : Routes("home")                    // Home / Dashboard
    data object Dashboard : Routes("dashboard")          // My Reservations
    data object NewReservation : Routes("new_reservation")
    data object History : Routes("history")
    data object Profile : Routes("profile")
    data object EditProfile : Routes("edit_profile")
    data object ChatBot : Routes("chat_bot")

    // Parameterized route: edit a specific reservation by ID
    data object EditReservation : Routes("edit_reservation/{id}") {
        fun createRoute(id: String) = "edit_reservation/$id"
    }
}
