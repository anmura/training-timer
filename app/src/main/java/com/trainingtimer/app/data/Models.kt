package com.trainingtimer.app.data

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/**
 * icon is either a built-in icon key (see [ExerciseIcons]) or "img:<file name>" for a picture from the gallery.
 * [categories] holds category ids; an exercise can be in several categories or none.
 */
data class Exercise(
    val id: String,
    val name: String,
    val icon: String,
    val categories: List<String> = emptyList(),
    /** Done with dumbbells, a kettlebell etc.: complexes get a weight field for it. */
    val usesWeight: Boolean = false,
)

/** [color] is "#RRGGBB". */
data class Category(val id: String, val name: String, val color: String)

object CategoryColors {
    val palette = listOf("#4ADE9B", "#FF9F43", "#C49BFF", "#5EC2FF", "#FF7A9C", "#FFD54A", "#7FE0D6")
}

/**
 * One exercise in a complex. A timed step lasts [seconds]; a reps step ([byReps]) has no timer and
 * ends when the user taps Done. Both values are kept so switching the type back and forth loses nothing.
 */
data class Step(
    val exerciseId: String,
    val seconds: Int,
    val key: String = newId(),
    val reps: Int = DEFAULT_REPS,
    val byReps: Boolean = false,
    /** Weight for every round (only used when the exercise uses weight). */
    val weight: Double = 0.0,
    /** In a cycle: a weight per round instead of one for all. */
    val perRound: Boolean = false,
    /** Per-round weights; missing rounds come from the fill formula below. */
    val roundWeights: List<Double> = emptyList(),
    /** "Fill" settings: round i gets fillStart + fillStep × i, then any round can be changed by hand. */
    val fillStart: Double = 0.0,
    val fillStep: Double = 2.0,
)

/** What "Fill" gives for round [index] (0-based). */
fun Step.plannedWeight(index: Int): Double = (fillStart + fillStep * index).coerceAtLeast(0.0)

/** Per-round weights for a cycle of [rounds] rounds. */
fun Step.roundWeightsFor(rounds: Int): List<Double> =
    (0 until rounds.coerceAtLeast(1)).map { roundWeights.getOrNull(it) ?: plannedWeight(it) }

/** Weight in round [round] (1-based; 0 outside cycles). */
fun Step.weightFor(round: Int): Double =
    if (perRound && round > 0) roundWeights.getOrNull(round - 1) ?: plannedWeight(round - 1) else weight

/** "12 kg", "12.5 lb". */
fun formatWeight(w: Double, unit: String): String {
    val r = Math.round(w * 100) / 100.0
    val text = if (r == Math.floor(r)) r.toLong().toString() else r.toString().trimEnd('0').trimEnd('.')
    return "$text $unit"
}

const val DEFAULT_REPS = 10
const val MAX_REPS = 999

sealed class ComplexItem {
    abstract val key: String

    data class Single(val step: Step, override val key: String = newId()) : ComplexItem()

    /** [rest] is the rest between exercises inside this cycle (also between its rounds); null = use the complex's rest. */
    data class Cycle(
        val repeats: Int,
        val steps: List<Step>,
        override val key: String = newId(),
        val rest: Int? = null,
    ) : ComplexItem()
}

data class Complex(
    val id: String,
    val name: String,
    val restSeconds: Int,
    val items: List<ComplexItem>,
)

data class Settings(
    val beeps: Boolean = true,
    val vibration: Boolean = true,
    /** Seconds of "get ready" countdown before the first exercise, 0..15 (0 = off). */
    val countdown: Int = 5,
    /** "kg" or "lb". */
    val weightUnit: String = "kg",
)

data class AppData(
    val exercises: List<Exercise> = emptyList(),
    val complexes: List<Complex> = emptyList(),
    val settings: Settings = Settings(),
    val categories: List<Category> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
)

/**
 * A workout reminder at [hour]:[minute].
 * [days] are ISO days of week (1 = Monday … 7 = Sunday); empty = one time, then it switches itself off.
 * [complexId] = remind about that workout; null = a plain "time to train" nudge with [message].
 * [alarm] = ring full screen with sound; otherwise a quiet notification.
 * [sound] is a ringtone URI for alarms; null = the phone's default alarm sound.
 */
data class Reminder(
    val id: String = newId(),
    val hour: Int = 7,
    val minute: Int = 30,
    val days: List<Int> = emptyList(),
    val complexId: String? = null,
    val message: String = "",
    val alarm: Boolean = false,
    val snoozeMinutes: Int = 10,
    val sound: String? = null,
    val enabled: Boolean = true,
)

