package com.trainingtimer.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.trainingtimer.app.MainActivity
import com.trainingtimer.app.R
import com.trainingtimer.app.data.Reminder
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.describe
import com.trainingtimer.app.data.durationLabel
import com.trainingtimer.app.data.timeLabel
import com.trainingtimer.app.data.title

/** Fires reminders, handles the Snooze button and puts alarms back after a reboot. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Repo.init(context)
        when (intent.action) {
            ACTION_FIRE -> fire(context, intent.getStringExtra(EXTRA_ID) ?: return, intent.getBooleanExtra(EXTRA_SNOOZED, false))
            ACTION_SNOOZE -> {
                val r = find(intent.getStringExtra(EXTRA_ID)) ?: return
                ReminderNotifier.cancel(context, r.id)
                ReminderScheduler.snooze(context, r)
            }
        }
    }

    private fun fire(context: Context, id: String, snoozed: Boolean) {
        val r = find(id) ?: return
        // A snoozed one-time reminder is already switched off but should still ring.
        if (!r.enabled && !snoozed) return
        if (!snoozed) {
            if (r.days.isEmpty()) {
                // A one-time reminder switches itself off once it has fired.
                Repo.saveReminder(r.copy(enabled = false))
            } else {
                ReminderScheduler.schedule(context, r)
            }
        }
        if (r.alarm) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_RING).putExtra(EXTRA_ID, r.id),
                )
                return
            } catch (e: Exception) {
                // Background start refused (e.g. inexact fallback alarm): show a normal notification instead.
            }
        }
        ReminderNotifier.show(context, r)
    }

    private fun find(id: String?): Reminder? = Repo.state.value.reminders.find { it.id == id }

    companion object {
        const val ACTION_FIRE = "com.trainingtimer.app.REMINDER_FIRE"
        const val ACTION_SNOOZE = "com.trainingtimer.app.REMINDER_SNOOZE"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_SNOOZED = "snoozed"
    }
}

/** The quiet ("notification") kind of reminder. */
object ReminderNotifier {
    private const val CHANNEL = "reminders"

    fun notificationId(reminderId: String): Int = 1000 + (reminderId.hashCode() and 0xFFFF)

    fun show(context: Context, r: Reminder) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Workout reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders you set in Settings › Reminders"
            },
        )
        val data = Repo.state.value
        val complex = data.complexes.find { it.id == r.complexId }
        val nid = notificationId(r.id)
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, nid, complexId = null))
        if (complex != null) {
            builder
                .setContentTitle("${complex.name} at ${r.timeLabel()}")
                .setContentText("${complex.describe()} · ${complex.durationLabel()}. Ready when you are.")
                .addAction(0, "Start", openApp(context, nid, complexId = complex.id))
        } else {
            builder
                .setContentTitle(r.title(data.complexes))
                .setContentText("Pick a complex and go.")
                .addAction(0, "Choose workout", openApp(context, nid, complexId = null))
        }
        builder.addAction(0, "Snooze ${r.snoozeMinutes} min", snoozeIntent(context, r.id))
        try {
            nm.notify(nid, builder.build())
        } catch (e: SecurityException) {
            // Notifications are not allowed; the Reminders screen asks for the permission.
        }
    }

    fun cancel(context: Context, reminderId: String) {
        context.getSystemService(NotificationManager::class.java)?.cancel(notificationId(reminderId))
    }

    /** Opens the app; with [complexId] it starts that workout right away. */
    fun openApp(context: Context, notificationId: Int, complexId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(Uri.parse("trainingtimer://open/$notificationId/${complexId ?: "home"}"))
            .putExtra(MainActivity.EXTRA_CANCEL_NOTIFICATION, notificationId)
        if (complexId != null) intent.putExtra(MainActivity.EXTRA_START_COMPLEX, complexId)
        else intent.putExtra(MainActivity.EXTRA_SHOW_COMPLEXES, true)
        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun snoozeIntent(context: Context, reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_SNOOZE)
            .setData(Uri.parse("trainingtimer://snooze/$reminderId"))
            .putExtra(ReminderReceiver.EXTRA_ID, reminderId)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

/** System broadcasts (reboot, app update, clock change) that wipe or shift scheduled alarms. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.rescheduleAll(context)
    }
}
