package com.trainingtimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.AppData
import com.trainingtimer.app.data.Category
import com.trainingtimer.app.data.CategoryColors
import com.trainingtimer.app.data.Equipment
import com.trainingtimer.app.data.EquipmentIcons
import com.trainingtimer.app.data.Exercise
import com.trainingtimer.app.data.MAX_EQUIPMENT_NAME
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.exerciseIds

/** A list of ids that survives leaving the screen for another one and coming back. */
@Composable
fun rememberIdList(initial: List<String>): MutableState<List<String>> =
    rememberSaveable(saver = listSaver(save = { it.value }, restore = { mutableStateOf(it) })) { mutableStateOf(initial) }

fun List<Exercise>.withEquipment(equipmentId: String?): List<Exercise> =
    if (equipmentId == null) this else filter { equipmentId in it.equipment }

private fun countLabel(n: Int) = if (n == 1) "1 exercise" else "$n exercises"

/** Equipment icon on a rounded tile. */
@Composable
fun EquipmentIcon(
    icon: String,
    tileSize: Dp = 34.dp,
    iconSize: Dp = 20.dp,
    tile: Color = C.Surface2,
    tint: Color = C.Lime,
    corner: Dp = 10.dp,
) {
    Box(
        Modifier
            .size(tileSize)
            .clip(RoundedCornerShape(corner))
            .background(tile),
        contentAlignment = Alignment.Center,
    ) {
        PathIcon(EquipmentIcons.path(icon), tint, size = iconSize, strokeWidth = 2f)
    }
}

/** Small equipment icons for an exercise row in the library. */
@Composable
fun EquipmentStrip(exercise: Exercise, equipment: List<Equipment>, highlight: String? = null) {
    val items = exercise.equipment.mapNotNull { id -> equipment.find { it.id == id } }
    if (items.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (q in items.take(3)) {
            EquipmentIcon(q.icon, tileSize = 26.dp, iconSize = 16.dp, tile = C.Surface, tint = if (q.id == highlight) C.Lime else C.Muted, corner = 8.dp)
        }
        if (items.size > 3) Text("+${items.size - 3}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Muted, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

/** Filter row: Any + every piece of equipment that some of these exercises use. null = any. */
@Composable
fun EquipmentFilterRow(
    equipment: List<Equipment>,
    exercises: List<Exercise>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    val shown = equipment.filter { q -> q.id == selected || exercises.any { q.id in it.equipment } }
    if (shown.isEmpty()) return
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EquipmentChip("Any", UiIcons.FILTER, selected == null, null) { onSelect(null) }
        for (q in shown) {
            EquipmentChip(q.name, EquipmentIcons.path(q.icon), selected == q.id, exercises.count { q.id in it.equipment }) {
                onSelect(if (selected == q.id) null else q.id)
            }
        }
    }
}

@Composable
private fun EquipmentChip(label: String, iconPath: String, selected: Boolean, count: Int?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) C.LimeDim else C.Surface)
            .border(1.5.dp, if (selected) C.Lime else C.Line, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PathIcon(iconPath, if (selected || count != null) C.Lime else C.Muted, size = 16.dp, strokeWidth = 2.2f)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) C.Lime else C.Text, maxLines = 1, softWrap = false)
        if (selected && count != null) {
            Spacer(Modifier.width(6.dp))
            PathIcon(UiIcons.CLOSE, C.Lime, size = 14.dp, strokeWidth = 2.6f)
        } else if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text("$count", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Muted, maxLines = 1)
        }
    }
}

// ---------- exercise editor: chosen categories / equipment ----------

/** A card with a title, an "Add" button and the chosen items as compact rows. */
@Composable
fun ChosenSection(title: String, count: Int, addLabel: String, onAdd: () -> Unit, emptyText: String, rows: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(C.Surface)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (count > 0) "$title · $count" else title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(C.LimeDim)
                    .clickable(role = Role.Button, onClickLabel = addLabel, onClick = onAdd)
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PathIcon(UiIcons.PLUS, C.Lime, size = 16.dp, strokeWidth = 2.6f)
                Spacer(Modifier.width(4.dp))
                Text("Add", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = C.Lime)
            }
        }
        if (count == 0) {
            Text(emptyText, fontSize = 14.sp, color = C.Faint, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
        } else {
            rows()
        }
    }
}

