package com.trainingtimer.app

import android.Manifest
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
