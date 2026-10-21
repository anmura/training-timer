package com.trainingtimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.Step
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.formatWeight
import com.trainingtimer.app.data.plannedWeight
import com.trainingtimer.app.data.roundWeightsFor

private const val MAX_WEIGHT = 999.0

/** − / + step for weights: 1 kg or 2.5 lb. */
private fun unitStep(unit: String) = if (unit == "lb") 2.5 else 1.0

private fun number(w: Double): String = formatWeight(w, "").trim()

/**
 * The weight part of an exercise row in the complex builder (only for exercises that use weight).
 * Outside a cycle ([rounds] = 0): one weight. In a cycle: Same for every round, or Per round with
 * a "Fill" (start + step) that can then be changed round by round.
 */
@Composable
fun WeightSection(step: Step, onStep: (Step) -> Unit, unit: String, rounds: Int, title: String) {
    var expanded by rememberSaveable(step.key) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.VioletDim)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PathIcon(UiIcons.WEIGHT, C.Violet, size = 15.dp, strokeWidth = 2.2f)
                Spacer(Modifier.width(6.dp))
                Text("Weight", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = C.Violet)
            }
            Spacer(Modifier.weight(1f))
            if (rounds > 0) {
                SegmentedPair("Same", "Per round", firstSelected = !step.perRound, color = C.Violet) { same ->
                    if (same) {
                        onStep(step.copy(perRound = false))
                    } else {
                        val start = if (step.roundWeights.isEmpty()) step.copy(fillStart = step.weight) else step
                        onStep(start.copy(perRound = true, roundWeights = start.roundWeightsFor(rounds)))
                        expanded = true
                    }
                }
            } else {
                WeightStepper(step.weight, unit, title) { onStep(step.copy(weight = it, fillStart = it)) }
            }
        }
        if (rounds > 0 && !step.perRound) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                WeightStepper(step.weight, unit, title) { onStep(step.copy(weight = it, fillStart = it)) }
            }
        }
        if (rounds > 0 && step.perRound) {
            PerRoundWeights(step, onStep, unit, rounds, title, expanded) { expanded = it }
        }
    }
}