@Composable
fun ChosenRow(name: String, onClick: () -> Unit, clickLabel: String, leading: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        PathIcon(UiIcons.FORWARD, C.Muted, size = 20.dp, strokeWidth = 2.2f)
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
fun CategoryDotTile(color: String) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(C.Surface2),
        contentAlignment = Alignment.Center,
    ) {
        ColorDot(categoryColor(color), 10.dp)
    }
}

// ---------- pickers ----------

@Composable
private fun PickerNewRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, C.Faint, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.LimeDim),
            contentAlignment = Alignment.Center,
        ) { PathIcon(UiIcons.PLUS, C.Lime, size = 20.dp, strokeWidth = 2.6f) }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Lime)
    }
}

@Composable
private fun PickerRow(name: String, checked: Boolean, onToggle: (Boolean) -> Unit, leading: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(C.Surface2)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onToggle)
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = C.Lime, checkmarkColor = C.OnLime, uncheckedColor = C.Muted),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    title: String,
    selectedCount: Int,
    query: String,
    onQuery: (String) -> Unit,
    searchHint: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = C.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontFamily = Condensed, fontSize = 30.sp, color = C.Text, modifier = Modifier.weight(1f))
                if (selectedCount > 0) Text("$selectedCount selected", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = C.Muted)
            }
            Spacer(Modifier.height(12.dp))
            SearchField(query, onChange = onQuery, background = C.Surface2, hint = searchHint)
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content,
            )
            Spacer(Modifier.height(12.dp))
            BigButton("Done", onClick = onDone, icon = UiIcons.CHECK, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** All equipment with check boxes, a search and "New equipment". */
@Composable
fun EquipmentPickerSheet(data: AppData, initial: List<String>, onDismiss: () -> Unit, onDone: (List<String>) -> Unit) {
    val chosen = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    val q = query.trim()
    val shown = data.equipment.filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }.sortedBy { it.name.lowercase() }
    PickerSheet(
        title = "Equipment",
        selectedCount = chosen.size,
        query = query,
        onQuery = { query = it },
        searchHint = "Search equipment",
        onDismiss = onDismiss,
        onDone = { onDone(chosen.toList()) },
    ) {
        item(key = "new") { PickerNewRow("New equipment") { creating = true } }
        if (shown.isEmpty() && q.isNotEmpty()) {
            item(key = "none") { Text("No equipment matches “$q”.", fontSize = 14.sp, color = C.Muted, modifier = Modifier.padding(8.dp)) }
        }
        items(shown, key = { it.id }) { item ->
            PickerRow(item.name, item.id in chosen, { on -> if (on) chosen.add(item.id) else chosen.remove(item.id) }) {
                EquipmentIcon(item.icon, tileSize = 38.dp, iconSize = 22.dp, tile = C.Surface3)
            }
        }
    }
    if (creating) {
        EquipmentFormSheet(
            initialName = q,
            onDismiss = { creating = false },
            onCreate = { name, icon ->
                val e = Repo.addEquipment(name, icon)
                chosen.add(e.id)
                query = ""
                creating = false
            },
        )
    }
}

/** All categories with check boxes, a search and "New category". */
@Composable
fun CategoryPickerSheet(data: AppData, initial: List<String>, onDismiss: () -> Unit, onDone: (List<String>) -> Unit) {
    val chosen = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    val q = query.trim()
    val shown = data.categories.filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
    PickerSheet(
        title = "Categories",
        selectedCount = chosen.size,
        query = query,
        onQuery = { query = it },
        searchHint = "Search categories",
        onDismiss = onDismiss,
        onDone = { onDone(chosen.toList()) },
    ) {
        item(key = "new") { PickerNewRow("New category") { creating = true } }
        if (shown.isEmpty() && q.isNotEmpty()) {
            item(key = "none") { Text("No category matches “$q”.", fontSize = 14.sp, color = C.Muted, modifier = Modifier.padding(8.dp)) }
        }
        items(shown, key = { it.id }) { c ->
            PickerRow(c.name, c.id in chosen, { on -> if (on) chosen.add(c.id) else chosen.remove(c.id) }) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { ColorDot(categoryColor(c.color), 12.dp) }
            }
        }
    }
    if (creating) {
        CategoryEditDialog(
            title = "New category",
            initialName = q,
            initialColor = Repo.nextCategoryColor(),
            exceptId = null,
            confirmLabel = "Create",
            onDismiss = { creating = false },
            onConfirm = { name, color ->
                val c = Repo.addCategory(name, color)
                chosen.add(c.id)
                query = ""
                creating = false
            },
        )
    }
}

