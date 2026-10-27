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

/** PREPARE is the "get ready" countdown before the first exercise. */
enum class PhaseType { PREPARE, WORK, REST }

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
    /** Non-null for a reps set: no timer, it ends when the user taps Done. [seconds] is 0 then. */
    val reps: Int? = null,
    /** True for the first exercise of a cycle round: the moment a new round begins. */
    val roundStart: Boolean = false,
    /** Planned weight, only for exercises that use weight. */
    val weight: Double? = null,
    /** 1-based cycle number in the complex, 0 for a single exercise (drives the progress line). */
    val cycle: Int = 0,
) {
    val isReps: Boolean get() = type == PhaseType.WORK && reps != null
}

data class WorkoutState(
    val complexName: String,
    val phases: List<Phase>,
    val index: Int,
    val remainingMs: Long,
    val paused: Boolean,
    val finished: Boolean,
    val totalWork: Int,
    val plannedSeconds: Int,
    /** Time spent on the current reps set. */
    val elapsedMs: Long = 0,
    val repsSets: Int = 0,
    /** Real time the workout took, pauses excluded; set when it finishes. */
    val actualSeconds: Int = 0,
    /** Full length of the current timed phase including time added with +10 s; drives the ring. */
    val phaseTotalMs: Long = 0,
    /** Goes up by one at every breathing reminder; the screen shows the icon when it changes. */
    val breathCue: Int = 0,
) {
    /** Reps sets after the current phase; their time is unknown, so "time left" can't include them. */
    val repsSetsAhead: Int get() = phases.drop(index + 1).count { it.isReps }

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
        val prev = sets.getOrNull(i - 1)?.first
        val roundStart = f.rounds > 0 && (prev == null || prev.cycle != f.cycle || prev.round != f.round || prev.rounds != f.rounds)
        out += Phase(PhaseType.WORK, ex, f.seconds, i + 1, f.round, f.rounds, f.reps, roundStart, f.weight.takeIf { ex.usesWeight }, f.cycle)
        // No rest after the last exercise.
        if (i < sets.lastIndex && f.restAfter > 0) {
            val (next, nextEx) = sets[i + 1]
            out += Phase(PhaseType.REST, nextEx, f.restAfter, i + 2, next.round, next.rounds, next.reps, weight = next.weight.takeIf { nextEx.usesWeight }, cycle = next.cycle)
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
    /** When the current reps set started (shifted on resume so pauses don't count). */
    private var startedAt = 0L
    private var workoutStartedAt = 0L
    private var pausedAt = 0L
    private var pausedTotal = 0L
    private var lastBeepSecond = -1
    private var tone: ToneGenerator? = null
    /** Exercise time since the last breathing reminder. */
    private var breathAcc = 0L
    private var lastTickAt = 0L

    fun start(context: Context, complex: Complex, exercises: List<Exercise>) {
        val built = buildPhases(complex, exercises)
        if (built.isEmpty()) return
        val countdown = Repo.state.value.settings.countdown.coerceIn(0, 15)
        val phases = if (countdown > 0) {
            val f = built[0]
            listOf(Phase(PhaseType.PREPARE, f.exercise, countdown, 1, f.round, f.rounds, f.reps, weight = f.weight, cycle = f.cycle)) + built
        } else {
            built
        }
        val ctx = context.applicationContext
        appContext = ctx
        stopTicking()
        val first = phases[0]
        _state.value = WorkoutState(
            complexName = complex.name,
            phases = phases,
            index = 0,
            remainingMs = first.seconds * 1000L,
            phaseTotalMs = first.seconds * 1000L,
            paused = false,
            finished = false,
            totalWork = phases.count { it.type == PhaseType.WORK },
            plannedSeconds = complex.totalSeconds(),
            repsSets = phases.count { it.isReps },
        )
        endAt = now() + first.seconds * 1000L
        startedAt = now()
        workoutStartedAt = now()
        pausedTotal = 0L
        lastBeepSecond = -1
        breathAcc = 0L
        lastTickAt = now()
        signalPhaseStart(first)
        startTicking()
        ContextCompat.startForegroundService(ctx, Intent(ctx, WorkoutService::class.java))
    }

    fun togglePause() {
        val s = _state.value ?: return
        if (s.finished) return
        if (s.paused) {
            endAt = now() + s.remainingMs
            startedAt = now() - s.elapsedMs
            pausedTotal += now() - pausedAt
            _state.value = s.copy(paused = false)
        } else {
            val rem = (endAt - now()).coerceAtLeast(0)
            pausedAt = now()
            _state.value = s.copy(paused = true, remainingMs = rem, elapsedMs = now() - startedAt)
        }
    }

    /** Finishes the current reps set (or skips any other phase) and moves on. */
    fun done() = skip()

    fun skip() {
        val s = _state.value ?: return
        if (s.finished) return
        goTo(s.index + 1)
    }

    fun previous() {
        val s = _state.value ?: return
        if (s.finished) return
        val elapsed = if (s.phase.isReps) s.elapsedMs else s.phaseTotalMs - s.remainingMs
        if (elapsed > 3000 || s.index == 0) goTo(s.index) else goTo(s.index - 1)
    }

    fun addTime(seconds: Int) {
        val s = _state.value ?: return
        if (s.finished || s.phase.isReps) return
        lastBeepSecond = -1
        if (s.paused) {
            _state.value = s.copy(remainingMs = s.remainingMs + seconds * 1000L, phaseTotalMs = s.phaseTotalMs + seconds * 1000L)
        } else {
            endAt += seconds * 1000L
            _state.value = s.copy(remainingMs = endAt - now(), phaseTotalMs = s.phaseTotalMs + seconds * 1000L)
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
        val t = now()
        val dt = (t - lastTickAt).coerceIn(0L, 1000L)
        lastTickAt = t
        val s0 = _state.value ?: return
        if (s0.paused || s0.finished) return
        val s = breathCheck(s0, dt)
        if (s.phase.isReps) {
            // No countdown: wait for Done, just keep the stopwatch going.
            _state.value = s.copy(elapsedMs = now() - startedAt)
            return
        }
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

    /**
     * Counts exercise time (not rest) and gives a breathing reminder every N seconds.
     * Not in the first 2 s of a set or during the last-3-seconds countdown: then it waits a moment.
     */
    private fun breathCheck(s: WorkoutState, dt: Long): WorkoutState {
        val settings = Repo.state.value.settings
        if (!settings.breathReminders || s.phase.type != PhaseType.WORK) return s
        breathAcc += dt
        if (breathAcc < settings.breathEvery * 1000L) return s
        val inSet = now() - startedAt
        val left = if (s.phase.isReps) Long.MAX_VALUE else endAt - now()
        if (inSet < 2000 || left < 4000) return s
        breathAcc = 0L
        if (settings.beeps) {
            scope.launch {
                toneGenerator()?.startTone(ToneGenerator.TONE_PROP_PROMPT, 140)
            }
        }
        if (settings.vibration) vibrate(longArrayOf(0, 60, 90, 60))
        return s.copy(breathCue = s.breathCue + 1)
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
        startedAt = now()
        lastBeepSecond = -1
        _state.value = s.copy(index = i, remainingMs = p.seconds * 1000L, elapsedMs = 0, phaseTotalMs = p.seconds * 1000L)
        signalPhaseStart(p)
    }

    private fun finish() {
        val s = _state.value ?: return
        stopTicking()
        if (s.paused) pausedTotal += now() - pausedAt
        val actual = ((now() - workoutStartedAt - pausedTotal) / 1000).toInt().coerceAtLeast(0)
        _state.value = s.copy(finished = true, paused = false, remainingMs = 0, actualSeconds = actual)
        val settings = Repo.state.value.settings
        if (settings.beeps) toneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 900)
        if (settings.vibration) vibrate(longArrayOf(0, 400, 150, 400, 150, 600))
    }

    private fun countdownBeep() {
        if (Repo.state.value.settings.beeps) toneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    private fun signalPhaseStart(p: Phase) {
        val settings = Repo.state.value.settings
        if (p.type == PhaseType.WORK && p.roundStart) {
            // A new cycle round: three quick beeps and three short buzzes, unlike a normal switch.
            if (settings.beeps) {
                scope.launch {
                    repeat(3) {
                        toneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
                        delay(170)
                    }
                }
            }
            if (settings.vibration) vibrate(longArrayOf(0, 90, 90, 90, 90, 160))
            return
        }
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