private val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun dayShort(day: Int): String = DAY_SHORT[(day - 1).coerceIn(0, 6)]

/** "07:30" */
fun Reminder.timeLabel(): String = "%02d:%02d".format(hour, minute)

/** "Every day", "Weekdays", "Weekends", "Mon · Wed · Fri" or "Once". */
fun Reminder.daysLabel(): String {
    val d = days.sorted()
    return when {
        d.isEmpty() -> "Once"
        d.size == 7 -> "Every day"
        d == listOf(1, 2, 3, 4, 5) -> "Weekdays"
        d == listOf(6, 7) -> "Weekends"
        else -> d.joinToString(" · ") { dayShort(it) }
    }
}

fun Reminder.title(complexes: List<Complex>): String =
    complexes.find { it.id == complexId }?.name ?: message.ifBlank { DEFAULT_REMINDER_MESSAGE }

const val DEFAULT_REMINDER_MESSAGE = "Time to train"

/** Exercise ids in the order they are performed within one pass (cycles counted once). */
fun Complex.exerciseIds(): List<String> = items.flatMap { item ->
    when (item) {
        is ComplexItem.Single -> listOf(item.step.exerciseId)
        is ComplexItem.Cycle -> item.steps.map { it.exerciseId }
    }
}

/**
 * One exercise set of the expanded workout and the rest that follows it (0 after the last one).
 * [reps] is null for a timed set; for a reps set [seconds] is 0.
 */
data class FlatSet(
    val exerciseId: String,
    val seconds: Int,
    val reps: Int?,
    /** Planned weight for this set (shown only if the exercise uses weight). */
    val weight: Double,
    val round: Int,
    val rounds: Int,
    val restAfter: Int,
)

private data class RawSet(val step: Step, val round: Int, val rounds: Int, val cycleKey: String?, val cycleRest: Int?)

/** The workout in order, cycles expanded. Rest between two sets of the same cycle uses the cycle's own rest if set. */
fun Complex.flatten(): List<FlatSet> {
    val raw = ArrayList<RawSet>()
    for (item in items) {
        when (item) {
            is ComplexItem.Single -> raw += RawSet(item.step, 0, 0, null, null)
            is ComplexItem.Cycle -> for (r in 1..item.repeats) {
                for (s in item.steps) raw += RawSet(s, r, item.repeats, item.key, item.rest)
            }
        }
    }
    return raw.mapIndexed { i, r ->
        val rest = when {
            i == raw.lastIndex -> 0
            r.cycleKey != null && r.cycleKey == raw[i + 1].cycleKey -> r.cycleRest ?: restSeconds
            else -> restSeconds
        }
        val st = r.step
        FlatSet(st.exerciseId, if (st.byReps) 0 else st.seconds, if (st.byReps) st.reps else null, st.weightFor(r.round), r.round, r.rounds, rest)
    }
}

/** Number of exercise intervals in the whole workout, cycles expanded. */
fun Complex.setCount(): Int = flatten().size

/** Known workout length in seconds: timed exercises plus all rests. Reps sets add their own, unknown time. */
fun Complex.totalSeconds(): Int = flatten().sumOf { it.seconds + it.restAfter }

/** Number of reps sets in the whole workout, cycles expanded. */
fun Complex.repsSetCount(): Int = flatten().count { it.reps != null }

/** "12:30", or "12:30 + 3 reps sets" when some sets are counted in reps. */
fun Complex.durationLabel(): String {
    val reps = repsSetCount()
    val time = formatTime(totalSeconds())
    return when (reps) {
        0 -> time
        1 -> "$time + 1 reps set"
        else -> "$time + $reps reps sets"
    }
}

/** Short value of a step: "0:30" or "×12". */
fun Step.valueLabel(): String = if (byReps) "×$reps" else formatTime(seconds)

fun Complex.describe(): String {
    val n = exerciseIds().size
    val cycles = items.filterIsInstance<ComplexItem.Cycle>()
    val parts = mutableListOf(if (n == 1) "1 exercise" else "$n exercises")
    if (cycles.size == 1) parts += "1 cycle ×${cycles[0].repeats}"
    else if (cycles.size > 1) parts += "${cycles.size} cycles"
    val reps = repsSetCount()
    if (reps > 0) parts += if (reps == 1) "1 reps set" else "$reps reps sets"
    parts += if (restSeconds > 0) "rest $restSeconds s" else "no rest"
    return parts.joinToString(" · ")
}

fun formatTime(totalSeconds: Int): String {
    val s = if (totalSeconds < 0) 0 else totalSeconds
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