// ---------- equipment form ----------

@Composable
private fun EquipmentIconGrid(selected: String, columns: Int, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in EquipmentIcons.all.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((key, path) in row) {
                    val on = key == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) C.LimeDim else C.Surface2)
                            .border(2.dp, if (on) C.Lime else Color.Transparent, RoundedCornerShape(16.dp))
                            .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(key) })
                            .semantics { contentDescription = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        PathIcon(path, C.Lime, size = 28.dp, strokeWidth = 1.7f)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun EquipmentNameField(name: String, onName: (String) -> Unit, error: String?) {
    OutlinedTextField(
        value = name,
        onValueChange = { onName(it.take(MAX_EQUIPMENT_NAME)) },
        label = { Text("Name") },
        singleLine = true,
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.Lime, focusedLabelColor = C.Lime, cursorColor = C.Lime, unfocusedBorderColor = C.Line),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** "New equipment": name and icon. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentFormSheet(initialName: String = "", onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(EquipmentIcons.DEFAULT) }
    val trimmed = name.trim()
    val duplicate = trimmed.isNotEmpty() && Repo.equipmentNameExists(trimmed, null)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = C.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("New equipment", fontFamily = Condensed, fontSize = 30.sp, color = C.Text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                EquipmentIcon(icon, tileSize = 60.dp, iconSize = 34.dp, tile = C.Surface2, corner = 18.dp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) { EquipmentNameField(name, { name = it }, if (duplicate) "This equipment already exists" else null) }
            }
            Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted)
            EquipmentIconGrid(icon, 4) { icon = it }
            BigButton(
                "Create",
                onClick = { onCreate(trimmed, icon) },
                icon = UiIcons.CHECK,
                enabled = trimmed.isNotEmpty() && !duplicate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------- categories: edit dialog + settings screen ----------

/** Name and colour of a category (new or existing). */
@Composable
fun CategoryEditDialog(
    title: String,
    initialName: String,
    initialColor: String,
    exceptId: String?,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    val trimmed = name.trim()
    val duplicate = trimmed.isNotEmpty() && Repo.categoryNameExists(trimmed, exceptId)
    val ok = trimmed.isNotEmpty() && !duplicate
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text(title, color = C.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = { if (duplicate) Text("This category already exists") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.Lime, focusedLabelColor = C.Lime, cursorColor = C.Lime),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Colour", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (hex in CategoryColors.palette) {
                        val sel = hex == color
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (sel) C.Text else Color.Transparent, CircleShape)
                                .selectable(selected = sel, role = Role.RadioButton) { color = hex }
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(categoryColor(hex)),
                        )
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        PathIcon(UiIcons.TRASH, C.Red, size = 18.dp, strokeWidth = 2f)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete category", color = C.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed, color) }, enabled = ok) {
                Text(confirmLabel, color = if (ok) C.Lime else C.Muted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = C.Text) } },
    )
}

@Composable
private fun ManageRow(title: String, subtitle: String, onClick: () -> Unit, trailingIcon: String, leading: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
            Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
        }
        PathIcon(trailingIcon, C.Muted, size = 20.dp, strokeWidth = 2.2f)
    }
}