@Composable
private fun PerRoundWeights(
    step: Step,
    onStep: (Step) -> Unit,
    unit: String,
    rounds: Int,
    title: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
) {
    val weights = step.roundWeightsFor(rounds)
    val edited = weights.indices.filter { weights[it] != step.plannedWeight(it) }
    var selected by rememberSaveable(step.key) { mutableIntStateOf(0) }
    val sel = selected.coerceIn(0, rounds - 1)
    var typing by remember { mutableStateOf(false) }
    val setRound: (Int, Double) -> Unit = { i, w ->
        val list = weights.toMutableList()
        list[i] = w.coerceIn(0.0, MAX_WEIGHT)
        onStep(step.copy(roundWeights = list))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Folded summary: all rounds on one line; tap to edit.
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(C.VioletBg)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Fold" else "Edit weights") { onExpand(!expanded) }
                .padding(start = 12.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    weights.joinToString(" · ") { number(it) } + " $unit",
                    fontFamily = Condensed,
                    fontSize = 19.sp,
                    color = C.Violet,
                    lineHeight = 22.sp,
                )
                val editedText = when (edited.size) {
                    0 -> ""
                    1 -> " · 1 edited by hand"
                    else -> " · ${edited.size} edited by hand"
                }
                Text("$rounds rounds$editedText", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = C.Muted)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (expanded) "Done" else "Edit", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = C.Violet)
            PathIcon("M6 9l6 6 6-6", C.Violet, modifier = Modifier.rotate(if (expanded) 180f else 0f), size = 18.dp, strokeWidth = 2.4f)
        }
        if (!expanded) return@Column

        // Fill: start + step, Apply sets every round (and drops hand edits).
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(C.VioletBg)
                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Fill", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color8)
            Spacer(Modifier.width(4.dp))
            Stepper(
                text = number(step.fillStart),
                onMinus = { onStep(step.copy(fillStart = (step.fillStart - unitStep(unit)).coerceAtLeast(0.0))) },
                onPlus = { onStep(step.copy(fillStart = (step.fillStart + unitStep(unit)).coerceAtMost(MAX_WEIGHT))) },
                textColor = C.Violet,
                buttonWidth = 26.dp,
                minTextWidth = 32.dp,
                fontSize = 18.sp,
                background = C.VioletDim,
                minusLabel = "Lower start weight",
                plusLabel = "Higher start weight",
            )
            Spacer(Modifier.width(4.dp))
            Text("step", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color8)
            Spacer(Modifier.width(4.dp))
            Stepper(
                text = (if (step.fillStep > 0) "+" else if (step.fillStep < 0) "−" else "") + number(kotlin.math.abs(step.fillStep)),
                onMinus = { onStep(step.copy(fillStep = (step.fillStep - unitStep(unit)).coerceAtLeast(-50.0))) },
                onPlus = { onStep(step.copy(fillStep = (step.fillStep + unitStep(unit)).coerceAtMost(50.0))) },
                textColor = C.Violet,
                buttonWidth = 26.dp,
                minTextWidth = 28.dp,
                fontSize = 18.sp,
                background = C.VioletDim,
                minusLabel = "Smaller step",
                plusLabel = "Bigger step",
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Violet)
                    .clickable(role = Role.Button) {
                        onStep(step.copy(roundWeights = (0 until rounds).map { step.plannedWeight(it) }))
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Apply", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = C.OnLime)
            }
        }

        // Rounds, four per row; tap one to change it below.
        for (row in weights.indices.chunked(4)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in row) {
                    val isSel = i == sel
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) C.VioletDim else C.Surface2)
                            .border(2.dp, if (isSel) C.Violet else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable(role = Role.Tab) { selected = i }
                            .semantics { contentDescription = "Round ${i + 1}, ${formatWeight(weights[i], unit)}" + if (i in edited) ", changed by hand" else "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("R${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = C.Muted)
                            Text(number(weights[i]), fontFamily = Condensed, fontSize = 20.sp, color = C.Violet)
                        }
                        if (i in edited) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(C.Orange),
                            )
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // Edit bar for the selected round.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.Surface2)
                .border(1.5.dp, C.VioletBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(UiIcons.BACK, "Previous round", { selected = (sel - 1).coerceAtLeast(0) }, size = 40.dp, iconSize = 20.dp)
            Column(Modifier.weight(1f)) {
                Text("Round ${sel + 1}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                Text(
                    if (sel in edited) "Edited · Fill: ${number(step.plannedWeight(sel))}" else "From Fill",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Muted,
                )
            }
            Stepper(
                text = formatWeight(weights[sel], unit),
                onMinus = { setRound(sel, weights[sel] - unitStep(unit)) },
                onPlus = { setRound(sel, weights[sel] + unitStep(unit)) },
                textColor = C.Violet,
                onTextClick = { typing = true },
                buttonWidth = 32.dp,
                minTextWidth = 56.dp,
                background = C.VioletDim,
                minusLabel = "Lighter",
                plusLabel = "Heavier",
            )
            RoundIconButton(UiIcons.FORWARD, "Next round", { selected = (sel + 1).coerceAtMost(rounds - 1) }, size = 40.dp, iconSize = 20.dp)
        }
    }
    if (typing) {
        WeightDialog("$title · round ${sel + 1}", weights[sel], unit, onDismiss = { typing = false }) {
            setRound(sel, it)
            typing = false
        }
    }
}

private val Color8 = androidx.compose.ui.graphics.Color(0xFFD8CCE8)

/** −/+ in 1 kg (2.5 lb) steps; tap the number to type it. */
@Composable
fun WeightStepper(weight: Double, unit: String, title: String, onChange: (Double) -> Unit) {
    var typing by remember { mutableStateOf(false) }
    Stepper(
        text = formatWeight(weight, unit),
        onMinus = { onChange((weight - unitStep(unit)).coerceAtLeast(0.0)) },
        onPlus = { onChange((weight + unitStep(unit)).coerceAtMost(MAX_WEIGHT)) },
        textColor = C.Violet,
        onTextClick = { typing = true },
        buttonWidth = 34.dp,
        minTextWidth = 64.dp,
        minusLabel = "Lighter",
        plusLabel = "Heavier",
    )
    if (typing) {
        WeightDialog(title, weight, unit, onDismiss = { typing = false }) {
            onChange(it)
            typing = false
        }
    }
}

@Composable
fun WeightDialog(title: String, initial: Double, unit: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    val start = number(initial)
    var field by remember { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    val value = field.text.replace(',', '.').toDoubleOrNull()
    val valid = value != null && value >= 0 && value <= MAX_WEIGHT
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text(title, color = C.Text) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { next ->
                    val cleaned = next.text.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
                    field = if (cleaned == next.text) next else TextFieldValue(cleaned, TextRange(cleaned.length))
                },
                label = { Text(unit) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 22.sp, color = C.Text),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(value!!) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Violet,
                    focusedLabelColor = C.Violet,
                    cursorColor = C.Violet,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = valid) {
                Text("OK", color = if (valid) C.Violet else C.Muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = C.Text) }
        },
    )
}
