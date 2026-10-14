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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.trainingtimer.app.data.Complex
import com.trainingtimer.app.data.Exercise
import com.trainingtimer.app.data.ImportExport
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.describe
import com.trainingtimer.app.data.exerciseIds
import com.trainingtimer.app.data.formatTime
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
                ComplexesTab(data, onStart, onEditComplex, onSettings)
            } else {
                ExercisesTab(data, onEditExercise, onImportExport = { showSheet = true })
            }
        }
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
            selected = tab == 0,
            onClick = { onTab(0) },
            icon = { PathIcon(UiIcons.LIST, if (tab == 0) C.Lime else C.Muted) },
            label = { Text("Complexes", fontWeight = FontWeight.Bold) },
            colors = colors,
        )
        NavigationBarItem(
            selected = tab == 1,
            onClick = { onTab(1) },
            icon = { PathIcon(UiIcons.DUMBBELL, if (tab == 1) C.Lime else C.Muted) },
            label = { Text("Exercises", fontWeight = FontWeight.Bold) },
            colors = colors,
        )
    }
}

// ---------- complexes ----------

@Composable
private fun ComplexesTab(
    data: AppData,
    onStart: (Complex) -> Unit,
    onEdit: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var toDelete by remember { mutableStateOf<Complex?>(null) }
    val exById = remember(data.exercises) { data.exercises.associateBy { it.id } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TabHeader("Your training", "Complexes") {
                RoundIconButton(UiIcons.SETTINGS, "Settings", onSettings, background = C.Surface, iconSize = 20.dp)
            }
        }
        if (data.complexes.isEmpty()) {
            item { EmptyState("No complexes yet", "Tap “Create complex” to build your first training.") }
        }
        items(data.complexes, key = { it.id }) { c ->
            ComplexCard(
                complex = c,
                exById = exById,
                onStart = { onStart(c) },
                onEdit = { onEdit(c.id) },
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
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val icons = complex.exerciseIds().distinct().mapNotNull { exById[it]?.icon }.take(5)
    Column(
        Modifier
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
            }
            Text(
                formatTime(complex.totalSeconds()),
                fontFamily = Condensed,
                fontSize = 30.sp,
                color = C.Text,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            )
            OverflowMenu("Complex options", listOf("Edit" to onEdit, "Delete" to onDelete))
        }
        Row(
            Modifier.padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (icon in icons) ExerciseIcon(icon, tileSize = 34.dp, iconSize = 20.dp, corner = 10.dp)
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
    var toDelete by remember { mutableStateOf<Exercise?>(null) }
    val list = remember(data.exercises, query) {
        val q = query.trim()
        data.exercises
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            TabHeader(
                if (data.exercises.size == 1) "1 exercise" else "${data.exercises.size} exercises",
                "Exercises",
            ) {
                PillButton("Import / Export", UiIcons.IMPORT_EXPORT, onImportExport)
            }
        }
        item {
            SearchField(query, onChange = { query = it })
            Spacer(Modifier.height(8.dp))
        }
        if (list.isEmpty()) {
            item {
                if (data.exercises.isEmpty()) {
                    EmptyState("No exercises yet", "Tap + to add one, or import them from a file.")
                } else {
                    EmptyState("Nothing found", "No exercise matches “${query.trim()}”.")
                }
            }
        }
        items(list, key = { it.id }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onEdit(e.id) }
                    .padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExerciseIcon(e.icon, tileSize = 48.dp, iconSize = 26.dp, tile = C.Surface, corner = 14.dp)
                Spacer(Modifier.width(14.dp))
                Text(e.name, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Text)
                OverflowMenu(
                    "Exercise options",
                    listOf("Edit" to { onEdit(e.id) }, "Delete" to { toDelete = e }),
                    size = 44.dp,
                )
            }
        }
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
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(C.Surface)
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