/** Settings → Categories: rename, recolour, reorder, delete, create. */
@Composable
fun CategoriesScreen(data: AppData, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar("Categories", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (data.categories.size > 1) "Tap to rename or change the colour. Hold ⠿ and drag to reorder." else "Tap to rename or change the colour.",
                fontSize = 13.sp,
                color = C.Muted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
            if (data.categories.isEmpty()) EmptyState("No categories yet", "Create the first one below.")
            DraggableColumn(
                items = data.categories,
                keyOf = { it.id },
                onMove = { from, to -> Repo.moveCategory(from, to) },
                spacing = 8.dp,
            ) { c, _, handle ->
                Row(
                    handle
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.Surface)
                        .clickable(role = Role.Button) { editing = c }
                        .padding(start = 6.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PathIcon(UiIcons.GRIP, C.Faint, size = 18.dp, strokeWidth = 3f)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(categoryColor(c.color).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) { ColorDot(categoryColor(c.color), 12.dp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                        Text(countLabel(data.exercises.count { c.id in it.categories }), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
                    }
                    PathIcon(UiIcons.EDIT, C.Muted, size = 20.dp, strokeWidth = 2f)
                }
            }
            Text(
                "Deleting a category keeps its exercises — they just lose that category.",
                fontSize = 12.sp,
                color = C.Muted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
            BigButton("New category", onClick = { creating = true }, icon = UiIcons.PLUS, modifier = Modifier.fillMaxWidth())
        }
    }
    if (creating) {
        CategoryEditDialog(
            title = "New category",
            initialName = "",
            initialColor = Repo.nextCategoryColor(),
            exceptId = null,
            confirmLabel = "Create",
            onDismiss = { creating = false },
            onConfirm = { name, color ->
                Repo.addCategory(name, color)
                creating = false
            },
        )
    }
    editing?.let { c ->
        CategoryEditDialog(
            title = "Edit category",
            initialName = c.name,
            initialColor = c.color,
            exceptId = c.id,
            confirmLabel = "Save",
            onDismiss = { editing = null },
            onConfirm = { name, color ->
                Repo.renameCategory(c.id, name)
                Repo.setCategoryColor(c.id, color)
                editing = null
            },
            onDelete = {
                editing = null
                deleting = c
            },
        )
    }
    deleting?.let { c ->
        ConfirmDialog(
            title = "Delete “${c.name}”?",
            text = "Its exercises stay in your library.",
            confirmLabel = "Delete",
            onConfirm = {
                Repo.deleteCategory(c.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

// ---------- equipment: settings list + editor ----------

/** Settings → Equipment. Tap an item to edit it. */
@Composable
fun EquipmentListScreen(data: AppData, onBack: () -> Unit, onEdit: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar("Equipment", onBack = onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "hint") {
                Text(
                    "Tap an item to edit it and see the exercises that use it.",
                    fontSize = 13.sp,
                    color = C.Muted,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
            if (data.equipment.isEmpty()) item(key = "empty") { EmptyState("No equipment yet", "Create the first item below.") }
            items(data.equipment.sortedBy { it.name.lowercase() }, key = { it.id }) { q ->
                ManageRow(
                    title = q.name,
                    subtitle = countLabel(data.exercises.count { q.id in it.equipment }),
                    onClick = { onEdit(q.id) },
                    trailingIcon = UiIcons.FORWARD,
                    leading = { EquipmentIcon(q.icon, tileSize = 40.dp, iconSize = 22.dp, tile = C.Surface2, corner = 12.dp) },
                )
            }
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
            BigButton("New equipment", onClick = { creating = true }, icon = UiIcons.PLUS, modifier = Modifier.fillMaxWidth())
        }
    }
    if (creating) {
        EquipmentFormSheet(
            onDismiss = { creating = false },
            onCreate = { name, icon ->
                Repo.addEquipment(name, icon)
                creating = false
            },
        )
    }
}

/** Edit one piece of equipment: name, icon, the exercises that use it, delete. */
@Composable
fun EquipmentEditorScreen(item: Equipment?, data: AppData, onBack: () -> Unit, onOpenExercise: (String) -> Unit) {
    if (item == null) {
        // Deleted meanwhile.
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var icon by rememberSaveable(item.id) { mutableStateOf(item.icon) }
    var deleting by remember { mutableStateOf(false) }
    val trimmed = name.trim()
    val duplicate = trimmed.isNotEmpty() && Repo.equipmentNameExists(trimmed, item.id)
    val users = data.exercises.filter { item.id in it.equipment }.sortedBy { it.name.lowercase() }
    fun save() {
        if (trimmed.isEmpty() || duplicate) return
        Repo.saveEquipment(item.copy(name = trimmed, icon = icon))
        onBack()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar("Edit equipment", onBack = onBack) {
            RoundIconButton(UiIcons.TRASH, "Delete equipment", { deleting = true }, tint = C.Red, size = 48.dp, strokeWidth = 2f)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.border(2.dp, C.Lime, RoundedCornerShape(20.dp))) {
                    EquipmentIcon(icon, tileSize = 64.dp, iconSize = 36.dp, tile = C.Surface, corner = 20.dp)
                }
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f)) {
                    EquipmentNameField(name, { name = it }, when {
                        trimmed.isEmpty() -> "Enter a name"
                        duplicate -> "This equipment already exists"
                        else -> null
                    })
                }
            }
            Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted)
            EquipmentIconGrid(icon, 6) { icon = it }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.Surface)
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            ) {
                Text(
                    if (users.isEmpty()) "Not used yet" else "Used in ${countLabel(users.size)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                if (users.isEmpty()) {
                    Text("Add it to an exercise in the exercise’s settings.", fontSize = 14.sp, color = C.Faint, modifier = Modifier.padding(vertical = 6.dp))
                }
                for (e in users) {
                    ChosenRow(e.name, onClick = { onOpenExercise(e.id) }, clickLabel = "Open exercise") {
                        ExerciseIcon(e.icon, tileSize = 36.dp, iconSize = 20.dp, tile = C.Surface2, corner = 10.dp)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
            BigButton("Save changes", onClick = { save() }, icon = UiIcons.CHECK, enabled = trimmed.isNotEmpty() && !duplicate, modifier = Modifier.fillMaxWidth())
        }
    }
    if (deleting) {
        ConfirmDialog(
            title = "Delete “${item.name}”?",
            text = if (users.isEmpty()) "It isn’t used by any exercise." else "It will be removed from ${countLabel(users.size)}. The exercises stay.",
            confirmLabel = "Delete",
            onConfirm = {
                deleting = false
                Repo.deleteEquipment(item.id)
                onBack()
            },
            onDismiss = { deleting = false },
        )
    }
}

/** Settings → Library entry row. */
@Composable
fun LibraryEntry(title: String, subtitle: String, iconPath: String, tint: Color, tile: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tile),
            contentAlignment = Alignment.Center,
        ) { PathIcon(iconPath, tint, size = 24.dp, strokeWidth = 2f) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
            Text(subtitle, fontSize = 13.sp, color = C.Muted)
        }
        PathIcon(UiIcons.FORWARD, C.Muted, size = 22.dp, strokeWidth = 2.2f)
    }
}

/** Equipment the complex needs, each with how many of its exercises use it, in order of first use. */
fun AppData.equipmentNeeded(complex: com.trainingtimer.app.data.Complex): List<Pair<Equipment, Int>> {
    val exById = exercises.associateBy { it.id }
    val used = complex.exerciseIds().distinct().mapNotNull { exById[it] }
    val ids = used.flatMap { it.equipment }.distinct()
    return ids.mapNotNull { id -> equipment.find { it.id == id }?.let { q -> q to used.count { id in it.equipment } } }
}

/** Complex card: a small stack of equipment icons and "3 items needed". */
@Composable
fun NeededStack(items: List<Equipment>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row {
            items.take(3).forEachIndexed { i, q ->
                Box(
                    Modifier
                        .padding(start = if (i == 0) 0.dp else 0.dp)
                        .offset(x = (-6 * i).dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Surface)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(C.LimeDim),
                    contentAlignment = Alignment.Center,
                ) { PathIcon(EquipmentIcons.path(q.icon), C.Lime, size = 14.dp, strokeWidth = 2.2f) }
            }
        }
        val shift = (-6 * (minOf(items.size, 3) - 1)).dp
        Text(
            if (items.size == 1) "1 item needed" else "${items.size} items needed",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = C.Lime,
            modifier = Modifier.offset(x = shift).padding(start = 8.dp),
        )
    }
}
