package com.trainingtimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.trainingtimer.app.data.BREATH_MAX
import com.trainingtimer.app.data.BREATH_MIN
import com.trainingtimer.app.data.AppData
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.title
import com.trainingtimer.app.reminders.ReminderScheduler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn

@Composable
fun SettingsScreen(data: AppData, onBack: () -> Unit, onReminders: () -> Unit, onCategories: () -> Unit, onEquipment: () -> Unit) {
    val settings = data.settings
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
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel("Library")
            LibraryEntry(
                "Categories",
                when (data.categories.size) {
                    0 -> "Group exercises: create, rename, colours"
                    1 -> "1 category · names and colours"
                    else -> "${data.categories.size} categories · names and colours"
                },
                UiIcons.TAG,
                C.Green,
                C.GreenDim,
                onCategories,
            )
            LibraryEntry(
                "Equipment",
                when (data.equipment.size) {
                    0 -> "What exercises need: create, icons"
                    1 -> "1 item · names and icons"
                    else -> "${data.equipment.size} items · names and icons"
                },
                UiIcons.DUMBBELL,
                C.Lime,
                C.LimeDim,
                onEquipment,
            )
            SectionLabel("Reminders")
            RemindersEntry(data, onReminders)
            SectionLabel("Workout start")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.Surface)
                    .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Countdown before start", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                    Text(
                        if (settings.countdown == 0) "Off — the first exercise starts right away" else "Get ready for ${settings.countdown} s before the first exercise",
                        fontSize = 13.sp,
                        color = C.Muted,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Stepper(
                    text = if (settings.countdown == 0) "Off" else "${settings.countdown} s",
                    onMinus = { Repo.updateSettings(settings.copy(countdown = (settings.countdown - 1).coerceAtLeast(0))) },
                    onPlus = { Repo.updateSettings(settings.copy(countdown = (settings.countdown + 1).coerceAtMost(15))) },
                    textColor = C.Orange,
                    minTextWidth = 48.dp,
                    minusLabel = "Shorter countdown",
                    plusLabel = "Longer countdown",
                )
            }
            SectionLabel("Units")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.Surface)
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Weight unit", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                    Text("For exercises that use weight", fontSize = 13.sp, color = C.Muted)
                }
                Spacer(Modifier.width(12.dp))
                SegmentedPair(
                    "kg", "lb",
                    firstSelected = settings.weightUnit != "lb",
                    color = C.Violet,
                    onChange = { kg -> Repo.updateSettings(settings.copy(weightUnit = if (kg) "kg" else "lb")) },
                )
            }
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
            SwitchRow(
                title = "Breathing reminders",
                description = "During exercises an icon pops up with a soft sound and buzz: don't forget to breathe",
                checked = settings.breathReminders,
                onChange = { Repo.updateSettings(settings.copy(breathReminders = it)) },
            )
            if (settings.breathReminders) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.Surface)
                        .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Remind every", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                        Text("Counted in exercise time; rests don't count", fontSize = 13.sp, color = C.Muted)
                    }
                    Spacer(Modifier.width(12.dp))
                    val every = settings.breathEvery
                    Stepper(
                        text = "$every s",
                        onMinus = { Repo.updateSettings(settings.copy(breathEvery = (every - if (every > 60) 10 else 5).coerceAtLeast(BREATH_MIN))) },
                        onPlus = { Repo.updateSettings(settings.copy(breathEvery = (every + if (every >= 60) 10 else 5).coerceAtMost(BREATH_MAX))) },
                        textColor = C.Sky,
                        minTextWidth = 56.dp,
                        minusLabel = "Shorter interval",
                        plusLabel = "Longer interval",
                    )
                }
            }
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
private fun RemindersEntry(data: AppData, onClick: () -> Unit) {
    val on = data.reminders.count { it.enabled }
    val next = ReminderScheduler.nextOf(data.reminders)
    val text = when {
        data.reminders.isEmpty() -> "Get a notification or an alarm when it's time to train"
        next == null -> "${data.reminders.size} set · all off"
        else -> "$on on · next ${ReminderScheduler.whenLabel(next.second)}, ${next.first.title(data.complexes)}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(C.LimeDim),
            contentAlignment = Alignment.Center,
        ) {
            PathIcon(UiIcons.BELL, C.Lime, size = 26.dp, strokeWidth = 2f)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Workout reminders", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
            Text(text, fontSize = 13.sp, color = C.Muted, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        PathIcon(UiIcons.FORWARD, C.Muted, size = 22.dp, strokeWidth = 2.2f)
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
