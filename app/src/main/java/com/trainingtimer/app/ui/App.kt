package com.trainingtimer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainingtimer.app.data.Complex
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.workout.WorkoutEngine

sealed interface Screen {
    data object Home : Screen
    data class ExerciseEditor(val id: String?) : Screen
    data class ComplexEditor(val id: String?) : Screen
    data object Settings : Screen
    data object Workout : Screen
}

@Composable
fun App(onStartWorkout: (Complex) -> Unit) {
    val data by Repo.state.collectAsStateWithLifecycle()
    val engine by WorkoutEngine.state.collectAsStateWithLifecycle()
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    // Show the workout whenever one is running (also when the app is reopened from the notification).
    val running = engine != null
    LaunchedEffect(running) {
        if (running && stack.last() != Screen.Workout) stack.add(Screen.Workout)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        when (val top = stack.last()) {
            Screen.Home -> HomeScreen(
                data = data,
                tab = tab,
                onTab = { tab = it },
                onStart = { onStartWorkout(it) },
                onCreateComplex = { push(Screen.ComplexEditor(null)) },
                onEditComplex = { push(Screen.ComplexEditor(it)) },
                onAddExercise = { push(Screen.ExerciseEditor(null)) },
                onEditExercise = { push(Screen.ExerciseEditor(it)) },
                onSettings = { push(Screen.Settings) },
            )
            is Screen.ExerciseEditor -> key(top) {
                ExerciseEditorScreen(
                    existing = data.exercises.find { it.id == top.id },
                    onClose = { pop() },
                )
            }
            is Screen.ComplexEditor -> key(top) {
                ComplexEditorScreen(
                    existing = data.complexes.find { it.id == top.id },
                    data = data,
                    onClose = { pop() },
                )
            }
            Screen.Settings -> SettingsScreen(data.settings, onBack = { pop() })
            Screen.Workout -> WorkoutScreen(onExit = {
                stack.removeAll { it == Screen.Workout }
                if (stack.isEmpty()) stack.add(Screen.Home)
            })
        }
    }
}
