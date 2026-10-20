package com.trainingtimer.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.trainingtimer.app.data.Complex
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.ui.App
import com.trainingtimer.app.ui.AppTheme
import com.trainingtimer.app.ui.NavRequests
import com.trainingtimer.app.workout.WorkoutEngine

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        setContent {
            AppTheme {
                App(onStartWorkout = { startWorkout(it) })
            }
        }
        handleLaunch(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunch(intent)
    }

    /** Opened from a reminder: start its workout or show the complexes. */
    private fun handleLaunch(intent: Intent?) {
        if (intent == null) return
        val notificationId = intent.getIntExtra(EXTRA_CANCEL_NOTIFICATION, -1)
        if (notificationId >= 0) getSystemService(NotificationManager::class.java)?.cancel(notificationId)
        val complexId = intent.getStringExtra(EXTRA_START_COMPLEX)
        val complex = complexId?.let { id -> Repo.state.value.complexes.find { it.id == id } }
        when {
            complex != null && WorkoutEngine.state.value == null -> startWorkout(complex)
            complex != null -> Unit // a workout is already running; its screen is shown
            complexId != null || intent.getBooleanExtra(EXTRA_SHOW_COMPLEXES, false) -> NavRequests.showComplexes()
        }
        // Handle each launch once (not again on rotation).
        intent.removeExtra(EXTRA_START_COMPLEX)
        intent.removeExtra(EXTRA_SHOW_COMPLEXES)
        intent.removeExtra(EXTRA_CANCEL_NOTIFICATION)
    }

    companion object {
        const val EXTRA_START_COMPLEX = "start_complex"
        const val EXTRA_SHOW_COMPLEXES = "show_complexes"
        const val EXTRA_CANCEL_NOTIFICATION = "cancel_notification"
    }

    private fun startWorkout(complex: Complex) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        WorkoutEngine.start(this, complex, Repo.state.value.exercises)
    }
}
