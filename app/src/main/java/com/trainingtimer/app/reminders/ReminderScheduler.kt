package com.trainingtimer.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.trainingtimer.app.MainActivity
import com.trainingtimer.app.data.Reminder
import com.trainingtimer.app.data.Repo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Puts reminders into the system AlarmManager so they fire even when the app is closed. */
object ReminderScheduler {

    /** Next time [r] should fire after [now], or null if it is switched off. */
    fun nextTrigger(r: Reminder, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!r.enabled) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val at = date.atTime(r.hour, r.minute)
            if (!at.isAfter(now)) continue
            if (r.days.isEmpty() || date.dayOfWeek.value in r.days) return at
        }
        return null
    }

    /** "today 07:30", "tomorrow 07:30", "Mon 07:30". */
    fun whenLabel(at: LocalDateTime, today: LocalDate = LocalDate.now()): String {
        val time = "%02d:%02d".format(at.hour, at.minute)
        return when (at.toLocalDate()) {
            today -> "today $time"
            today.plusDays(1) -> "tomorrow $time"
            else -> at.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + time
        }
    }

    /** The soonest upcoming reminder among [list], with its time. */
    fun nextOf(list: List<Reminder>): Pair<Reminder, LocalDateTime>? =
        list.mapNotNull { r -> nextTrigger(r)?.let { r to it } }.minByOrNull { it.second }

    fun schedule(context: Context, r: Reminder) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = fireIntent(context, r.id, snooze = false)
        am.cancel(pi)
        // Switching a reminder off also drops a pending snooze. (A one-time reminder switches itself off
        // right when it fires; a snooze set after that is scheduled later and stays.)
        if (!r.enabled) am.cancel(fireIntent(context, r.id, snooze = true))
        val at = nextTrigger(r) ?: return
        setAlarm(context, am, toMillis(at), pi, r.alarm)
    }

    fun cancel(context: Context, id: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(fireIntent(context, id, snooze = false))
        am.cancel(fireIntent(context, id, snooze = true))
    }

    fun snooze(context: Context, r: Reminder) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + r.snoozeMinutes * 60_000L
        setAlarm(context, am, at, fireIntent(context, r.id, snooze = true), r.alarm)
    }

    /** After a reboot, an app update or a clock change every alarm has to be set again. */
    fun rescheduleAll(context: Context) {
        Repo.init(context)
        for (r in Repo.state.value.reminders) schedule(context, r)
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }

    private fun setAlarm(context: Context, am: AlarmManager, atMillis: Long, pi: PendingIntent, alarm: Boolean) {
        try {
            if (!canScheduleExact(context)) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            } else if (alarm) {
                // Shows the alarm icon in the status bar and is never delayed by battery saving.
                am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, openAppIntent(context)), pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun toMillis(at: LocalDateTime): Long = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun fireIntent(context: Context, id: String, snooze: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            // The data URI makes each reminder's intent distinct, so they don't replace each other.
            .setData(Uri.parse("trainingtimer://reminder/$id" + if (snooze) "/snooze" else ""))
            .putExtra(ReminderReceiver.EXTRA_ID, id)
            .putExtra(ReminderReceiver.EXTRA_SNOOZED, snooze)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
