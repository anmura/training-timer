package com.trainingtimer.app.data

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/**
 * icon is either a built-in icon key (see [ExerciseIcons]) or "img:<file name>" for a picture from the gallery.
 * [categories] holds category ids; an exercise can be in several categories or none.
 */
data class Exercise(val id: String, val name: String, val icon: String, val categories: List<String> = emptyList())

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
)

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
)

data class AppData(
    val exercises: List<Exercise> = emptyList(),
    val complexes: List<Complex> = emptyList(),
    val settings: Settings = Settings(),
    val categories: List<Category> = emptyList(),
)

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
        FlatSet(st.exerciseId, if (st.byReps) 0 else st.seconds, if (st.byReps) st.reps else null, r.round, r.rounds, rest)
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
