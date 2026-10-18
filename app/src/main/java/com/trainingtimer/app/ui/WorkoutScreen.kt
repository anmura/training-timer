package com.trainingtimer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.formatTime
import com.trainingtimer.app.workout.Phase
import com.trainingtimer.app.workout.PhaseType
import com.trainingtimer.app.workout.WorkoutEngine
import com.trainingtimer.app.workout.WorkoutState

@Composable
fun WorkoutScreen(onExit: () -> Unit) {
    val state by WorkoutEngine.state.collectAsStateWithLifecycle()
    var confirmEnd by remember { mutableStateOf(false) }
    KeepScreenOn()

    val s = state
    BackHandler {
        if (s == null || s.finished) {
            WorkoutEngine.stop()
            onExit()
        } else {
            confirmEnd = true
        }
    }

    if (s == null) {
        LaunchedEffect(Unit) { onExit() }
        Box(Modifier.fillMaxSize().background(C.Bg))
        return
    }

    when {
        s.finished -> FinishedView(s, onDone = {
            WorkoutEngine.stop()
            onExit()
        })
        s.phase.type == PhaseType.WORK -> WorkView(s, onClose = { confirmEnd = true })
        else -> RestView(s, onClose = { confirmEnd = true })
    }

    if (confirmEnd) {
        ConfirmDialog(
            title = "End workout?",
            text = "The timer will stop and this session ends.",
            confirmLabel = "End",
            onConfirm = {
                confirmEnd = false
                WorkoutEngine.stop()
                onExit()
            },
            onDismiss = { confirmEnd = false },
        )
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun WorkoutHeader(s: WorkoutState, onClose: () -> Unit, muted: Color, badgeBg: Color, badgeFg: Color) {
    val p = s.phase
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(UiIcons.CLOSE, "End workout", onClose, size = 48.dp)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(s.complexName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val progress = if (p.type == PhaseType.WORK) {
                "Exercise ${p.workNumber}/${s.totalWork}"
            } else {
                "${p.workNumber - 1}/${s.totalWork} done"
            }
            Text("$progress · ${formatTime(s.totalLeftSec)} left", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = muted, maxLines = 1)
        }
        if (p.rounds > 0) {
            Row(
                Modifier
                    .padding(end = 8.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(badgeBg)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PathIcon(UiIcons.REPEAT, badgeFg, size = 16.dp, strokeWidth = 2.2f)
                Spacer(Modifier.width(6.dp))
                Text("${p.round}/${p.rounds}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = badgeFg, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SegmentBar(s: WorkoutState, color: Color) {
    val current = s.phase.workNumber
    val doneCount = if (s.phase.type == PhaseType.WORK) current - 1 else current - 1
    if (s.totalWork <= 40) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (i in 1..s.totalWork) {
                val c = when {
                    i <= doneCount -> color
                    i == current && s.phase.type == PhaseType.WORK -> C.Text
                    else -> C.Surface3
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c),
                )
            }
        }
    } else {
        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(C.Surface3),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(doneCount.toFloat() / s.totalWork)
                    .height(4.dp)
                    .background(color),
            )
        }
    }
}

@Composable
private fun TimerRing(
    fraction: Float,
    color: Color,
    track: Color,
    size: Dp,
    stroke: Dp,
    content: @Composable () -> Unit,
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2f
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(sw))
            drawArc(
                color,
                -90f,
                360f * fraction.coerceIn(0f, 1f),
                false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

private fun fractionLeft(s: WorkoutState): Float {
    val total = s.phase.seconds * 1000f
    return if (total <= 0f) 0f else (s.remainingMs / total)
}

// ---------- exercise ----------

@Composable
private fun WorkView(s: WorkoutState, onClose: () -> Unit) {
    val p = s.phase
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        WorkoutHeader(s, onClose, muted = C.Muted, badgeBg = C.LimeDim, badgeFg = C.Lime)
        SegmentBar(s, C.Lime)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val ringSize = minOf(maxWidth - 64.dp, maxHeight - 230.dp, 320.dp).coerceAtLeast(160.dp)
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseIcon(p.exercise.icon, tileSize = 52.dp, iconSize = 30.dp, tile = C.Surface, corner = 16.dp, strokeWidth = 1.6f)
                    Spacer(Modifier.width(12.dp))
                    FitText(
                        p.exercise.name,
                        maxFontSize = 34.sp,
                        minFontSize = 18.sp,
                        maxLines = 2,
                        fontFamily = Condensed,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                TimerRing(
                    fraction = fractionLeft(s),
                    color = if (s.paused) C.Faint else C.Lime,
                    track = Color(0xFF1F2327),
                    size = ringSize,
                    stroke = 16.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatTime(s.remainingSec),
                            fontFamily = Condensed,
                            fontSize = (ringSize.value * 0.33f).sp,
                            color = C.Text,
                        )
                        Text(
                            if (s.paused) "PAUSED" else "WORK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = if (s.paused) C.Orange else C.Lime,
                        )
                    }
                }
                Controls(
                    paused = s.paused,
                    accent = C.Lime,
                    onAccent = C.OnLime,
                    sideBg = C.Surface,
                )
            }
        }
        NextCard(s)
    }
}

@Composable
private fun Controls(paused: Boolean, accent: Color, onAccent: Color, sideBg: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        RoundIconButton(UiIcons.PREV, "Previous", { WorkoutEngine.previous() }, size = 56.dp, background = sideBg, filled = true)
        RoundIconButton(
            if (paused) UiIcons.PLAY else UiIcons.PAUSE,
            if (paused) "Resume" else "Pause",
            { WorkoutEngine.togglePause() },
            size = 88.dp,
            background = accent,
            tint = onAccent,
            iconSize = 36.dp,
            filled = true,
        )
        RoundIconButton(UiIcons.NEXT, "Skip", { WorkoutEngine.skip() }, size = 56.dp, background = sideBg, filled = true)
    }
}

