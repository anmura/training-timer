package com.trainingtimer.app.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.trainingtimer.app.R
import com.trainingtimer.app.data.Reminder
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.timeLabel
import com.trainingtimer.app.data.title
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Rings an alarm reminder: loud sound, vibration and a full-screen alarm screen, until the user reacts. */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopRinging(snoozeAfter = true) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Repo.init(this)
        when (intent?.action) {
            ACTION_RING -> {
                val r = Repo.state.value.reminders.find { it.id == intent.getStringExtra(ReminderReceiver.EXTRA_ID) }
                if (r == null) {
                    stopSelf()
                } else {
                    ring(r)
                }
            }
            ACTION_SNOOZE -> stopRinging(snoozeAfter = true)
            else -> stopRinging(snoozeAfter = false)
        }
        return START_NOT_STICKY
    }

    private fun ring(r: Reminder) {
        stopSound()
        _ringing.value = r
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(r), type)
        startSound(r)
        startVibration()
        handler.removeCallbacks(timeout)
        // Nobody reacted for 5 minutes: stop and try again after the snooze time.
        handler.postDelayed(timeout, 5 * 60_000L)
    }

    private fun stopRinging(snoozeAfter: Boolean) {
        val r = _ringing.value
        if (snoozeAfter && r != null) ReminderScheduler.snooze(this, r)
        handler.removeCallbacks(timeout)
        stopSound()
        _ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        stopSound()
        _ringing.value = null
        super.onDestroy()
    }

    private fun startSound(r: Reminder) {
        val uri = r.sound?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        try {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
        } catch (e: Exception) {
            // Vibration is optional.
        }
    }

    private fun stopSound() {
        try {
            player?.stop()
        } catch (e: Exception) {
            // Already stopped.
        }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun buildNotification(r: Reminder): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(CHANNEL, "Workout alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alarm reminders that ring full screen"
                setSound(null, null) // the service plays the alarm sound itself
                enableVibration(false)
            },
        )
        val full = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = r.title(Repo.state.value.complexes)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("$title · ${r.timeLabel()}")
            .setContentText("Workout alarm")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .addAction(0, "Snooze ${r.snoozeMinutes} min", serviceIntent(this, ACTION_SNOOZE, 11))
            .addAction(0, "Dismiss", serviceIntent(this, ACTION_DISMISS, 12))
            .build()
    }

    companion object {
        const val ACTION_RING = "com.trainingtimer.app.ALARM_RING"
        const val ACTION_SNOOZE = "com.trainingtimer.app.ALARM_SNOOZE"
        const val ACTION_DISMISS = "com.trainingtimer.app.ALARM_DISMISS"
        private const val CHANNEL = "alarms"
        private const val NOTIFICATION_ID = 77

        private val _ringing = MutableStateFlow<Reminder?>(null)

        /** The reminder that is ringing right now, if any. */
        val ringing: StateFlow<Reminder?> = _ringing

        fun snooze(context: Context) = context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE))

        fun dismiss(context: Context) = context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS))

        private fun serviceIntent(context: Context, action: String, code: Int): PendingIntent = PendingIntent.getService(
            context,
            code,
            Intent(context, AlarmService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
