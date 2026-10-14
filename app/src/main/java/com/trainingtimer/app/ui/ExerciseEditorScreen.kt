package com.trainingtimer.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun ExerciseEditorScreen(existing: Exercise?, onClose: () -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "plank") }
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
                Repo.saveExercise(Exercise(existing?.id ?: newId(), n, icon))
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
                .padding(horizontal = 20.dp),
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
            Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Muted)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (icon.startsWith("img:")) {
                    item {
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .border(2.dp, C.Lime, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            ExerciseIcon(icon, tileSize = 56.dp, corner = 16.dp)
                        }
                    }
                }
                items(ExerciseIcons.all, key = { it.key }) { def ->
                    val selected = icon == def.key
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) C.Lime else C.Surface)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { icon = def.key })
                            .semantics { contentDescription = def.label },
                        contentAlignment = Alignment.Center,
                    ) {
                        PathIcon(def.path, if (selected) C.OnLime else C.Lime, size = 28.dp, strokeWidth = 1.7f)
                    }
                }
                item {
                    Box(
                        Modifier
                            .aspectRatio(1f)
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
}
