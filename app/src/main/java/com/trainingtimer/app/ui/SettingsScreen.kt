package com.trainingtimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.Settings

@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar("Settings", onBack = onBack)
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel("Workout signals")
            SwitchRow(
                title = "Beeps",
                description = "Countdown beeps in the last 3 seconds and a signal when each exercise or rest starts",
                checked = settings.beeps,
                onChange = { Repo.updateSettings(settings.copy(beeps = it)) },
            )
            SwitchRow(
                title = "Vibration",
                description = "Vibrate when an exercise or rest starts and when the workout ends",
                checked = settings.vibration,
                onChange = { Repo.updateSettings(settings.copy(vibration = it)) },
            )
            Text(
                "Beeps follow your phone's media volume.",
                fontSize = 13.sp,
                color = C.Muted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            if (version.isNotEmpty()) {
                Text(
                    "Training Timer $version",
                    fontSize = 12.sp,
                    color = C.Faint,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
            Text(description, fontSize = 13.sp, color = C.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = C.OnLime,
                checkedTrackColor = C.Lime,
                uncheckedThumbColor = C.Muted,
                uncheckedTrackColor = C.Surface2,
                uncheckedBorderColor = C.Faint,
            ),
        )
    }
}
