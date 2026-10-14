package com.trainingtimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.AppData
import com.trainingtimer.app.data.Category
import com.trainingtimer.app.data.CategoryColors
import com.trainingtimer.app.data.Exercise
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.UiIcons

/** Filter value for exercises without any category. null means "All". */
const val NO_CATEGORY = "__none__"

fun categoryColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    C.Muted
}

fun List<Exercise>.inCategory(category: String?): List<Exercise> = when (category) {
    null -> this
    NO_CATEGORY -> this.filter { it.categories.isEmpty() }
    else -> this.filter { category in it.categories }
}

@Composable
fun ColorDot(color: Color, size: Dp = 8.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color? = null,
    count: Int? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) C.Lime else C.Surface)
            .border(1.5.dp, if (selected) C.Lime else C.Line, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (color != null) {
            ColorDot(color)
            Spacer(Modifier.width(7.dp))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) C.OnLime else C.Text, maxLines = 1, softWrap = false)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text("$count", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) C.OnLime.copy(alpha = 0.65f) else C.Muted, maxLines = 1)
        }
    }
}

/**
 * Horizontal row of category filters: All, every category, and "No category" when some exercises have none.
 * [onManage] shows a settings button at the start.
 */
@Composable
fun CategoryFilterRow(
    categories: List<Category>,
    exercises: List<Exercise>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onManage: (() -> Unit)? = null,
    horizontalPadding: Dp = 16.dp,
) {
    if (categories.isEmpty() && onManage == null) return
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onManage != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(C.Surface)
                    .border(1.5.dp, C.Line, CircleShape)
                    .clickable(role = Role.Button, onClick = onManage)
                    .semantics { contentDescription = "Manage categories" },
                contentAlignment = Alignment.Center,
            ) {
                PathIcon(UiIcons.SETTINGS, C.Lime, size = 18.dp, strokeWidth = 2.2f)
            }
        }
        FilterChip("All", selected == null, { onSelect(null) }, count = exercises.size)
        for (c in categories) {
            FilterChip(
                c.name,
                selected == c.id,
                { onSelect(c.id) },
                color = categoryColor(c.color),
                count = exercises.count { c.id in it.categories },
            )
        }
        val none = exercises.count { it.categories.isEmpty() }
        if (none > 0 && categories.isNotEmpty()) {
            FilterChip("No category", selected == NO_CATEGORY, { onSelect(NO_CATEGORY) }, color = C.Faint, count = none)
        }
    }
}

/** Small line under an exercise name: coloured dots and category names. */
@Composable
fun CategoryLine(exercise: Exercise, categories: List<Category>) {
    val cats = exercise.categories.mapNotNull { id -> categories.find { it.id == id } }
    if (cats.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (c in cats) ColorDot(categoryColor(c.color), 7.dp)
        Text(cats.joinToString(" · ") { it.name }, fontSize = 12.sp, color = C.Muted, maxLines = 1)
    }
}

/** Dialog to type a category name (new or rename). */
@Composable
fun CategoryNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    exceptId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val trimmed = name.trim()
    val duplicate = trimmed.isNotEmpty() && Repo.categoryNameExists(trimmed, exceptId)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text(title, color = C.Text) },
        text = {
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
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed) }, enabled = trimmed.isNotEmpty() && !duplicate) {
                Text(confirmLabel, color = if (trimmed.isNotEmpty() && !duplicate) C.Lime else C.Muted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = C.Text) } },
    )
}

/** Bottom sheet to add, rename, recolour, reorder and delete categories. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerSheet(data: AppData, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newName by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf(Repo.nextCategoryColor()) }
    var renaming by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    val trimmed = newName.trim()
    val duplicate = trimmed.isNotEmpty() && Repo.categoryNameExists(trimmed, null)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = C.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Categories", fontFamily = Condensed, fontSize = 30.sp, color = C.Text, modifier = Modifier.weight(1f))
                if (data.categories.size > 1) Text("Hold ⠿ and drag to reorder", fontSize = 12.sp, color = C.Muted)
            }
            if (data.categories.isEmpty()) {
                Text("No categories yet. Add the first one below.", fontSize = 14.sp, color = C.Muted)
            }
            DraggableColumn(
                items = data.categories,
                keyOf = { it.id },
                onMove = { from, to -> Repo.moveCategory(from, to) },
                spacing = 6.dp,
            ) { c, _, handle ->
                val count = data.exercises.count { c.id in it.categories }
                Row(
                    handle
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.Surface2)
                        .padding(start = 4.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PathIcon(UiIcons.GRIP, C.Faint, size = 18.dp, strokeWidth = 3f)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(44.dp)
                            .clickable(role = Role.Button) {
                                val p = CategoryColors.palette
                                val i = p.indexOf(c.color)
                                Repo.setCategoryColor(c.id, p[(i + 1).mod(p.size)])
                            }
                            .semantics { contentDescription = "Change colour" },
                        contentAlignment = Alignment.Center,
                    ) {
                        ColorDot(categoryColor(c.color), 24.dp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
                        Text(if (count == 1) "1 exercise" else "$count exercises", fontSize = 12.sp, color = C.Muted)
                    }
                    OverflowMenu(
                        "Category options",
                        listOf("Rename" to { renaming = c }, "Delete" to { deleting = c }),
                    )
                }
            }
            Text("New category", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted, modifier = Modifier.padding(top = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("e.g. Кардио") },
                    singleLine = true,
                    isError = duplicate,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.Lime, cursorColor = C.Lime, unfocusedBorderColor = C.Line),
                    modifier = Modifier.weight(1f),
                )
                BigButton(
                    "Add",
                    onClick = {
                        Repo.addCategory(trimmed, newColor)
                        newName = ""
                        newColor = Repo.nextCategoryColor()
                    },
                    enabled = trimmed.isNotEmpty() && !duplicate,
                    height = 52.dp,
                )
            }
            if (duplicate) Text("This category already exists", fontSize = 12.sp, color = C.Red)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (hex in CategoryColors.palette) {
                    val sel = hex == newColor
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (sel) C.Text else Color.Transparent, CircleShape)
                            .clickable(role = Role.RadioButton) { newColor = hex }
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(categoryColor(hex)),
                    )
                }
            }
            Text(
                "Deleting a category keeps its exercises — they just lose that category.",
                fontSize = 12.sp,
                color = C.Muted,
            )
        }
    }

    renaming?.let { c ->
        CategoryNameDialog(
            title = "Rename category",
            initial = c.name,
            confirmLabel = "Save",
            exceptId = c.id,
            onDismiss = { renaming = null },
            onConfirm = {
                Repo.renameCategory(c.id, it)
                renaming = null
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
