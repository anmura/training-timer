package com.trainingtimer.app.workout

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.trainingtimer.app.data.Complex
import com.trainingtimer.app.data.flatten
import com.trainingtimer.app.data.Exercise
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.totalSeconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PhaseType { WORK, REST }

/**
 * One interval of the workout. For REST, [exercise] is the exercise that comes next.
 * [workNumber] is the 1-based number of the (upcoming) exercise; [round]/[rounds] are 0 outside cycles.
 */
data class Phase(
    val type: PhaseType,
    val exercise: Exercise,
    val seconds: Int,
    val workNumber: Int,
    val round: Int,
    val rounds: Int,
)

data class WorkoutState(
    val complexName: String,
    val phases: List<Phase>,
    val index: Int,
    val remainingMs: Long,
    val paused: Boolean,
    val finished: Boolean,
    val totalWork: Int,
    val plannedSeconds: Int,
) {
    val phase: Phase get() = phases[index.coerceIn(0, phases.lastIndex)]
    val remainingSec: Int get() = ((remainingMs + 999) / 1000).toInt()
    val totalLeftSec: Int get() = remainingSec + phases.drop(index + 1).sumOf { it.seconds }

    /** Next exercise phase after the current one. */
    val nextWork: Phase? get() = phases.drop(index + 1).firstOrNull { it.type == PhaseType.WORK }

    /** True if a rest comes right after the current phase. */
    val restIsNext: Boolean get() = phases.getOrNull(index + 1)?.type == PhaseType.REST
}

fun buildPhases(complex: Complex, exercises: List<Exercise>): List<Phase> {
    val byId = exercises.associateBy { it.id }
    val sets = complex.flatten().mapNotNull { f -> byId[f.exerciseId]?.let { ex -> f to ex } }
    val out = ArrayList<Phase>()
    sets.forEachIndexed { i, (f, ex) ->
        out += Phase(PhaseType.WORK, ex, f.seconds, i + 1, f.round, f.rounds)
        // No rest after the last exercise.
        if (i < sets.lastIndex && f.restAfter > 0) {
            val (next, nextEx) = sets[i + 1]
            out += Phase(PhaseType.REST, nextEx, f.restAfter, i + 2, next.round, next.rounds)
        }
    }
    return out
}

/** Runs the workout clock. Lives as long as the app process; the service keeps the process alive. */
object WorkoutEngine {
    private val _state = MutableStateFlow<WorkoutState?>(null)
    val state: StateFlow<WorkoutState?> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appContext: Context? = null
    private var tickJob: Job? = null
    private var endAt = 0L
    private var lastBeepSecond = -1
    private var tone: ToneGenerator? = null

    fun start(context: Context, complex: Complex, exercises: List<Exercise>) {
        val phases = buildPhases(complex, exercises)
        if (phases.isEmpty()) return
        val ctx = context.applicationContext
        appContext = ctx
        stopTicking()
        val first = phases[0]
        _state.value = WorkoutState(
            complexName = complex.name,
            phases = phases,
            index = 0,
            remainingMs = first.seconds * 1000L,
            paused = false,
            finished = false,
            totalWork = phases.count { it.type == PhaseType.WORK },
            plannedSeconds = complex.totalSeconds(),
        )
        endAt = now() + first.seconds * 1000L
        lastBeepSecond = -1
        signalPhaseStart(first)
        startTicking()
        ContextCompat.startForegroundService(ctx, Intent(ctx, WorkoutService::class.java))
    }

    fun togglePause() {
        val s = _state.value ?: return
        if (s.finished) return
        if (s.paused) {
            endAt = now() + s.remainingMs
            _state.value = s.copy(paused = false)
        } else {
            val rem = (endAt - now()).coerceAtLeast(0)
            _state.value = s.copy(paused = true, remainingMs = rem)
        }
    }

    fun skip() {
        val s = _state.value ?: return
        if (s.finished) return
        goTo(s.index + 1)
    }

    fun previous() {
        val s = _state.value ?: return
        if (s.finished) return
        val elapsed = s.phase.seconds * 1000L - s.remainingMs
        if (elapsed > 3000 || s.index == 0) goTo(s.index) else goTo(s.index - 1)
    }

    fun addTime(seconds: Int) {
        val s = _state.value ?: return
        if (s.finished) return
        lastBeepSecond = -1
        if (s.paused) {
            _state.value = s.copy(remainingMs = s.remainingMs + seconds * 1000L)
        } else {
            endAt += seconds * 1000L
            _state.value = s.copy(remainingMs = endAt - now())
        }
    }

    fun stop() {
        stopTicking()
        _state.value = null
        tone?.release()
        tone = null
    }

    // ---------- internals ----------

    private fun now() = SystemClock.elapsedRealtime()

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                tick()
                delay(100)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun tick() {
        val s = _state.value ?: return
        if (s.paused || s.finished) return
        val rem = endAt - now()
        if (rem <= 0) {
            goTo(s.index + 1)
            return
        }
        val sec = ((rem + 999) / 1000).toInt()
        if (sec in 1..3 && sec != lastBeepSecond) {
            lastBeepSecond = sec
            countdownBeep()
        }
        _state.value = s.copy(remainingMs = rem)
    }

    private fun goTo(index: Int) {
        val s = _state.value ?: return
        if (index > s.phases.lastIndex) {
            finish()
            return
        }
        val i = index.coerceAtLeast(0)
        val p = s.phases[i]
        endAt = now() + p.seconds * 1000L
        lastBeepSecond = -1
        _state.value = s.copy(index = i, remainingMs = p.seconds * 1000L)
        signalPhaseStart(p)
    }

    private fun finish() {
        val s = _state.value ?: return
        stopTicking()
        _state.value = s.copy(finished = true, paused = false, remainingMs = 0)
        val settings = Repo.state.value.settings
        if (settings.beeps) toneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 900)
        if (settings.vibration) vibrate(longArrayOf(0, 400, 150, 400, 150, 600))
    }

    private fun countdownBeep() {
        if (Repo.state.value.settings.beeps) toneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    private fun signalPhaseStart(p: Phase) {
        val settings = Repo.state.value.settings
        if (settings.beeps) {
            toneGenerator()?.startTone(
                if (p.type == PhaseType.WORK) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_ACK,
                400,
            )
        }
        if (settings.vibration) {
            vibrate(if (p.type == PhaseType.WORK) longArrayOf(0, 350) else longArrayOf(0, 150, 120, 150))
        }
    }

    private fun toneGenerator(): ToneGenerator? {
        if (tone == null) {
            tone = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            } catch (e: RuntimeException) {
                null
            }
        }
        return tone
    }

    private fun vibrate(pattern: LongArray) {
        val ctx = appContext ?: return
        try {
            vibrator(ctx)?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } catch (e: Exception) {
            // Vibration is a nice-to-have.
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrator(ctx: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
