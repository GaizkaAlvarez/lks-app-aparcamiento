package com.parkinglksnext

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime

class ParkingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.parking_widget)

        // Click opens the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("reservations")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener { snapshot ->
                    val now = LocalTime.now()
                    val today = LocalDate.now()

                    val upcoming = snapshot.documents
                        .mapNotNull { it.toObject(Reservation::class.java)?.copy(id = it.id) }
                        .filter { r ->
                            try {
                                val d = LocalDate.parse(r.date)
                                val endMin = r.endTime.split(":").let {
                                    it[0].toInt() * 60 + it[1].toInt()
                                }
                                d > today || (d == today && endMin > now.hour * 60 + now.minute)
                            } catch (_: Exception) { false }
                        }
                        .minByOrNull { "${it.date}T${it.startTime}" }

                    if (upcoming != null) {
                        val emoji = when (upcoming.spotType) {
                            "electric" -> "⚡"
                            "motorcycle" -> "🏍"
                            else -> "🚗"
                        }
                        views.setTextViewText(
                            R.id.widget_info,
                            "${upcoming.date} · ${upcoming.startTime} – ${upcoming.endTime}"
                        )
                        views.setTextViewText(R.id.widget_spot, "${upcoming.spotNumber} $emoji")
                    } else {
                        views.setTextViewText(R.id.widget_info, "Sin reservas próximas")
                        views.setTextViewText(R.id.widget_spot, "—")
                    }
                    manager.updateAppWidget(widgetId, views)
                }
        } else {
            views.setTextViewText(R.id.widget_info, "Inicia sesión en la app")
            manager.updateAppWidget(widgetId, views)
        }
    }

    companion object {
        /** Call from app to force an immediate widget refresh. */
        fun notifyDataChanged(context: Context) {
            val intent = Intent(context, ParkingWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, ParkingWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}
