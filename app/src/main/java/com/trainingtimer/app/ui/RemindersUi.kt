package com.trainingtimer.app.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.trainingtimer.app.data.AppData
import com.trainingtimer.app.data.DEFAULT_REMINDER_MESSAGE
import com.trainingtimer.app.data.Reminder
import com.trainingtimer.app.data.windowLabel
import com.trainingtimer.app.data.minuteLabel
import com.trainingtimer.app.data.intervalLabel
import com.trainingtimer.app.data.isRepeat
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.daysLabel
import com.trainingtimer.app.data.describe
import com.trainingtimer.app.data.durationLabel
import com.trainingtimer.app.data.timeLabel
import com.trainingtimer.app.data.title
import com.trainingtimer.app.reminders.ReminderScheduler

// ---------- list ----------

@Composable
fun RemindersScreen(data: AppData, onBack: () -> Unit, onEdit: (String?) -> Unit) {
    val context = LocalContext.current
    // Re-check permissions when coming back from the system settings.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val notificationsAllowed = remember(resumes) { notificationsAllowed(context) }
    val fullScreenAllowed = remember(resumes) { fullScreenAllowed(context) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumes++ }

    val on = data.reminders.count { it.enabled }
    val next = ReminderScheduler.nextOf(data.reminders)
    val subtitle = when {
        data.reminders.isEmpty() -> null
        next == null -> "All off"
        else -> "$on on · next ${ReminderScheduler.whenLabel(next.second)}"
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Reminders", onBack = onBack, subtitle = subtitle)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!notificationsAllowed && data.reminders.any { it.enabled }) {
                    PermissionBanner(
                        "Notifications are off, so reminders can't show up.",
                        onAllow = {
                            if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else openAppNotificationSettings(context)
                        },
                    )
                }
                if (!fullScreenAllowed && data.reminders.any { it.enabled && it.alarm }) {
                    PermissionBanner(
                        "Allow full-screen alarms so an alarm can open over the lock screen.",
                        onAllow = { openFullScreenSettings(context) },
                    )
                }
                if (data.reminders.isEmpty()) {
                    EmptyState(
                        "No reminders yet",
                        "Get a notification or an alarm at the time you want to train. Tap “New reminder” to add one.",
                    )
                }
                val sorted = data.reminders.sortedWith(compareBy({ it.hour }, { it.minute }))
                for (r in sorted) {
                    ReminderCard(
                        r = r,
                        title = r.title(data.complexes),
                        isWorkout = data.complexes.any { it.id == r.complexId },
                        onClick = { onEdit(r.id) },
                        onToggle = { Repo.setReminderEnabled(r.id, it) },
                    )
                }
                if (data.reminders.isNotEmpty()) {
                    Text(
                        "Alarms ring at full volume even in silent mode; notifications are quieter.",
                        fontSize = 13.sp,
                        color = C.Muted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
        }
        BigButton(
            "New reminder",
            onClick = { onEdit(null) },
            icon = UiIcons.PLUS,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}

@Composable
private fun ReminderCard(r: Reminder, title: String, isWorkout: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val alpha = if (r.enabled) 1f else 0.5f
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(C.Surface)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(r.timeLabel(), fontFamily = Condensed, fontSize = 44.sp, color = C.Text.copy(alpha = alpha))
                Text(
                    if (r.isRepeat) "${r.windowLabel()} · ${r.daysLabel()}" else r.daysLabel(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Muted.copy(alpha = alpha),
                )
            }
            Switch(
                checked = r.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = C.OnLime,
                    checkedTrackColor = C.Lime,
                    uncheckedThumbColor = C.Muted,
                    uncheckedTrackColor = C.Surface2,
                    uncheckedBorderColor = C.Faint,
                ),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Surface2),
                contentAlignment = Alignment.Center,
            ) {
                PathIcon(
                    if (isWorkout) UiIcons.PLAY else UiIcons.HEART,
                    (if (isWorkout) C.Lime else C.Muted).copy(alpha = alpha),
                    size = 18.dp,
                    filled = isWorkout,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = C.Text.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            TypeBadge(r.alarm, alpha)
        }
    }
}

