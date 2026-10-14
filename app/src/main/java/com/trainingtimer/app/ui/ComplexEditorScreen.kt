package com.trainingtimer.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.AppData
import com.trainingtimer.app.data.Category
import com.trainingtimer.app.data.Complex
import com.trainingtimer.app.data.ComplexItem
import com.trainingtimer.app.data.Exercise
import com.trainingtimer.app.data.ExerciseIcons
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.Step
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.formatTime
import com.trainingtimer.app.data.newId
import com.trainingtimer.app.data.setCount
import com.trainingtimer.app.data.totalSeconds
import kotlinx.coroutines.launch

private const val DEFAULT_SECONDS = 30

/** Editable copy of a complex while the user builds it. */
private class ComplexDraft(existing: Complex?) {
    val id: String = existing?.id ?: newId()
    var name by mutableStateOf(existing?.name ?: "")
    var rest by mutableIntStateOf(existing?.restSeconds ?: 15)
    val items: SnapshotStateList<ComplexItem> = mutableStateListOf<ComplexItem>().apply {
        if (existing != null) addAll(existing.items)
    }
    val selected: SnapshotStateList<String> = mutableStateListOf()

    fun toComplex() = Complex(id, name.trim(), rest, items.toList())

    fun indexOf(key: String) = items.indexOfFirst { it.key == key }

    fun replace(key: String, item: ComplexItem) {
        val i = indexOf(key)
        if (i >= 0) items[i] = item
    }

    fun remove(key: String) {
        val i = indexOf(key)
        if (i >= 0) items.removeAt(i)
    }

    /** Moves a single exercise from the list to the end of a cycle. */
    fun moveIntoCycle(singleKey: String, cycleKey: String) {
        val single = items.getOrNull(indexOf(singleKey)) as? ComplexItem.Single ?: return
        val ci = indexOf(cycleKey)
        val cycle = items.getOrNull(ci) as? ComplexItem.Cycle ?: return
        items[ci] = cycle.copy(steps = cycle.steps + single.step.copy(key = newId()))
        remove(singleKey)
    }

    /** Takes an exercise out of a cycle and puts it into the list right after that cycle. */
    fun moveOutOfCycle(cycleKey: String, stepKey: String) {
        val ci = indexOf(cycleKey)
        val cycle = items.getOrNull(ci) as? ComplexItem.Cycle ?: return
        val step = cycle.steps.find { it.key == stepKey } ?: return
        items[ci] = cycle.copy(steps = cycle.steps.filter { it.key != stepKey })
        items.add(ci + 1, ComplexItem.Single(step.copy(key = newId())))
    }

    fun moveIndex(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices || from == to) return
        val item = items.removeAt(from)
        items.add(to, item)
    }

    fun move(key: String, delta: Int) {
        val i = indexOf(key)
        val target = i + delta
        if (i < 0 || target < 0 || target > items.lastIndex) return
        val item = items.removeAt(i)
        items.add(target, item)
    }
}

@Composable
fun ComplexEditorScreen(existing: Complex?, data: AppData, onClose: () -> Unit) {
    val draft = remember { ComplexDraft(existing) }
    var step by rememberSaveable { mutableIntStateOf(if (existing == null) 1 else 2) }

    if (step == 1) {
        SelectExercisesStep(
            exercises = data.exercises,
            categories = data.categories,
            selected = draft.selected,
            onBack = onClose,
            onNext = {
                // Rebuild the single exercises from the selection, keeping times already set and any cycles.
                val previousTimes = draft.items.filterIsInstance<ComplexItem.Single>().associate { it.step.exerciseId to it.step.seconds }
                val cycles = draft.items.filterIsInstance<ComplexItem.Cycle>()
                draft.items.clear()
                for (id in draft.selected) {
                    draft.items.add(ComplexItem.Single(Step(id, previousTimes[id] ?: DEFAULT_SECONDS)))
                }
                draft.items.addAll(cycles)
                step = 2
            },
        )
    } else {
        BuilderStep(
            draft = draft,
            data = data,
            isNew = existing == null,
            onBack = {
                if (existing == null) {
                    step = 1
                } else {
                    onClose()
                }
            },
            hasChanges = { existing != null && draft.toComplex() != existing },
            onSaved = onClose,
        )
    }
}

