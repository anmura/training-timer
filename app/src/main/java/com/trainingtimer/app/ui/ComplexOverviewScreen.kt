package com.trainingtimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.AppData
import com.trainingtimer.app.data.Complex
import com.trainingtimer.app.data.ComplexItem
import com.trainingtimer.app.data.Step
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.flatten
import com.trainingtimer.app.data.formatTime
import com.trainingtimer.app.data.repsSetCount
import com.trainingtimer.app.data.totalSeconds

/** Shown when Start is tapped: what the workout holds and the equipment it needs, then Start workout. */
@Composable
fun ComplexOverviewScreen(complex: Complex?, data: AppData, onBack: () -> Unit, onEdit: () -> Unit, onStart: (Complex) -> Unit) {
    if (complex == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val needed = data.equipmentNeeded(complex)
    val exById = data.exercises.associateBy { it.id }
    val sets = complex.flatten().size
    val cycles = complex.items.count { it is ComplexItem.Cycle }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar(complex.name, onBack = onBack) {
            RoundIconButton(UiIcons.EDIT, "Edit complex", onEdit, size = 48.dp, strokeWidth = 2f)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(formatTime(complex.totalSeconds()) + if (complex.repsSetCount() > 0) "+" else "", "total", Modifier.weight(1f))
                Stat("$sets", if (sets == 1) "set" else "sets", Modifier.weight(1f))
                Stat("$cycles", if (cycles == 1) "cycle" else "cycles", Modifier.weight(1f))
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.Surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("You’ll need", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted)
                if (needed.isEmpty()) {
                    Text("No equipment needed", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Text)
                } else {
                    for (row in needed.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for ((q, n) in row) {
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(C.Surface2)
                                        .padding(horizontal = 6.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    PathIcon(com.trainingtimer.app.data.EquipmentIcons.path(q.icon), C.Lime, size = 30.dp, strokeWidth = 1.8f)
                                    Text(q.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(if (n == 1) "1 exercise" else "$n exercises", fontSize = 11.sp, color = C.Muted)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.Surface)
                    .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
            ) {
                Text("Exercises", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted, modifier = Modifier.padding(bottom = 4.dp))
                for (item in complex.items) {
                    when (item) {
                        is ComplexItem.Single -> StepLine(item.step, exById)
                        is ComplexItem.Cycle -> {
                            Row(Modifier.padding(top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                PathIcon(UiIcons.REPEAT, C.Lime, size = 15.dp, strokeWidth = 2.2f)
                                Spacer(Modifier.width(6.dp))
                                Text("Cycle ×${item.repeats}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = C.Lime)
                            }
                            Column(Modifier.padding(start = 10.dp)) { for (st in item.steps) StepLine(st, exById) }
                        }
                    }
                }
            }
            Spacer(Modifier.padding(2.dp))
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
            BigButton(
                "Start workout",
                onClick = { onStart(complex) },
                icon = UiIcons.PLAY,
                iconFilled = true,
                enabled = complex.items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(value, fontFamily = Condensed, fontSize = 28.sp, color = C.Text, maxLines = 1)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Muted)
    }
}

@Composable
private fun StepLine(step: Step, exById: Map<String, com.trainingtimer.app.data.Exercise>) {
    val e = exById[step.exerciseId] ?: return
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        ExerciseIcon(e.icon, tileSize = 36.dp, iconSize = 20.dp, tile = C.Surface2, corner = 10.dp)
        Spacer(Modifier.width(12.dp))
        Text(e.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(
            if (step.byReps) "×${step.reps}" else formatTime(step.seconds),
            fontFamily = Condensed,
            fontSize = 20.sp,
            color = if (step.byReps) C.Sky else C.Muted,
        )
    }
}
