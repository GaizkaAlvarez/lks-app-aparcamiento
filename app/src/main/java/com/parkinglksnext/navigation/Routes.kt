package com.parkinglksnext.navigation

/**
 * Sealed class defining all navigation routes for the app.
 * Matches the Figma reference exactly: 9 screens.
 */
sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object Register : Routes("register")
    data object ForgotPassword : Routes("forgot_password")
    data object Dashboard : Routes("dashboard")          // Active Reservations
    data object NewReservation : Routes("new_reservation")
    data object History : Routes("history")
    data object Profile : Routes("profile")
    data object EditProfile : Routes("edit_profile")

    // Parameterized route: edit a specific reservation by ID
    data object EditReservation : Routes("edit_reservation/{id}") {
        fun createRoute(id: String) = "edit_reservation/$id"
    }
}