@Composable
fun TypeBadge(alarm: Boolean, alpha: Float = 1f) {
    Row(
        Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (alarm) C.AlarmBg else C.Surface2)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = (if (alarm) C.Orange else Color(0xFFC9CED3)).copy(alpha = alpha)
        PathIcon(if (alarm) UiIcons.ALARM else UiIcons.BELL, color, size = 14.dp, strokeWidth = 2.2f)
        Spacer(Modifier.width(5.dp))
        Text(if (alarm) "Alarm" else "Notification", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun PermissionBanner(text: String, onAllow: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.AlarmBg)
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PathIcon(UiIcons.WARNING, C.Orange, size = 20.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, Modifier.weight(1f), fontSize = 13.sp, color = C.Text, lineHeight = 18.sp)
        TextButton(onClick = onAllow) { Text("Allow", color = C.Orange, fontWeight = FontWeight.ExtraBold) }
    }
}

// ---------- editor ----------

private enum class What { WORKOUT, NUDGE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorScreen(existing: Reminder?, data: AppData, onClose: () -> Unit) {
    val context = LocalContext.current
    val start = remember { existing ?: Reminder(days = listOf(1, 3, 5), complexId = data.complexes.firstOrNull()?.id) }
    var hour by rememberSaveable { mutableIntStateOf(start.hour) }
    var minute by rememberSaveable { mutableIntStateOf(start.minute) }
    var days by rememberSaveable { mutableStateOf(start.days.sorted()) }
    var alarm by rememberSaveable { mutableStateOf(start.alarm) }
    var snooze by rememberSaveable { mutableIntStateOf(start.snoozeMinutes) }
    var sound by rememberSaveable { mutableStateOf(start.sound) }
    val startsAsWorkout = start.complexId != null && data.complexes.any { it.id == start.complexId }
    var what by rememberSaveable { mutableStateOf(if (startsAsWorkout || (existing == null && data.complexes.isNotEmpty())) What.WORKOUT else What.NUDGE) }
    var complexId by rememberSaveable { mutableStateOf(start.complexId?.takeIf { id -> data.complexes.any { it.id == id } } ?: data.complexes.firstOrNull()?.id) }
    var message by rememberSaveable { mutableStateOf(start.message) }
    var timeDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var repeat by rememberSaveable { mutableStateOf(start.isRepeat) }
    var every by rememberSaveable { mutableIntStateOf(if (start.isRepeat) start.repeatMinutes else 120) }
    var from by rememberSaveable { mutableIntStateOf(start.fromMinute) }
    var until by rememberSaveable { mutableIntStateOf(start.untilMinute) }
    // Which time the picker edits: 0 = reminder time, 1 = from, 2 = until.
    var timeTarget by remember { mutableIntStateOf(0) }

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val default = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            sound = if (uri == null || uri == default) null else uri.toString()
        }
    }

    fun build(): Reminder = (existing ?: start).copy(
        hour = hour,
        minute = minute,
        days = days,
        alarm = alarm,
        snoozeMinutes = snooze,
        sound = sound,
        complexId = if (what == What.WORKOUT) complexId else null,
        message = if (what == What.NUDGE) message.trim() else "",
        enabled = true,
        repeatMinutes = if (repeat) every else 0,
        fromMinute = from,
        untilMinute = until,
        // Saving (re)starts the step: it counts from now.
        anchor = System.currentTimeMillis(),
        awaitingWorkout = false,
    )

    fun save() {
        if (repeat && until <= from) {
            Toast.makeText(context, "“Until” has to be later than “From”", Toast.LENGTH_SHORT).show()
            return
        }
        if (what == What.WORKOUT && complexId == null) {
            Toast.makeText(context, "Choose a workout or switch to “Just a nudge”", Toast.LENGTH_SHORT).show()
            return
        }
        val r = build()
        Repo.saveReminder(r)
        if (!notificationsAllowed(context) && Build.VERSION.SDK_INT >= 33) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ReminderScheduler.nextTrigger(r)?.let {
            Toast.makeText(context, "${if (r.alarm) "Alarm" else "Reminder"} set for ${ReminderScheduler.whenLabel(it)}", Toast.LENGTH_SHORT).show()
        }
        onClose()
    }

    BackHandler(onBack = onClose)

    val preview = build()
    val whenText = when {
        repeat -> "${intervalLabel(every)}, ${preview.windowLabel()}, ${preview.daysLabel().lowercase()}"
        days.isEmpty() -> ReminderScheduler.nextTrigger(preview)?.let { "Once · " + ReminderScheduler.whenLabel(it) } ?: "Once"
        else -> preview.daysLabel().let { if (it == "Every day" || it == "Weekdays" || it == "Weekends") it else "Every $it" }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar(
            if (existing == null) "New reminder" else "Edit reminder",
            onBack = onClose,
            backIcon = UiIcons.CLOSE,
            backLabel = "Close",
        ) {
            if (existing != null) {
                RoundIconButton(UiIcons.TRASH, "Delete reminder", { confirmDelete = true }, size = 48.dp, tint = C.Muted)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // Mode: once at a time, or every X between two times.
            ModeToggle(repeat) { repeat = it }
            if (!repeat) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d:%02d".format(hour, minute),
                        fontFamily = Condensed,
                        fontSize = 88.sp,
                        color = C.Text,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button, onClickLabel = "Change time") {
                            timeTarget = 0
                            timeDialog = true
                        }
                            .padding(horizontal = 12.dp),
                    )
                    Text(whenText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Every")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(C.Surface)
                            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Interval", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                            Text("Counted from when you switch it on", fontSize = 13.sp, color = C.Muted)
                        }
                        Stepper(
                            text = intervalLabel(every).removePrefix("Every "),
                            onMinus = { every = REPEAT_STEPS.lastOrNull { it < every } ?: every },
                            onPlus = { every = REPEAT_STEPS.firstOrNull { it > every } ?: every },
                            textColor = C.Lime,
                            minTextWidth = 72.dp,
                            minusLabel = "More often",
                            plusLabel = "Less often",
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (m in listOf(30, 60, 90, 120, 180)) {
                            val on = every == m
                            Box(
                                Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(if (on) C.LimeDim else C.Surface)
                                    .border(1.5.dp, if (on) C.Lime else C.Line, RoundedCornerShape(17.dp))
                                    .selectable(selected = on, role = Role.RadioButton) { every = m }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(intervalLabel(m).removePrefix("Every "), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (on) C.Lime else C.Text)
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Between")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeTile("From", from, Modifier.weight(1f)) {
                            timeTarget = 1
                            timeDialog = true
                        }
                        Text("–", fontSize = 22.sp, color = C.Faint)
                        TimeTile("Until", until, Modifier.weight(1f)) {
                            timeTarget = 2
                            timeDialog = true
                        }
                    }
                    val today = remember(every, from, until, days) { ReminderScheduler.todayTimes(preview) }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(C.CycleBg)
                            .border(1.5.dp, C.LimeDim, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("TODAY", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = C.Lime)
                        Text(
                            if (today.isEmpty()) "No more reminders today" else today.joinToString("  ·  ") { "%02d:%02d".format(it.hour, it.minute) },
                            fontFamily = Condensed,
                            fontSize = 20.sp,
                            color = C.Text,
                        )
                        Text(
                            "Counted from now. Nothing at or after ${minuteLabel(until)}. Next days start at ${minuteLabel(from)}. " +
                                "After a workout started from this reminder (or a snooze), the step starts again when it ends.",
                            fontSize = 12.sp,
                            color = C.Muted,
                            lineHeight = 17.sp,
                        )
                    }
                }
            }

            // Repeat
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Repeat")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
                    for (d in 1..7) {
                        val on = d in days
                        Box(
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(if (on) C.Lime else C.Surface)
                                .border(1.5.dp, if (on) C.Lime else C.Line, CircleShape)
                                .selectable(selected = on, role = Role.Checkbox) {
                                    days = if (on) days - d else (days + d).sorted()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(letters[d - 1], fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (on) C.OnLime else C.Muted)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetChip("Every day") { days = (1..7).toList() }
                    PresetChip("Weekdays") { days = (1..5).toList() }
                    if (!repeat) PresetChip("Once") { days = emptyList() }
                }
            }

            // How to remind
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("How to remind")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OptionCard("Notification", "A banner with a Start button.", UiIcons.BELL, !alarm, C.Lime, Modifier.weight(1f)) { alarm = false }
                    OptionCard("Alarm", "Rings full screen, even on silent.", UiIcons.ALARM, alarm, C.Orange, Modifier.weight(1f)) { alarm = true }
                }
                if (alarm) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(C.Surface),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable(role = Role.Button) { pickSound.launch(soundPickerIntent(sound)) }
                                .padding(start = 16.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Sound", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Text)
                            val soundLabel = remember(sound) { soundName(context, sound) }
                            Text(
                                soundLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = C.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(4.dp))
                            PathIcon(UiIcons.FORWARD, C.Muted, size = 20.dp, strokeWidth = 2.2f)
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(C.Surface2),
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(start = 16.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Snooze", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Text)
                            Stepper(
                                text = "$snooze min",
                                onMinus = { snooze = (snooze - 5).coerceAtLeast(5) },
                                onPlus = { snooze = (snooze + 5).coerceAtMost(30) },
                                textColor = C.Orange,
                                minTextWidth = 64.dp,
                                minusLabel = "Shorter snooze",
                                plusLabel = "Longer snooze",
                            )
                        }
                    }
                } else {
                    Text(
                        "Snooze from the notification: ${snooze} min.",
                        fontSize = 13.sp,
                        color = C.Muted,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            // Remind about
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Remind about")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OptionCard(
                        "A workout",
                        "Pick a complex. Start it right from the reminder.",
                        UiIcons.PLAY,
                        what == What.WORKOUT,
                        C.Lime,
                        Modifier.weight(1f),
                        iconFilled = true,
                    ) { what = What.WORKOUT }
                    OptionCard(
                        "Just a nudge",
                        "“Time to train”. Choose what to do later.",
                        UiIcons.HEART,
                        what == What.NUDGE,
                        C.Lime,
                        Modifier.weight(1f),
                    ) { what = What.NUDGE }
                }
                if (what == What.WORKOUT) {
                    if (data.complexes.isEmpty()) {
                        Text("You have no complexes yet. Create one first, or use “Just a nudge”.", fontSize = 13.sp, color = C.Muted)
                    }
                    for (c in data.complexes) {
                        val selected = c.id == complexId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) C.LimeBg else C.Surface)
                                .border(1.5.dp, if (selected) C.LimeBorder else Color.Transparent, RoundedCornerShape(16.dp))
                                .selectable(selected = selected, role = Role.RadioButton) { complexId = c.id }
                                .padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = C.Lime, unselectedColor = C.Faint),
                                modifier = Modifier.padding(horizontal = 10.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, maxLines = 2)
                                Text(c.describe(), fontSize = 12.sp, color = C.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(c.durationLabel().substringBefore(" "), fontFamily = Condensed, fontSize = 22.sp, color = C.Text)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it.take(80) },
                        label = { Text("Message") },
                        placeholder = { Text(DEFAULT_REMINDER_MESSAGE) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = C.Lime,
                            focusedLabelColor = C.Lime,
                            cursorColor = C.Lime,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        ActionBar {
            val target = if (what == What.WORKOUT) data.complexes.find { it.id == complexId }?.name ?: "—" else "“${message.trim().ifEmpty { DEFAULT_REMINDER_MESSAGE }}”"
            Text(
                "${if (alarm) "Alarm" else "Notification"} at ${"%02d:%02d".format(hour, minute)} · $target",
                Modifier.weight(1f),
                fontSize = 13.sp,
                color = C.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.width(12.dp))
            BigButton("Save", onClick = { save() }, icon = UiIcons.CHECK)
        }
    }

    if (timeDialog) {
        val initial = when (timeTarget) {
            1 -> from
            2 -> until % (24 * 60)
            else -> hour * 60 + minute
        }
        val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
        Dialog(onDismissRequest = { timeDialog = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = C.Surface) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(when (timeTarget) { 1 -> "From"; 2 -> "Until"; else -> "Reminder time" }, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = state)
                    Row(Modifier.align(Alignment.End)) {
                        TextButton(onClick = { timeDialog = false }) { Text("Cancel", color = C.Text) }
                        TextButton(onClick = {
                            when (timeTarget) {
                                1 -> from = state.hour * 60 + state.minute
                                2 -> until = (state.hour * 60 + state.minute).let { if (it == 0) 24 * 60 else it }
                                else -> {
                                    hour = state.hour
                                    minute = state.minute
                                }
                            }
                            timeDialog = false
                        }) { Text("OK", color = C.Lime) }
                    }
                }
            }
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete reminder?",
            text = "The ${existing.timeLabel()} reminder will be removed.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                Repo.deleteReminder(existing.id)
                onClose()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun PresetChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .border(1.5.dp, C.Line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Text)
    }
}

