package com.parkinglksnext.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.parkinglksnext.*
import org.junit.Before
import org.junit.Test

class ViewModelIntegrationTest {

    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    // ── NewReservationViewModel ──────────────────────────────────

    @Test fun newReservation_initial_state_is_step1() {
        val vm = NewReservationViewModel(application)
        assertThat(vm.uiState.value.step).isEqualTo(1)
        assertThat(vm.uiState.value.selectedVehicle).isNull()
        assertThat(vm.uiState.value.selectedSpot).isNull()
    }

    @Test fun newReservation_setDate_updates_state() {
        val vm = NewReservationViewModel(application)
        vm.setDate("2026-07-15")
        assertThat(vm.uiState.value.selectedDate).isEqualTo("2026-07-15")
    }

    @Test fun newReservation_setStartTime_updates_state() {
        val vm = NewReservationViewModel(application)
        vm.setStartTime("10:00")
        assertThat(vm.uiState.value.startTime).isEqualTo("10:00")
    }

    @Test fun newReservation_setEndTime_updates_state() {
        val vm = NewReservationViewModel(application)
        vm.setEndTime("14:00")
        assertThat(vm.uiState.value.endTime).isEqualTo("14:00")
    }

    @Test fun newReservation_setVehicle_updates_state() {
        val vm = NewReservationViewModel(application)
        val v = Vehicle(id = "v1", licensePlate = "1234ABC", type = "combustion")
        vm.setVehicle(v)
        assertThat(vm.uiState.value.selectedVehicle?.licensePlate).isEqualTo("1234ABC")
    }

    @Test fun newReservation_selectSpot_updates_state() {
        val vm = NewReservationViewModel(application)
        val spot = ParkingSpot(id = "spot-3", number = 3, type = "combustion")
        vm.selectSpot(spot)
        assertThat(vm.uiState.value.selectedSpot?.number).isEqualTo(3)
    }

    @Test fun newReservation_resetState_clears_all() {
        val vm = NewReservationViewModel(application)
        vm.setDate("2026-07-15")
        vm.setStartTime("10:00")
        vm.resetState()
        assertThat(vm.uiState.value.selectedDate).isNull()
        assertThat(vm.uiState.value.startTime).isNull()
        assertThat(vm.uiState.value.selectedVehicle).isNull()
    }

    @Test fun newReservation_clearError_does_not_crash() {
        val vm = NewReservationViewModel(application)
        vm.clearError()
    }

    // ── ReservationsViewModel ────────────────────────────────────

    @Test fun reservations_initial_state_empty() {
        val vm = ReservationsViewModel(application)
        assertThat(vm.uiState.value.currentReservations).isEmpty()
        assertThat(vm.uiState.value.futureReservations).isEmpty()
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    @Test fun reservations_clearError_does_not_crash() {
        val vm = ReservationsViewModel(application)
        vm.clearError()
    }

    // ── ProfileViewModel ─────────────────────────────────────────

    @Test fun profile_initial_state_null() {
        val vm = ProfileViewModel()
        assertThat(vm.uiState.value.userProfile).isNull()
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    @Test fun profile_clearError_does_not_crash() {
        val vm = ProfileViewModel()
        vm.clearError()
    }

    // ── Vehicle types (Spanish labels) ───────────────────────────

    @Test fun vehicleTypeLabel_combustion() {
        val v = Vehicle(type = "combustion")
        assertThat(v.type).isEqualTo("combustion")
    }

    @Test fun vehicleTypeLabel_electric() {
        val v = Vehicle(type = "electric")
        assertThat(v.type).isEqualTo("electric")
    }

    @Test fun vehicleTypeLabel_motorcycle() {
        val v = Vehicle(type = "motorcycle")
        assertThat(v.type).isEqualTo("motorcycle")
    }
}
