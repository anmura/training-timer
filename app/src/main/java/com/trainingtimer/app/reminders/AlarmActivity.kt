package com.trainingtimer.app.reminders

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainingtimer.app.MainActivity
import com.trainingtimer.app.data.Complex
import com.trainingtimer.app.data.ExerciseIcons
import com.trainingtimer.app.data.Reminder
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.daysLabel
import com.trainingtimer.app.data.describe
import com.trainingtimer.app.data.durationLabel
import com.trainingtimer.app.data.exerciseIds
import com.trainingtimer.app.data.timeLabel
import com.trainingtimer.app.data.title
import com.trainingtimer.app.ui.AppTheme
import com.trainingtimer.app.ui.BigButton
import com.trainingtimer.app.ui.C
import com.trainingtimer.app.ui.Condensed
import com.trainingtimer.app.ui.ExerciseIcon
import com.trainingtimer.app.ui.FitText
import com.trainingtimer.app.ui.PathIcon

/** Full-screen alarm shown over the lock screen while [AlarmService] rings. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            AppTheme {
                val ringing by AlarmService.ringing.collectAsStateWithLifecycle()
                val data by Repo.state.collectAsStateWithLifecycle()
                val r = ringing
                // The alarm was stopped from the notification or timed out.
                LaunchedEffect(r == null) { if (r == null) finish() }
                BackHandler { }
                if (r != null) {
                    AlarmScreen(
                        r = r,
                        complex = data.complexes.find { it.id == r.complexId },
                        title = r.title(data.complexes),
                        firstIcon = data.complexes.find { it.id == r.complexId }?.exerciseIds()?.firstOrNull()
                            ?.let { id -> data.exercises.find { it.id == id }?.icon },
                        onStart = { start(r) },
                        onSnooze = {
                            AlarmService.snooze(this)
                            finish()
                        },
                        onDismiss = {
                            AlarmService.dismiss(this)
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun start(r: Reminder) {
        AlarmService.dismiss(this)
        val open = {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (r.complexId != null && Repo.state.value.complexes.any { it.id == r.complexId }) {
                intent.putExtra(MainActivity.EXTRA_START_COMPLEX, r.complexId)
            } else {
                intent.putExtra(MainActivity.EXTRA_SHOW_COMPLEXES, true)
            }
            startActivity(intent)
            finish()
        }
        val km = getSystemService(KeyguardManager::class.java)
        if (km != null && km.isKeyguardLocked) {
            km.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() = open()
                    override fun onDismissCancelled() = finish()
                    override fun onDismissError() = finish()
                },
            )
        } else {
            open()
        }
    }
}

@Composable
private fun AlarmScreen(
    r: Reminder,
    complex: Complex?,
    title: String,
    firstIcon: String?,
    onStart: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "ring",
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(C.RestBg)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(C.RestSurface2)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PathIcon(UiIcons.ALARM, C.Orange, size = 16.dp, strokeWidth = 2.2f)
            Spacer(Modifier.width(6.dp))
            Text("WORKOUT ALARM", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = C.Orange)
        }
        Spacer(Modifier.height(14.dp))
        Text(r.timeLabel(), fontFamily = Condensed, fontSize = 104.sp, color = C.Text)
        Text(r.daysLabel(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.RestMuted)

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(220.dp)
                    .scale(pulse)
                    .border(2.dp, C.RestLine, CircleShape),
            )
            Box(
                Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(C.RestSurface2),
                contentAlignment = Alignment.Center,
            ) {
                if (firstIcon != null) {
                    ExerciseIcon(firstIcon, tileSize = 150.dp, iconSize = 72.dp, tile = C.RestSurface2, corner = 75.dp, strokeWidth = 1.4f)
                } else {
                    PathIcon(UiIcons.ALARM, C.Lime, size = 72.dp, strokeWidth = 1.4f)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(C.RestSurface)
                .border(1.5.dp, C.RestLine, RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("TIME TO TRAIN", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = C.Orange)
                FitText(title, maxFontSize = 32.sp, minFontSize = 18.sp, maxLines = 2, fontFamily = Condensed)
                if (complex != null) {
                    Text(complex.describe(), fontSize = 13.sp, color = C.RestMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (complex != null) {
                Spacer(Modifier.width(8.dp))
                Text(complex.durationLabel().substringBefore(" "), fontFamily = Condensed, fontSize = 28.sp, color = C.Text)
            }
        }
        Spacer(Modifier.height(18.dp))
        BigButton(
            if (complex != null) "Start workout" else "Choose workout",
            onClick = onStart,
            icon = UiIcons.PLAY,
            iconFilled = true,
            height = 72.dp,
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(
                "Snooze ${r.snoozeMinutes} min",
                onClick = onSnooze,
                container = C.RestSurface2,
                content = C.Text,
                modifier = Modifier.weight(1f),
            )
            BigButton(
                "Dismiss",
                onClick = onDismiss,
                container = C.RestSurface2,
                content = C.Text,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