@Composable
private fun OptionCard(
    title: String,
    text: String,
    icon: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier,
    iconFilled: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .heightIn(min = 118.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .border(2.dp, if (selected) accent else C.Line, RoundedCornerShape(18.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PathIcon(icon, if (selected) accent else C.Muted, size = 24.dp, filled = iconFilled)
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
        Text(text, fontSize = 12.sp, color = C.Muted, lineHeight = 16.sp)
    }
}

// ---------- system helpers ----------

private fun notificationsAllowed(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false
    return context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() != false
}

private fun fullScreenAllowed(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 34) return true
    return context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() != false
}

private fun openFullScreenSettings(context: Context) {
    if (Build.VERSION.SDK_INT < 34) return
    try {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")),
        )
    } catch (e: Exception) {
        openAppNotificationSettings(context)
    }
}

private fun openAppNotificationSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    } catch (e: Exception) {
        // No settings screen available.
    }
}

private fun soundPickerIntent(current: String?): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        .putExtra(
            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            current?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        )

private fun soundName(context: Context, sound: String?): String {
    if (sound == null) return "Default alarm"
    return try {
        RingtoneManager.getRingtone(context, Uri.parse(sound))?.getTitle(context) ?: "Custom"
    } catch (e: Exception) {
        "Custom"
    }
}

/** Interval choices for repeating reminders, in minutes. */
private val REPEAT_STEPS = listOf(15, 20, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360)

@Composable
private fun ModeToggle(repeat: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(C.Surface)
            .padding(4.dp),
    ) {
        for ((label, icon, isRepeat) in listOf(Triple("At a time", UiIcons.ALARM, false), Triple("Repeat", UiIcons.REPEAT, true))) {
            val on = repeat == isRepeat
            Row(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) C.Lime else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) { onChange(isRepeat) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PathIcon(icon, if (on) C.OnLime else C.Muted, size = 16.dp, strokeWidth = 2.2f)
                Spacer(Modifier.width(6.dp))
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (on) C.OnLime else C.Muted)
            }
        }
    }
}

@Composable
private fun TimeTile(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .clickable(role = Role.Button, onClickLabel = "Change $label time", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Muted)
        Text(minuteLabel(minutes), fontFamily = Condensed, fontSize = 40.sp, color = C.Text)
    }
}
