package com.trainingtimer.app.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.AppData
import com.trainingtimer.app.data.Category
import com.trainingtimer.app.data.Complex
import com.trainingtimer.app.data.Exercise
import com.trainingtimer.app.data.ImportExport
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.describe
import com.trainingtimer.app.data.exerciseIds
import com.trainingtimer.app.data.formatTime
import com.trainingtimer.app.data.repsSetCount
import com.trainingtimer.app.data.Reminder
import com.trainingtimer.app.data.daysLabel
import com.trainingtimer.app.data.timeLabel
import com.trainingtimer.app.data.totalSeconds

@Composable
fun HomeScreen(
    data: AppData,
    tab: Int,
    onTab: (Int) -> Unit,
    onStart: (Complex) -> Unit,
    onCreateComplex: () -> Unit,
    onEditComplex: (String) -> Unit,
    onAddExercise: () -> Unit,
    onEditExercise: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var showComplexSheet by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<ImportOutcome?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) outcome = runImport(context, uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pendingExport
        pendingExport = null
        if (uri != null && text != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't save the file", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        containerColor = C.Bg,
        bottomBar = { BottomNav(tab, onTab) },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onCreateComplex,
                    containerColor = C.Lime,
                    contentColor = C.OnLime,
                    shape = RoundedCornerShape(18.dp),
                    icon = { PathIcon(UiIcons.PLUS, C.OnLime, size = 22.dp, strokeWidth = 2.4f) },
                    text = { Text("Create complex", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
                )
            } else {
                FloatingActionButton(
                    onClick = onAddExercise,
                    containerColor = C.Lime,
                    contentColor = C.OnLime,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.semantics { contentDescription = "Add exercise" },
                ) {
                    PathIcon(UiIcons.PLUS, C.OnLime, size = 26.dp, strokeWidth = 2.4f)
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (tab == 0) {
                ComplexesTab(
                    data,
                    onStart,
                    onEditComplex,
                    onSettings,
                    onImportExport = { showComplexSheet = true },
                    onExport = { c ->
                        pendingExport = ImportExport.exportComplexes(Repo.state.value, listOf(c))
                        exportLauncher.launch(fileNameFor(c.name))
                    },
                )
            } else {
                ExercisesTab(data, onEditExercise, onImportExport = { showSheet = true })
            }
        }
    }

    if (showComplexSheet) {
        ComplexImportExportSheet(
            complexCount = data.complexes.size,
            onDismiss = { showComplexSheet = false },
            onImport = {
                showComplexSheet = false
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
            },
            onExportAll = {
                showComplexSheet = false
                pendingExport = ImportExport.exportComplexes(Repo.state.value, Repo.state.value.complexes)
                exportLauncher.launch("training_complexes.json")
            },
        )
    }
    if (showSheet) {
        ImportExportSheet(
            exerciseCount = data.exercises.size,
            onDismiss = { showSheet = false },
            onImport = {
                showSheet = false
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
            },
            onExportExercises = {
                showSheet = false
                pendingExport = ImportExport.exportExercises(Repo.state.value)
                exportLauncher.launch("training_exercises.json")
            },
            onExportBackup = {
                showSheet = false
                pendingExport = ImportExport.exportBackup(Repo.state.value)
                exportLauncher.launch("training_backup.json")
            },
        )
    }
    outcome?.let { ImportResultDialog(it, onDismiss = { outcome = null }) }
}

/** A safe file name from a complex name, e.g. "Зарядка" -> "Зарядка.json". */
private fun fileNameFor(name: String): String {
    val clean = name.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().ifEmpty { "complex" }
    return "$clean.json"
}

private fun runImport(context: Context, uri: Uri): ImportOutcome {
    val fileName = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }
    return try {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: return ImportOutcome.Failed("Couldn't open the file.")
        ImportOutcome.Done(ImportExport.importJson(text), fileName)
    } catch (e: ImportExport.ImportException) {
        ImportOutcome.Failed(e.message ?: "This file couldn't be imported.")
    } catch (e: Exception) {
        ImportOutcome.Failed("This file couldn't be read.")
    }
}

@Composable
private fun BottomNav(tab: Int, onTab: (Int) -> Unit) {
    val colors = NavigationBarItemDefaults.colors(
        selectedTextColor = C.Text,
        unselectedTextColor = C.Muted,
        indicatorColor = C.LimeDim,
    )
    NavigationBar(containerColor = C.NavBg) {
        NavigationBarItem(
            selected = tab == 1,
            onClick = { onTab(1) },
            icon = { PathIcon(UiIcons.DUMBBELL, if (tab == 1) C.Lime else C.Muted) },
            label = { Text("Exercises", fontWeight = FontWeight.Bold) },
            colors = colors,
        )
        NavigationBarItem(
            selected = tab == 0,
            onClick = { onTab(0) },
            icon = { PathIcon(UiIcons.LIST, if (tab == 0) C.Lime else C.Muted) },
            label = { Text("Complexes", fontWeight = FontWeight.Bold) },
            colors = colors,
        )
    }
}

