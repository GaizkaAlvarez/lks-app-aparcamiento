package com.parkinglksnext.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.parkinglksnext.MainActivity
import com.parkinglksnext.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Schedules and displays local push notifications for parking reservations.
 */
object NotificationHelper {

    const val CHANNEL_ID = "parking_reminders"
    const val CHANNEL_NAME = "Recordatorios de Parking"

    /**
     * Create the notification channel. Call once in Application.onCreate or MainActivity.
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de inicio y fin de reserva"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedule a reminder notification 15 minutes before the reservation start time.
     */
    fun scheduleStartReminder(
        context: Context,
        reservationId: String,
        date: String,        // yyyy-MM-dd
        startTime: String,   // HH:mm
        spotNumber: Int
    ) {
        val triggerTime = parseToZonedDateTime(date, startTime).minusMinutes(15)
        val now = ZonedDateTime.now()

        if (triggerTime.isBefore(now)) return  // don't schedule past reminders

        val delayMs = java.time.Duration.between(now, triggerTime).toMillis()

        val title = "Tu reserva empieza pronto"
        val body = "La plaza $spotNumber te espera en 15 minutos. ¡No llegues tarde!"

        scheduleWorker(context, reservationId, delayMs, title, body)
    }

    /**
     * Schedule an expiry reminder 15 minutes before the reservation end time.
     */
    fun scheduleExpiryReminder(
        context: Context,
        reservationId: String,
        date: String,
        endTime: String,
        spotNumber: Int
    ) {
        val triggerTime = parseToZonedDateTime(date, endTime).minusMinutes(15)
        val now = ZonedDateTime.now()

        if (triggerTime.isBefore(now)) return

        val delayMs = java.time.Duration.between(now, triggerTime).toMillis()

        val title = "Tu reserva está por terminar"
        val body = "La plaza $spotNumber expira en 15 minutos. ¿Necesitas más tiempo?"

        scheduleWorker(context, "${reservationId}_expiry", delayMs, title, body)
    }

    /**
     * Show an immediate notification (for confirmation).
     */
    fun showImmediate(context: Context, title: String, body: String, id: Int = 1) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    // ── Internals ────────────────────────────────────────────────

    private fun parseToZonedDateTime(date: String, time: String): ZonedDateTime {
        val localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        return ZonedDateTime.of(localDate, localTime, ZoneId.systemDefault())
    }

    private fun scheduleWorker(
        context: Context,
        reservationId: String,
        delayMs: Long,
        title: String,
        body: String
    ) {
        val inputData = Data.Builder()
            .putString("title", title)
            .putString("body", body)
            .putString("reservationId", reservationId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(reservationId)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Cancel scheduled reminders for a specific reservation.
     */
    fun cancelReminders(context: Context, reservationId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(reservationId)
        WorkManager.getInstance(context).cancelAllWorkByTag("${reservationId}_expiry")
    }

    /**
     * Worker that shows the notification when triggered.
     */
    class ReminderWorker(
        context: Context,
        params: WorkerParameters
    ) : Worker(context, params) {

        override fun doWork(): Result {
            val title = inputData.getString("title") ?: "Recordatorio"
            val body = inputData.getString("body") ?: ""
            val reservationId = inputData.getString("reservationId") ?: ""

            showImmediate(applicationContext, title, body, reservationId.hashCode())

            return Result.success()
        }
    }
}
