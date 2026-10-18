package com.trainingtimer.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import com.trainingtimer.app.data.Category
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.Exercise
import com.trainingtimer.app.data.ExerciseIcons
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.newId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditorScreen(existing: Exercise?, categories: List<Category>, onClose: () -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "plank") }
    var selectedCats by remember { mutableStateOf(existing?.categories ?: emptyList()) }
    var newCategory by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val saved = Repo.saveIconFromUri(uri)
            if (saved != null) {
                icon = saved
            } else {
                Toast.makeText(context, "Couldn't use this picture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun save() {
        val n = name.trim()
        when {
            n.isEmpty() -> error = "Enter a name"
            Repo.nameExists(n, existing?.id) -> error = "An exercise with this name already exists"
            else -> {
                Repo.saveExercise(Exercise(existing?.id ?: newId(), n, icon, selectedCats.filter { id -> categories.any { it.id == id } }))
                onClose()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        TopBar(
            title = if (existing == null) "New exercise" else "Edit exercise",
            onBack = onClose,
            backIcon = UiIcons.CLOSE,
            backLabel = "Close",
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                ExerciseIcon(
                    icon,
                    modifier = Modifier.border(2.dp, C.Lime, RoundedCornerShape(30.dp)),
                    tileSize = 104.dp,
                    iconSize = 56.dp,
                    tile = C.Surface,
                    corner = 30.dp,
                    strokeWidth = 1.5f,
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text("Name") },
                singleLine = true,
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Lime,
                    focusedLabelColor = C.Lime,
                    cursorColor = C.Lime,
                    unfocusedBorderColor = C.Line,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Categories", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in categories) {
                    val on = c.id in selectedCats
                    FilterChip(
                        c.name,
                        on,
                        { selectedCats = if (on) selectedCats - c.id else selectedCats + c.id },
                        color = categoryColor(c.color),
                    )
                }
                Row(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.5.dp, C.LimeBorder, RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) { newCategory = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PathIcon(UiIcons.PLUS, C.Lime, size = 16.dp, strokeWidth = 2.6f)
                    Spacer(Modifier.width(6.dp))
                    Text("New category", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = C.Lime)
                }
            }
            Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted)
            // A plain (non-lazy) grid so the whole page scrolls together.
            val tiles = buildList<@Composable () -> Unit> {
                if (icon.startsWith("img:")) {
                    add {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .border(2.dp, C.Lime, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            ExerciseIcon(icon, tileSize = 56.dp, corner = 16.dp)
                        }
                    }
                }
                for (def in ExerciseIcons.all.filter { it.key != ExerciseIcons.UNKNOWN }) {
                    add {
                        val selected = icon == def.key
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) C.Lime else C.Surface)
                                .selectable(selected = selected, role = Role.RadioButton, onClick = { icon = def.key })
                                .semantics { contentDescription = def.label },
                            contentAlignment = Alignment.Center,
                        ) {
                            PathIcon(def.path, if (selected) C.OnLime else C.Lime, size = 28.dp, strokeWidth = 1.7f)
                        }
                    }
                }
                add {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, C.Faint, RoundedCornerShape(16.dp))
                            .background(Color.Transparent)
                            .selectable(selected = false, role = Role.Button, onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            })
                            .semantics { contentDescription = "Choose a picture from the gallery" },
                        contentAlignment = Alignment.Center,
                    ) {
                        PathIcon(UiIcons.IMAGE, C.Muted, size = 26.dp, strokeWidth = 1.8f)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in tiles.chunked(5)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (tile in row) {
                            Box(Modifier.weight(1f).aspectRatio(1f)) { tile() }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
            BigButton(
                text = if (existing == null) "Save exercise" else "Save changes",
                onClick = { save() },
                icon = UiIcons.CHECK,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (newCategory) {
        CategoryNameDialog(
            title = "New category",
            initial = "",
            confirmLabel = "Add",
            exceptId = null,
            onDismiss = { newCategory = false },
            onConfirm = { name ->
                val c = Repo.addCategory(name, Repo.nextCategoryColor())
                selectedCats = selectedCats + c.id
                newCategory = false
            },
        )
    }
}