// ---------- step 1 ----------

@Composable
private fun SelectExercisesStep(
    exercises: List<Exercise>,
    categories: List<Category>,
    selected: SnapshotStateList<String>,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val sorted = remember(exercises) { exercises.sortedBy { it.name.lowercase() } }
    var catFilter by remember { mutableStateOf<String?>(null) }
    val shown = sorted.inCategory(catFilter)
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar("Create complex", onBack = onBack, subtitle = "Step 1 of 2 · Choose exercises")
        ProgressStrip(0.5f)
        Text(
            "Tick the exercises for this complex in the order you want to do them. You can set times, add cycles and reorder on the next step.",
            fontSize = 14.sp,
            color = C.Muted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        CategoryFilterRow(categories, sorted, catFilter, { catFilter = it }, Modifier.padding(bottom = 8.dp))
        if (sorted.isEmpty()) {
            EmptyState("Your library is empty", "Add exercises on the Exercises tab first.", Modifier.weight(1f))
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(shown, key = { it.id }) { e ->
                    val order = selected.indexOf(e.id)
                    CheckRow(
                        exercise = e,
                        checked = order >= 0,
                        onToggle = { on -> if (on) selected.add(e.id) else selected.remove(e.id) },
                        trailing = {
                            if (order >= 0) {
                                Text("#${order + 1}", fontFamily = Condensed, fontSize = 20.sp, color = C.Lime)
                            }
                        },
                    )
                }
            }
        }
        ActionBar {
            Column(Modifier.weight(1f)) {
                Text("${selected.size}", fontFamily = Condensed, fontSize = 28.sp, color = C.Text)
                Text("selected", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
            }
            BigButton("Next: set times", onClick = onNext, icon = null, enabled = selected.isNotEmpty())
        }
    }
}

// ---------- step 2 ----------