// ---------- complexes ----------

/** "Mon · Wed · Fri · 07:30" on a complex card; an alarm wins the icon if there are both kinds. */
@Composable
private fun ReminderChip(reminders: List<Reminder>) {
    val first = reminders.sortedWith(compareBy({ it.hour }, { it.minute })).first()
    val alarm = reminders.any { it.alarm }
    val more = if (reminders.size > 1) " +${reminders.size - 1}" else ""
    Row(
        Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (alarm) C.AlarmBg else C.Surface2)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (alarm) C.Orange else Color(0xFFC9CED3)
        PathIcon(if (alarm) UiIcons.ALARM else UiIcons.BELL, color, size = 13.dp, strokeWidth = 2.4f)
        Spacer(Modifier.width(5.dp))
        Text(
            "${first.daysLabel().replace(" · ", ", ")} · ${first.timeLabel()}$more",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ComplexesTab(
    data: AppData,
    onStart: (Complex) -> Unit,
    onEdit: (String) -> Unit,
    onSettings: () -> Unit,
    onImportExport: () -> Unit,
    onExport: (Complex) -> Unit,
) {
    var toDelete by remember { mutableStateOf<Complex?>(null) }
    val exById = remember(data.exercises) { data.exercises.associateBy { it.id } }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
    ) {
        TabHeader("Your training", "Complexes") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(UiIcons.IMPORT_EXPORT, "Import or export complexes", onImportExport, background = C.Surface, iconSize = 20.dp, tint = C.Lime)
                RoundIconButton(UiIcons.SETTINGS, "Settings", onSettings, background = C.Surface, iconSize = 20.dp)
            }
        }
        if (data.complexes.isEmpty()) {
            EmptyState("No complexes yet", "Tap “Create complex” to build your first workout.")
        }
        if (data.complexes.size > 1) {
            Text(
                "Hold a card and drag to reorder",
                fontSize = 12.sp,
                color = C.Muted,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }
        DraggableColumn(
            items = data.complexes,
            keyOf = { it.id },
            onMove = { from, to -> Repo.moveComplex(from, to) },
            spacing = 12.dp,
        ) { c, _, handle ->
            ComplexCard(
                complex = c,
                exById = exById,
                reminders = data.reminders.filter { it.enabled && it.complexId == c.id },
                handle = handle,
                onStart = { onStart(c) },
                onEdit = { onEdit(c.id) },
                onExport = { onExport(c) },
                onDelete = { toDelete = c },
            )
        }
    }
    toDelete?.let { c ->
        ConfirmDialog(
            title = "Delete “${c.name}”?",
            text = "This complex will be removed. Your exercises stay in the library.",
            confirmLabel = "Delete",
            onConfirm = {
                Repo.deleteComplex(c.id)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun ComplexCard(
    complex: Complex,
    exById: Map<String, Exercise>,
    reminders: List<Reminder>,
    handle: Modifier,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val icons = complex.exerciseIds().distinct().mapNotNull { exById[it]?.icon }
    Column(
        handle
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(C.Surface)
            .clickable(onClick = onEdit)
            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(complex.name, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                Text(complex.describe(), fontSize = 13.sp, color = C.Muted)
                if (reminders.isNotEmpty()) ReminderChip(reminders)
            }
            Text(
                formatTime(complex.totalSeconds()) + if (complex.repsSetCount() > 0) "+" else "",
                fontFamily = Condensed,
                fontSize = 30.sp,
                color = C.Text,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            )
            OverflowMenu("Complex options", listOf("Edit" to onEdit, "Export" to onExport, "Delete" to onDelete))
        }
        Row(
            Modifier.padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Show only as many icons as fit, with a "+N" tile for the rest, and keep a gap before Start.
            BoxWithConstraints(Modifier.weight(1f).padding(end = 12.dp)) {
                val slots = ((maxWidth + 6.dp) / 40.dp).toInt().coerceAtLeast(1)
                val shown = if (icons.size <= slots) icons else icons.take(slots - 1)
                val hidden = icons.size - shown.size
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (icon in shown) ExerciseIcon(icon, tileSize = 34.dp, iconSize = 20.dp, corner = 10.dp)
                    if (hidden > 0) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.Surface2),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+$hidden", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted, maxLines = 1)
                        }
                    }
                }
            }
            BigButton(
                text = "Start",
                onClick = onStart,
                icon = UiIcons.PLAY,
                iconFilled = true,
                height = 44.dp,
                fontSize = 15.sp,
                enabled = complex.items.isNotEmpty(),
            )
        }
    }
}

