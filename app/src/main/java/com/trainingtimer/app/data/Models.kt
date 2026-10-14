package com.trainingtimer.app.data

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** icon is either a built-in icon key (see [ExerciseIcons]) or "img:<file name>" for a picture from the gallery. */
data class Exercise(val id: String, val name: String, val icon: String)

data class Step(val exerciseId: String, val seconds: Int, val key: String = newId())

sealed class ComplexItem {
    abstract val key: String

    data class Single(val step: Step, override val key: String = newId()) : ComplexItem()

    data class Cycle(val repeats: Int, val steps: List<Step>, override val key: String = newId()) : ComplexItem()
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
)

data class AppData(
    val exercises: List<Exercise> = emptyList(),
    val complexes: List<Complex> = emptyList(),
    val settings: Settings = Settings(),
)

/** Exercise ids in the order they are performed within one pass (cycles counted once). */
fun Complex.exerciseIds(): List<String> = items.flatMap { item ->
    when (item) {
        is ComplexItem.Single -> listOf(item.step.exerciseId)
        is ComplexItem.Cycle -> item.steps.map { it.exerciseId }
    }
}

/** Number of exercise intervals in the whole workout, cycles expanded. */
fun Complex.setCount(): Int = items.sumOf { item ->
    when (item) {
        is ComplexItem.Single -> 1
        is ComplexItem.Cycle -> item.steps.size * item.repeats
    }
}

/** Total workout length in seconds: all exercises plus rest between them (no rest after the last one). */
fun Complex.totalSeconds(): Int {
    var work = 0
    for (item in items) {
        when (item) {
            is ComplexItem.Single -> work += item.step.seconds
            is ComplexItem.Cycle -> work += item.steps.sumOf { it.seconds } * item.repeats
        }
    }
    val sets = setCount()
    val rest = if (sets > 1) (sets - 1) * restSeconds else 0
    return work + rest
}

fun Complex.describe(): String {
    val n = exerciseIds().size
    val cycles = items.filterIsInstance<ComplexItem.Cycle>()
    val parts = mutableListOf(if (n == 1) "1 exercise" else "$n exercises")
    if (cycles.size == 1) parts += "1 cycle ×${cycles[0].repeats}"
    else if (cycles.size > 1) parts += "${cycles.size} cycles"
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
