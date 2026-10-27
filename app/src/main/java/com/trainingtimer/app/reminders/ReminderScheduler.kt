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
import com.trainingtimer.app.data.isRepeat
import java.time.Instant
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
        if (r.isRepeat) return nextRepeat(r, now)
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val at = date.atTime(r.hour, r.minute)
            if (!at.isAfter(now)) continue
            if (r.days.isEmpty() || date.dayOfWeek.value in r.days) return at
        }
        return null
    }

    /**
     * Repeat mode: [Reminder.anchor] + step, inside [from, until) on the chosen days (none = every day).
     * Past the end of the window the next allowed day starts at "from"; missed times are skipped, not caught up.
     */
    private fun nextRepeat(r: Reminder, now: LocalDateTime): LocalDateTime? {
        val step = r.repeatMinutes.toLong().coerceAtLeast(1)
        if (r.untilMinute <= r.fromMinute) return null
        // No anchor: rings sit on the From grid (From, From + step, …). An anchor (a ring, the end of a
        // snooze or a workout) moves the step to count from that moment.
        var cand = if (r.anchor <= 0) {
            now.toLocalDate().atStartOfDay().plusMinutes(r.fromMinute.toLong())
        } else {
            val base = Instant.ofEpochMilli(r.anchor).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val baseStart = base.toLocalDate().atStartOfDay().plusMinutes(r.fromMinute.toLong())
            if (base.isBefore(baseStart)) baseStart else base.plusMinutes(step)
        }
        repeat(3000) {
            val date = cand.toLocalDate()
            val start = date.atStartOfDay().plusMinutes(r.fromMinute.toLong())
            val end = date.atStartOfDay().plusMinutes(r.untilMinute.toLong())
            val dayOk = r.days.isEmpty() || date.dayOfWeek.value in r.days
            cand = when {
                !dayOk || !cand.isBefore(end) -> date.plusDays(1).atStartOfDay().plusMinutes(r.fromMinute.toLong())
                cand.isBefore(start) -> start
                !cand.isAfter(now) -> cand.plusMinutes(step)
                else -> return cand
            }
        }
        return null
    }

    /** The times a repeating reminder would ring today (for the editor's preview). */
    fun todayTimes(r: Reminder, now: LocalDateTime = LocalDateTime.now()): List<LocalDateTime> {
        val out = ArrayList<LocalDateTime>()
        var probe = r.copy(enabled = true)
        var t = now
        while (out.size < 48) {
            val next = nextRepeat(probe, t) ?: break
            if (next.toLocalDate() != now.toLocalDate()) break
            out += next
            probe = probe.copy(anchor = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            t = next
        }
        return out
    }

    /** What a repeating reminder does today and on its next day, for the "Show times" list. */
    class DayPlan(
        val past: List<LocalDateTime>,
        val upcoming: List<LocalDateTime>,
        val nextDay: LocalDate?,
        val nextDayTimes: List<LocalDateTime>,
    )

    fun dayPlan(r: Reminder, now: LocalDateTime = LocalDateTime.now()): DayPlan {
        val today = now.toLocalDate()
        val step = r.repeatMinutes.toLong().coerceAtLeast(1)
        fun grid(date: LocalDate, before: LocalDateTime?): List<LocalDateTime> {
            val out = ArrayList<LocalDateTime>()
            var t = date.atStartOfDay().plusMinutes(r.fromMinute.toLong())
            val end = date.atStartOfDay().plusMinutes(r.untilMinute.toLong())
            while (t.isBefore(end) && out.size < 96 && (before == null || t.isBefore(before))) {
                out += t
                t = t.plusMinutes(step)
            }
            return out
        }
        val dayOk = { d: LocalDate -> r.days.isEmpty() || d.dayOfWeek.value in r.days }
        // Times that would already have rung today (on the From grid, up to the last ring / switch-on).
        val anchorAt = if (r.anchor > 0) Instant.ofEpochMilli(r.anchor).atZone(ZoneId.systemDefault()).toLocalDateTime() else now
        val past = if (dayOk(today)) grid(today, minOf(now, anchorAt.plusSeconds(1))) else emptyList()
        val upcoming = ArrayList<LocalDateTime>()
        var probe = r
        var t = now
        while (upcoming.size < 96) {
            val next = nextRepeat(probe, t) ?: break
            if (next.toLocalDate() != today) break
            upcoming += next
            probe = probe.copy(anchor = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), awaitingWorkout = false)
            t = next
        }
        val nextDay = (1..7).map { today.plusDays(it.toLong()) }.firstOrNull { dayOk(it) }
        return DayPlan(past.filter { p -> upcoming.none { it == p } }, upcoming, nextDay, nextDay?.let { grid(it, null) } ?: emptyList())
    }

    /** A repeating reminder rang: the next step counts from now. */
    fun onRepeatFired(context: Context, r: Reminder) {
        // On the From grid it stays on the grid; after a snooze or a workout it keeps counting from the last ring.
        Repo.saveReminder(r.copy(anchor = if (r.anchor <= 0) 0 else System.currentTimeMillis(), awaitingWorkout = false))
    }

    /** A workout was started from reminder [id]: if it repeats, the step restarts when the workout ends. */
    fun startedWorkout(context: Context, id: String?, plannedSeconds: Int) {
        val r = Repo.state.value.reminders.find { it.id == id } ?: return
        if (!r.isRepeat) return
        val am = context.getSystemService(AlarmManager::class.java)
        am?.cancel(fireIntent(context, r.id, snooze = true))
        // Until the workout ends, plan the next one as if it ends on time (in case it never reports back).
        Repo.saveReminder(r.copy(anchor = System.currentTimeMillis() + plannedSeconds * 1000L, awaitingWorkout = true))
    }

    /** The workout ended: reminders waiting for it count their step from now. */
    fun workoutEnded(context: Context) {
        Repo.init(context)
        for (r in Repo.state.value.reminders.filter { it.awaitingWorkout }) {
            Repo.saveReminder(r.copy(anchor = System.currentTimeMillis(), awaitingWorkout = false))
        }
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
        // A repeating reminder waits: its step counts from the end of the snooze (or of the workout, if one starts).
        if (r.isRepeat) Repo.saveReminder(r.copy(anchor = at))
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
