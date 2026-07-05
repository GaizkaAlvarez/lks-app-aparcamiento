package com.parkinglksnext.e2e

import com.parkinglksnext.navigation.Routes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RouteTest {

    @Test fun routes_are_unique() {
        val paths = listOf(
            Routes.Login, Routes.Register, Routes.ForgotPassword,
            Routes.Home, Routes.Dashboard, Routes.NewReservation,
            Routes.History, Routes.Profile, Routes.EditProfile, Routes.ChatBot
        ).map { it.route }
        assertThat(paths.distinct().size).isEqualTo(paths.size)
    }

    @Test fun routes_have_correct_values() {
        assertThat(Routes.Login.route).isEqualTo("login")
        assertThat(Routes.Home.route).isEqualTo("home")
        assertThat(Routes.ChatBot.route).isEqualTo("chat_bot")
        assertThat(Routes.NewReservation.route).isEqualTo("new_reservation")
        assertThat(Routes.Profile.route).isEqualTo("profile")
        assertThat(Routes.Dashboard.route).isEqualTo("dashboard")
        assertThat(Routes.EditProfile.route).isEqualTo("edit_profile")
        assertThat(Routes.Register.route).isEqualTo("register")
        assertThat(Routes.ForgotPassword.route).isEqualTo("forgot_password")
        assertThat(Routes.History.route).isEqualTo("history")
    }

    @Test fun editReservation_parameterized_route() {
        val route = Routes.EditReservation.createRoute("res-123")
        assertThat(route).isEqualTo("edit_reservation/res-123")
        assertThat(route).contains("res-123")
    }
}