@Composable
private fun BuilderStep(
    draft: ComplexDraft,
    data: AppData,
    isNew: Boolean,
    onBack: () -> Unit,
    hasChanges: () -> Boolean,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val exById = remember(data.exercises) { data.exercises.associateBy { it.id } }
    var nameError by remember { mutableStateOf(false) }
    var pickerForCycle by remember { mutableStateOf<String?>(null) }
    var pickerForSingles by remember { mutableStateOf(false) }
    var showCycleSheet by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val tryBack: () -> Unit = {
        if (hasChanges()) {
            confirmDiscard = true
        } else {
            onBack()
        }
    }
    BackHandler(onBack = tryBack)

    val preview = draft.toComplex()
    val total = preview.totalSeconds()
    val sets = preview.setCount()

    fun save() {
        val name = draft.name.trim()
        if (name.isEmpty()) {
            nameError = true
            scope.launch { scrollState.animateScrollTo(0) }
            Toast.makeText(context, "Enter a name for the complex", Toast.LENGTH_SHORT).show()
            return
        }
        val cleaned = draft.items.filter { !(it is ComplexItem.Cycle && it.steps.isEmpty()) }
        if (cleaned.isEmpty()) {
            Toast.makeText(context, "Add at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }
        Repo.saveComplex(Complex(draft.id, name, draft.rest, cleaned))
        onSaved()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar(
            title = if (isNew) "Create complex" else "Edit complex",
            onBack = tryBack,
            subtitle = if (isNew) "Step 2 of 2 · Times, cycles & rest" else null,
        )
        if (isNew) ProgressStrip(1f)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = {
                    draft.name = it
                    nameError = false
                },
                label = { Text("Complex name") },
                singleLine = true,
                isError = nameError,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Lime,
                    focusedLabelColor = C.Lime,
                    cursorColor = C.Lime,
                    unfocusedBorderColor = C.Line,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.Surface)
                    .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PathIcon(UiIcons.TIMER, C.Orange, size = 24.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Rest between exercises", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Text)
                    Text("Not added after the last one", fontSize = 12.sp, color = C.Muted)
                }
                TimeStepper(draft.rest, { draft.rest = it }, min = 0, textColor = C.Orange, title = "Rest time")
            }
            SectionLabel("Sequence", trailing = if (draft.items.size > 1) "Hold ⠿ and drag to reorder" else null)
            if (draft.items.isEmpty()) {
                EmptyState("Nothing here yet", "Add exercises or a cycle below.")
            }
            val cycleKeys = draft.items.filterIsInstance<ComplexItem.Cycle>().map { it.key }
            DraggableColumn(
                items = draft.items,
                keyOf = { it.key },
                onMove = { from, to -> draft.moveIndex(from, to) },
            ) { item, _, handle ->
                when (item) {
                    is ComplexItem.Single -> SingleRow(
                        item = item,
                        exercise = exById[item.step.exerciseId],
                        handle = handle,
                        onTime = { sec -> draft.replace(item.key, item.copy(step = item.step.copy(seconds = sec))) },
                        menu = cycleKeys.mapIndexed { n, key ->
                            (if (cycleKeys.size == 1) "Move into cycle" else "Move into cycle ${n + 1}") to { draft.moveIntoCycle(item.key, key) }
                        } + ("Remove" to { draft.remove(item.key) }),
                    )
                    is ComplexItem.Cycle -> CycleBlock(
                        cycle = item,
                        number = if (cycleKeys.size > 1) cycleKeys.indexOf(item.key) + 1 else 0,
                        complexRest = draft.rest,
                        exById = exById,
                        handle = handle,
                        onChange = { draft.replace(item.key, it) },
                        onMoveOut = { stepKey -> draft.moveOutOfCycle(item.key, stepKey) },
                        onAddExercises = { pickerForCycle = item.key },
                        onRemove = { draft.remove(item.key) },
                    )
                }
            }
        }

        // Add buttons stay pinned above the save bar, so there's no need to scroll down to them.
        Column(Modifier.fillMaxWidth().background(C.Bg)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BigButton(
                    text = "Exercise",
                    onClick = { pickerForSingles = true },
                    icon = UiIcons.PLUS,
                    container = C.Surface,
                    content = C.Text,
                    height = 48.dp,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                BigButton(
                    text = "Cycle",
                    onClick = { showCycleSheet = true },
                    icon = UiIcons.REPEAT,
                    container = C.Surface,
                    content = C.Text,
                    height = 48.dp,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ActionBar {
            Column(Modifier.weight(1f)) {
                Text(formatTime(total), fontFamily = Condensed, fontSize = 30.sp, color = C.Text)
                Text("total · $sets exercise sets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
            }
            BigButton(if (isNew) "Save complex" else "Save", onClick = { save() }, icon = UiIcons.CHECK)
        }
    }

    if (pickerForSingles) {
        ExercisePickerSheet(
            title = "Add exercises",
            exercises = data.exercises,
            categories = data.categories,
            onDismiss = { pickerForSingles = false },
            onAdd = { ids ->
                for (id in ids) draft.items.add(ComplexItem.Single(Step(id, DEFAULT_SECONDS)))
                pickerForSingles = false
            },
        )
    }
    pickerForCycle?.let { cycleKey ->
        ExercisePickerSheet(
            title = "Add to cycle",
            exercises = data.exercises,
            categories = data.categories,
            onDismiss = { pickerForCycle = null },
            onAdd = { ids ->
                val i = draft.indexOf(cycleKey)
                val cycle = draft.items.getOrNull(i) as? ComplexItem.Cycle
                if (cycle != null) {
                    draft.items[i] = cycle.copy(steps = cycle.steps + ids.map { Step(it, DEFAULT_SECONDS) })
                }
                pickerForCycle = null
            },
        )
    }
    if (showCycleSheet) {
        AddCycleSheet(
            exercises = data.exercises,
            categories = data.categories,
            onDismiss = { showCycleSheet = false },
            onAdd = { repeats, steps ->
                draft.items.add(ComplexItem.Cycle(repeats, steps))
                showCycleSheet = false
            },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "Your changes to this complex won't be saved.",
            confirmLabel = "Discard",
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun SingleRow(
    item: ComplexItem.Single,
    exercise: Exercise?,
    handle: Modifier,
    onTime: (Int) -> Unit,
    menu: List<Pair<String, () -> Unit>>,
) {
    Row(
        handle
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.Surface)
            .padding(start = 2.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PathIcon(UiIcons.GRIP, C.Faint, size = 20.dp, strokeWidth = 3f)
        Spacer(Modifier.width(4.dp))
        ExerciseIcon(exercise?.icon ?: ExerciseIcons.DEFAULT, tileSize = 40.dp, iconSize = 22.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            exercise?.name ?: "Deleted exercise",
            Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = C.Text,
            lineHeight = 18.sp,
        )
        TimeStepper(item.step.seconds, onTime, buttonWidth = 30.dp, title = exercise?.name ?: "Set time")
        OverflowMenu("Exercise options", menu, size = 36.dp)
    }
}

@Composable
private fun CycleBlock(
    cycle: ComplexItem.Cycle,
    number: Int,
    complexRest: Int,
    exById: Map<String, Exercise>,
    onChange: (ComplexItem.Cycle) -> Unit,
    onMoveOut: (String) -> Unit,
    handle: Modifier,
    onAddExercises: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(C.CycleBg)
            .border(1.5.dp, C.LimeBorder, shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(handle, verticalAlignment = Alignment.CenterVertically) {
            PathIcon(UiIcons.GRIP, C.Faint, size = 20.dp, strokeWidth = 3f)
            Spacer(Modifier.width(2.dp))
            PathIcon(UiIcons.REPEAT, C.Lime, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (number > 0) "Cycle $number" else "Cycle", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                val n = cycle.steps.size
                Text(
                    "${if (n == 1) "1 exercise" else "$n exercises"} · ${formatTime(cycle.steps.sumOf { it.seconds })} per round",
                    fontSize = 12.sp,
                    color = C.Muted,
                )
            }
            Stepper(
                text = "×${cycle.repeats}",
                onMinus = { onChange(cycle.copy(repeats = (cycle.repeats - 1).coerceAtLeast(1))) },
                onPlus = { onChange(cycle.copy(repeats = (cycle.repeats + 1).coerceAtMost(99))) },
                textColor = C.Lime,
                buttonWidth = 32.dp,
                minTextWidth = 40.dp,
                minusLabel = "Fewer repeats",
                plusLabel = "More repeats",
            )
            OverflowMenu("Cycle options", listOf("Remove cycle" to onRemove), size = 36.dp)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.Surface)
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PathIcon(UiIcons.TIMER, C.Orange, size = 20.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Rest in cycle", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Text)
                if (cycle.rest == null) {
                    Text("Same as complex", fontSize = 12.sp, color = C.Muted)
                } else {
                    Text(
                        "Use complex rest",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Lime,
                        modifier = Modifier.clickable(role = Role.Button) { onChange(cycle.copy(rest = null)) },
                    )
                }
            }
            TimeStepper(
                cycle.rest ?: complexRest,
                { onChange(cycle.copy(rest = it)) },
                min = 0,
                textColor = if (cycle.rest == null) C.Muted else C.Orange,
                buttonWidth = 28.dp,
                title = "Rest in cycle",
            )
        }
        DraggableColumn(
            items = cycle.steps,
            keyOf = { it.key },
            onMove = { from, to ->
                val steps = cycle.steps.toMutableList()
                steps.add(to, steps.removeAt(from))
                onChange(cycle.copy(steps = steps))
            },
        ) { s, _, stepHandle ->
            val ex = exById[s.exerciseId]
            Row(
                stepHandle
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.Surface)
                    .padding(start = 2.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PathIcon(UiIcons.GRIP, C.Faint, size = 18.dp, strokeWidth = 3f)
                Spacer(Modifier.width(2.dp))
                ExerciseIcon(ex?.icon ?: ExerciseIcons.DEFAULT, tileSize = 38.dp, iconSize = 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    ex?.name ?: "Deleted exercise",
                    Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Text,
                    lineHeight = 18.sp,
                )
                TimeStepper(
                    s.seconds,
                    { sec -> onChange(cycle.copy(steps = cycle.steps.map { if (it.key == s.key) it.copy(seconds = sec) else it })) },
                    buttonWidth = 28.dp,
                    title = ex?.name ?: "Set time",
                )
                OverflowMenu(
                    "Exercise options",
                    listOf(
                        "Move out of cycle" to { onMoveOut(s.key) },
                        "Remove from cycle" to { onChange(cycle.copy(steps = cycle.steps.filter { it.key != s.key })) },
                    ),
                    size = 34.dp,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, C.LimeBorder, RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onAddExercises),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PathIcon(UiIcons.PLUS, C.Lime, size = 18.dp, strokeWidth = 2.4f)
            Spacer(Modifier.width(6.dp))
            Text("Add exercises to cycle", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Lime)
        }
    }
}

// ---------- sheets ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    title: String,
    exercises: List<Exercise>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = remember { mutableStateListOf<String>() }
    val sorted = remember(exercises) { exercises.sortedBy { it.name.lowercase() } }
    var catFilter by remember { mutableStateOf<String?>(null) }
    val shown = sorted.inCategory(catFilter)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = C.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(title, fontFamily = Condensed, fontSize = 30.sp, color = C.Text)
            Spacer(Modifier.height(12.dp))
            CategoryFilterRow(categories, sorted, catFilter, { catFilter = it }, Modifier.padding(bottom = 10.dp), horizontalPadding = 0.dp)
            LazyColumn(
                Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(shown, key = { it.id }) { e ->
                    CheckRow(
                        exercise = e,
                        checked = e.id in selected,
                        onToggle = { on -> if (on) selected.add(e.id) else selected.remove(e.id) },
                        background = C.Surface2,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BigButton(
                text = if (selected.isEmpty()) "Select exercises" else "Add ${selected.size}",
                onClick = { onAdd(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCycleSheet(
    exercises: List<Exercise>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (Int, List<Step>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var repeats by remember { mutableIntStateOf(3) }
    val order = remember { mutableStateListOf<String>() }
    val times = remember { mutableStateMapOf<String, Int>() }
    val sorted = remember(exercises) { exercises.sortedBy { it.name.lowercase() } }
    var catFilter by remember { mutableStateOf<String?>(null) }
    val shown = sorted.inCategory(catFilter)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = C.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text("Add cycle", fontFamily = Condensed, fontSize = 30.sp, color = C.Text)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.Surface2)
                    .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Repeats", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                    Text("How many rounds of this cycle", fontSize = 12.sp, color = C.Muted)
                }
                Stepper(
                    text = "×$repeats",
                    onMinus = { repeats = (repeats - 1).coerceAtLeast(1) },
                    onPlus = { repeats = (repeats + 1).coerceAtMost(99) },
                    textColor = C.Lime,
                    buttonWidth = 44.dp,
                    minTextWidth = 52.dp,
                    fontSize = 32.sp,
                    background = C.Surface3,
                    minusLabel = "Fewer repeats",
                    plusLabel = "More repeats",
                )
            }
            SectionLabel("Exercises in cycle", Modifier.padding(top = 8.dp, bottom = 6.dp), trailing = "Time per round")
            CategoryFilterRow(categories, sorted, catFilter, { catFilter = it }, Modifier.padding(bottom = 8.dp), horizontalPadding = 0.dp)
            LazyColumn(
                Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(shown, key = { it.id }) { e ->
                    val on = e.id in order
                    CheckRow(
                        exercise = e,
                        checked = on,
                        onToggle = { checked ->
                            if (checked) {
                                order.add(e.id)
                                times[e.id] = DEFAULT_SECONDS
                            } else {
                                order.remove(e.id)
                                times.remove(e.id)
                            }
                        },
                        background = C.Surface2,
                        trailing = {
                            if (on) {
                                TimeStepper(
                                    times[e.id] ?: DEFAULT_SECONDS,
                                    { times[e.id] = it },
                                    buttonWidth = 28.dp,
                                    background = C.Surface3,
                                    title = e.name,
                                )
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BigButton(
                text = if (order.isEmpty()) "Select exercises" else "Add cycle · ${order.size} × $repeats",
                onClick = { onAdd(repeats, order.map { Step(it, times[it] ?: DEFAULT_SECONDS) }) },
                enabled = order.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
