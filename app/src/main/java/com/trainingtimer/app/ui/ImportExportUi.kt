package com.trainingtimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.ImportResult
import com.trainingtimer.app.data.UiIcons

sealed class ImportOutcome {
    data class Done(val result: ImportResult, val fileName: String?) : ImportOutcome()
    data class Failed(val message: String) : ImportOutcome()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportSheet(
    exerciseCount: Int,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onExportExercises: () -> Unit,
    onExportBackup: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = C.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Import & export", fontFamily = Condensed, fontSize = 30.sp, color = C.Text)
            SheetOption(
                title = "Import exercises",
                subtitle = "Choose a .json file from your phone",
                icon = UiIcons.DOWNLOAD,
                highlighted = true,
                onClick = onImport,
            )
            SheetOption(
                title = "Export exercises",
                subtitle = "Save or share all $exerciseCount exercises as .json",
                icon = UiIcons.UPLOAD,
                onClick = onExportExercises,
            )
            SheetOption(
                title = "Full backup",
                subtitle = "Exercises and complexes in one .json file",
                icon = UiIcons.BACKUP,
                onClick = onExportBackup,
            )
            Text(
                "Exercises whose name already exists in your library are skipped. You'll see a summary after import.",
                fontSize = 13.sp,
                color = C.Muted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SheetOption(title: String, subtitle: String, icon: String, onClick: () -> Unit, highlighted: Boolean = false) {
    val bg = if (highlighted) C.Lime else C.Surface2
    val fg = if (highlighted) C.OnLime else C.Text
    val sub = if (highlighted) Color(0xFF2F3A12) else C.Muted
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (highlighted) Color(0x1F12140F) else C.Surface3),
            contentAlignment = Alignment.Center,
        ) {
            PathIcon(icon, if (highlighted) C.OnLime else C.Lime, size = 26.dp, strokeWidth = 2.2f)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = fg)
            Text(subtitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = sub)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportResultDialog(outcome: ImportOutcome, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = C.Lime, fontWeight = FontWeight.ExtraBold) }
        },
        icon = {
            val failed = outcome is ImportOutcome.Failed
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (failed) Color(0xFF3A1F1F) else C.LimeDim),
                contentAlignment = Alignment.Center,
            ) {
                PathIcon(if (failed) UiIcons.WARNING else UiIcons.CHECK, if (failed) C.Red else C.Lime, size = 28.dp, strokeWidth = 2.6f)
            }
        },
        title = {
            Text(
                if (outcome is ImportOutcome.Failed) "Import failed" else "Import complete",
                fontFamily = Condensed,
                fontSize = 30.sp,
                color = C.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            when (outcome) {
                is ImportOutcome.Failed -> Text(outcome.message, fontSize = 15.sp, color = C.Text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                is ImportOutcome.Done -> ImportSummary(outcome)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportSummary(outcome: ImportOutcome.Done) {
    val r = outcome.result
    Column(
        Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val n = r.addedExercises.size
        val loaded = buildString {
            append("Successfully loaded ")
            append(if (n == 1) "1 exercise" else "$n exercises")
            if (r.addedComplexes.isNotEmpty()) {
                append(" and ")
                append(if (r.addedComplexes.size == 1) "1 complex" else "${r.addedComplexes.size} complexes")
            }
            if (outcome.fileName != null) append(" from ${outcome.fileName}")
            append(".")
        }
        Text(loaded, fontSize = 16.sp, color = C.Text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        if (r.skippedExercises.isNotEmpty()) {
            SummaryBlock(
                icon = UiIcons.SKIPPED,
                iconColor = C.Orange,
                title = "Skipped ${r.skippedExercises.size} — already in your library",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (name in r.skippedExercises) Chip(name)
                }
            }
        }
        if (r.skippedComplexes.isNotEmpty()) {
            SummaryBlock(
                icon = UiIcons.SKIPPED,
                iconColor = C.Orange,
                title = "Skipped ${r.skippedComplexes.size} complex${if (r.skippedComplexes.size == 1) "" else "es"} with existing names",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (name in r.skippedComplexes) Chip(name)
                }
            }
        }
        if (r.errors.isNotEmpty()) {
            SummaryBlock(
                icon = UiIcons.WARNING,
                iconColor = C.Red,
                title = if (r.errors.size == 1) "1 entry couldn't be read" else "${r.errors.size} entries couldn't be read",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (e in r.errors.take(20)) Text(e, fontSize = 13.sp, color = C.Muted)
                    if (r.errors.size > 20) Text("…and ${r.errors.size - 20} more", fontSize = 13.sp, color = C.Muted)
                }
            }
        }
    }
}

@Composable
private fun SummaryBlock(icon: String, iconColor: Color, title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface2)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PathIcon(icon, iconColor, size = 18.dp, strokeWidth = 2.2f)
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
        }
        content()
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(C.Surface3)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Text)
    }
}
