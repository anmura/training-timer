package com.trainingtimer.app.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.trainingtimer.app.MainActivity
import com.trainingtimer.app.R
import com.trainingtimer.app.data.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Keeps the workout running with the screen off and shows controls in the notification. */
class WorkoutService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var inForeground = false

    private data class NotificationKey(val index: Int, val seconds: Int, val paused: Boolean, val finished: Boolean)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TrainingTimer:workout").apply {
            setReferenceCounted(false)
            acquire(4 * 60 * 60 * 1000L)
        }
        scope.launch {
            WorkoutEngine.state
                .map { s -> s?.let { NotificationKey(it.index, it.remainingSec, it.paused, it.finished) } }
                .distinctUntilChanged()
                .collect { key ->
                    val s = WorkoutEngine.state.value
                    if (key == null || s == null || s.finished) {
                        shutDown()
                    } else if (inForeground) {
                        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(s))
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!inForeground) {
            val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(WorkoutEngine.state.value), type)
            inForeground = true
        }
        when (intent?.action) {
            ACTION_TOGGLE -> WorkoutEngine.togglePause()
            ACTION_SKIP -> WorkoutEngine.skip()
            ACTION_STOP -> WorkoutEngine.stop()
        }
        if (WorkoutEngine.state.value == null) shutDown()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    private fun shutDown() {
        wakeLock?.let { if (it.isHeld) it.release() }
        if (inForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            inForeground = false
        }
        stopSelf()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Workout timer", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows the running workout with pause and skip controls"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(s: WorkoutState?): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp)

        if (s == null) {
            return builder.setContentTitle("Workout").setContentText("Starting…").build()
        }
        val p = s.phase
        val time = formatTime(s.remainingSec)
        val title = when (p.type) {
            PhaseType.WORK -> "${p.exercise.name} · $time"
            PhaseType.REST -> "Rest · $time"
        }
        val detail = when (p.type) {
            PhaseType.WORK -> {
                val round = if (p.rounds > 0) " · round ${p.round}/${p.rounds}" else ""
                "Exercise ${p.workNumber} of ${s.totalWork}$round"
            }
            PhaseType.REST -> "Next: ${p.exercise.name}"
        }
        return builder
            .setContentTitle(if (s.paused) "Paused — $title" else title)
            .setContentText("$detail · ${s.complexName}")
            .addAction(0, if (s.paused) "Resume" else "Pause", actionIntent(ACTION_TOGGLE, 1))
            .addAction(0, "Skip", actionIntent(ACTION_SKIP, 2))
            .addAction(0, "End", actionIntent(ACTION_STOP, 3))
            .build()
    }

    private fun actionIntent(action: String, code: Int): PendingIntent = PendingIntent.getService(
        this,
        code,
        Intent(this, WorkoutService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val CHANNEL_ID = "workout"
        private const val NOTIFICATION_ID = 42
        const val ACTION_TOGGLE = "com.trainingtimer.app.TOGGLE"
        const val ACTION_SKIP = "com.trainingtimer.app.SKIP"
        const val ACTION_STOP = "com.trainingtimer.app.STOP"
    }
}
