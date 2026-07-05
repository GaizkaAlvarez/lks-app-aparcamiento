package com.parkinglksnext.models

import com.google.common.truth.Truth.assertThat
import com.parkinglksnext.*
import org.junit.Test

class ModelsTest {

    @Test fun vehicle_default_values_correct() {
        val v = Vehicle()
        assertThat(v.id).isEmpty()
        assertThat(v.licensePlate).isEmpty()
        assertThat(v.type).isEqualTo("comun")
    }

    @Test fun vehicle_holds_data() {
        val v = Vehicle(id = "v1", licensePlate = "1234ABC", type = "electric")
        assertThat(v.id).isEqualTo("v1")
        assertThat(v.licensePlate).isEqualTo("1234ABC")
        assertThat(v.type).isEqualTo("electric")
    }

    @Test fun userProfile_defaults() {
        val p = UserProfile()
        assertThat(p.notificationSettings.startReminder).isTrue()
        assertThat(p.notificationSettings.expiringReminder).isTrue()
        assertThat(p.firstName).isEmpty()
        assertThat(p.vehicles).isEmpty()
    }

    @Test fun reservation_defaults() {
        val r = Reservation()
        assertThat(r.status).isEqualTo("active")
        assertThat(r.spotNumber).isEqualTo(0)
    }

    @Test fun reservation_holds_data() {
        val r = Reservation(id = "res1", spotNumber = 3, date = "2026-07-15", status = "active")
        assertThat(r.spotNumber).isEqualTo(3)
        assertThat(r.date).isEqualTo("2026-07-15")
    }

    @Test fun parkingSpot_defaults() {
        val s = ParkingSpot()
        assertThat(s.available).isTrue()
    }

    @Test fun parkingSpot_types() {
        assertThat(ParkingSpot(type = "comun").type).isEqualTo("comun")
        assertThat(ParkingSpot(type = "electric").type).isEqualTo("electric")
        assertThat(ParkingSpot(type = "motorcycle").type).isEqualTo("motorcycle")
    }

    @Test fun parkingSpot_default_type() {
        assertThat(ParkingSpot().type).isEqualTo("comun")
    }

    @Test fun reservation_has_vehiclePlate() {
        val r = Reservation(vehiclePlate = "1234ABC")
        assertThat(r.vehiclePlate).isEqualTo("1234ABC")
    }

    @Test fun reservation_vehiclePlate_default_empty() {
        assertThat(Reservation().vehiclePlate).isEmpty()
    }

    @Test fun notificationSettings() {
        val ns = NotificationSettings(startReminder = false, expiringReminder = false)
        assertThat(ns.startReminder).isFalse()
        assertThat(ns.expiringReminder).isFalse()
    }

    @Test fun notificationSettings_default_minutes() {
        val ns = NotificationSettings()
        assertThat(ns.startReminderMinutes).isEqualTo(15)
        assertThat(ns.expiringReminderMinutes).isEqualTo(15)
    }
}