// ---------- exercises ----------

@Composable
private fun ExercisesTab(
    data: AppData,
    onEdit: (String) -> Unit,
    onImportExport: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<Exercise?>(null) }
    var manageCategories by remember { mutableStateOf(false) }
    // A deleted category can't stay selected.
    val activeFilter = filter?.takeIf { f -> f == NO_CATEGORY || data.categories.any { it.id == f } }
    val searched = remember(data.exercises, query) {
        val q = query.trim()
        data.exercises
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
    }
    val list = searched.inCategory(activeFilter)
    // "All" with categories: one group per category (an exercise can appear in several), then the uncategorised ones.
    val groups: List<Pair<Category?, List<Exercise>>> = if (activeFilter == null && data.categories.isNotEmpty()) {
        data.categories.map { c -> c to list.filter { c.id in it.categories } }.filter { it.second.isNotEmpty() } +
            listOf<Pair<Category?, List<Exercise>>>(null to list.filter { it.categories.isEmpty() }).filter { it.second.isNotEmpty() }
    } else {
        listOf(null to list)
    }
    val grouped = activeFilter == null && data.categories.isNotEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header") {
            Box(Modifier.padding(horizontal = 16.dp)) {
                TabHeader(
                    if (data.exercises.size == 1) "1 exercise" else "${data.exercises.size} exercises",
                    "Exercises",
                ) {
                    PillButton("Import", UiIcons.IMPORT_EXPORT, onImportExport)
                }
            }
        }
        item(key = "search") {
            Box(Modifier.padding(horizontal = 16.dp)) {
                SearchField(query, onChange = { query = it })
            }
            Spacer(Modifier.height(12.dp))
        }
        item(key = "filters") {
            CategoryFilterRow(
                categories = data.categories,
                exercises = searched,
                selected = activeFilter,
                onSelect = { filter = it },
                onManage = { manageCategories = true },
            )
            Spacer(Modifier.height(6.dp))
        }
        if (list.isEmpty()) {
            item(key = "empty") {
                when {
                    data.exercises.isEmpty() -> EmptyState("No exercises yet", "Tap + to add one, or import them from a file.")
                    query.isNotBlank() -> EmptyState("Nothing found", "No exercise matches “${query.trim()}”.")
                    else -> EmptyState("Nothing here", "No exercises in this category yet.")
                }
            }
        }
        for ((cat, exercises) in groups) {
            val groupKey = cat?.id ?: NO_CATEGORY
            if (grouped) {
                item(key = "h-$groupKey") {
                    Row(
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColorDot(if (cat != null) categoryColor(cat.color) else C.Faint, 10.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (cat?.name ?: "No category").uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = C.Muted,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${exercises.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Faint)
                    }
                }
            }
            items(exercises, key = { "$groupKey-${it.id}" }) { e ->
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEdit(e.id) }
                        .padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseIcon(e.icon, tileSize = 48.dp, iconSize = 26.dp, tile = C.Surface, corner = 14.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(e.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Text)
                        // In grouped view the group already says it; show the line only for extra categories or filtered views.
                        if (!grouped || e.categories.size > 1) CategoryLine(e, data.categories)
                    }
                    OverflowMenu(
                        "Exercise options",
                        listOf("Edit" to { onEdit(e.id) }, "Delete" to { toDelete = e }),
                        size = 44.dp,
                    )
                }
            }
        }
    }
    if (manageCategories) {
        CategoryManagerSheet(data, onDismiss = { manageCategories = false })
    }
    toDelete?.let { e ->
        val used = Repo.usageCount(e.id)
        ConfirmDialog(
            title = "Delete “${e.name}”?",
            text = if (used > 0) {
                "It is used in $used complex${if (used == 1) "" else "es"} and will be removed from ${if (used == 1) "it" else "them"} too."
            } else {
                "This exercise will be removed from your library."
            },
            confirmLabel = "Delete",
            onConfirm = {
                Repo.deleteExercise(e.id)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, background: Color = C.Surface) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PathIcon(UiIcons.SEARCH, C.Muted, size = 20.dp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search exercises", color = C.Muted, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = C.Text, fontSize = 15.sp),
                cursorBrush = SolidColor(C.Lime),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            RoundIconButton(UiIcons.CLOSE, "Clear search", { onChange("") }, size = 36.dp, tint = C.Muted, iconSize = 18.dp)
        }
    }
}