@Composable
private fun NextCard(s: WorkoutState) {
    val next = s.nextWork
    Row(
        Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(C.Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (next == null) {
            PathIcon(UiIcons.CHECK, C.Lime, size = 26.dp, strokeWidth = 2.6f)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("LAST EXERCISE", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = C.Muted)
                Text("Finish strong!", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
            }
        } else {
            ExerciseIcon(next.exercise.icon, tileSize = 48.dp, iconSize = 26.dp, tile = C.Surface2, corner = 14.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                val label = if (s.restIsNext) {
                    "NEXT · AFTER ${formatTime(s.phases[s.index + 1].seconds)} REST"
                } else {
                    "NEXT"
                }
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = C.Muted)
                Text(next.exercise.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatTime(next.seconds), fontFamily = Condensed, fontSize = 26.sp, color = C.Text)
        }
    }
}

// ---------- rest ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestView(s: WorkoutState, onClose: () -> Unit) {
    val p = s.phase
    val after = s.phases.drop(s.index + 1).filter { it.type == PhaseType.WORK }.drop(1).take(2)
    Column(
        Modifier
            .fillMaxSize()
            .background(C.RestBg),
    ) {
        WorkoutHeader(s, onClose, muted = C.RestMuted, badgeBg = C.RestSurface2, badgeFg = C.Orange)
        SegmentBar(s, C.Orange)
        Row(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerRing(
                fraction = fractionLeft(s),
                color = if (s.paused) C.Faint else C.Orange,
                track = C.RestSurface2,
                size = 150.dp,
                stroke = 10.dp,
            ) {
                Text(formatTime(s.remainingSec), fontFamily = Condensed, fontSize = 52.sp, color = C.Text)
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FitText(
                    if (s.paused) "PAUSED" else "REST",
                    maxFontSize = 52.sp,
                    minFontSize = 24.sp,
                    maxLines = 1,
                    color = C.Orange,
                    fontFamily = Condensed,
                )
                Text("Get into position for the next exercise.", fontSize = 14.sp, color = C.RestMuted, maxLines = 3)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BigButton(
                "+10 s",
                onClick = { WorkoutEngine.addTime(10) },
                container = C.RestSurface2,
                content = C.Text,
                height = 48.dp,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.RestSurface2),
                contentAlignment = Alignment.Center,
            ) {
                RoundIconButton(
                    if (s.paused) UiIcons.PLAY else UiIcons.PAUSE,
                    if (s.paused) "Resume" else "Pause",
                    { WorkoutEngine.togglePause() },
                    size = 48.dp,
                    filled = true,
                    iconSize = 22.dp,
                )
            }
            BigButton(
                "Skip",
                onClick = { WorkoutEngine.skip() },
                icon = UiIcons.NEXT,
                iconFilled = true,
                container = C.Orange,
                content = C.OnOrange,
                height = 48.dp,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(C.RestSurface)
                .border(1.5.dp, C.RestLine, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("UP NEXT", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp, color = C.Orange)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseIcon(p.exercise.icon, tileSize = 104.dp, iconSize = 64.dp, tile = C.RestSurface2, corner = 30.dp, strokeWidth = 1.4f)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FitText(p.exercise.name, maxFontSize = 36.sp, minFontSize = 18.sp, maxLines = 3, fontFamily = Condensed)
                    Text(formatTime(p.seconds), fontFamily = Condensed, fontSize = 30.sp, color = C.Lime)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (p.rounds > 0) InfoChip("Round ${p.round} of ${p.rounds}", UiIcons.REPEAT)
                InfoChip("Exercise ${p.workNumber} of ${s.totalWork}", null)
            }
            if (after.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(C.RestLine),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("THEN", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = C.RestMuted)
                    for (a in after) ThenRow(a)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, icon: String?) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(C.RestSurface2)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            PathIcon(icon, C.Lime, size = 15.dp, strokeWidth = 2.2f)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Text)
    }
}

@Composable
private fun ThenRow(p: Phase) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExerciseIcon(p.exercise.icon, tileSize = 44.dp, iconSize = 26.dp, tile = C.RestSurface2, corner = 12.dp)
        Spacer(Modifier.width(12.dp))
        val round = if (p.rounds > 0) " · round ${p.round}" else ""
        Text("${p.exercise.name}$round", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD9D2CB), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatTime(p.seconds), fontFamily = Condensed, fontSize = 20.sp, color = C.RestMuted)
    }
}

// ---------- finished ----------

@Composable
private fun FinishedView(s: WorkoutState, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(C.LimeDim),
            contentAlignment = Alignment.Center,
        ) {
            PathIcon(UiIcons.CHECK, C.Lime, size = 56.dp, strokeWidth = 2.6f)
        }
        Spacer(Modifier.height(24.dp))
        FitText("Workout complete", maxFontSize = 40.sp, minFontSize = 24.sp, maxLines = 2, fontFamily = Condensed, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(s.complexName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(formatTime(s.plannedSeconds), "total time")
            Stat("${s.totalWork}", "exercise sets")
        }
        Spacer(Modifier.height(40.dp))
        BigButton("Done", onClick = onDone, icon = UiIcons.CHECK, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = Condensed, fontSize = 32.sp, color = C.Text)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
    }
}
