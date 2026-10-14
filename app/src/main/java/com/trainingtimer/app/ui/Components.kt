package com.trainingtimer.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainingtimer.app.data.Exercise
import com.trainingtimer.app.data.ExerciseIcons
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.formatTime
import java.io.File

// ---------- icons ----------

private val pathCache = HashMap<String, Path>()

private fun cachedPath(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** Draws an icon from 24x24 path data. */
@Composable
fun PathIcon(
    path: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Float = 2f,
    filled: Boolean = false,
) {
    val p = remember(path) { cachedPath(path) }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            if (filled) {
                drawPath(p, color = tint)
            } else {
                drawPath(p, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private val bitmapCache = HashMap<String, ImageBitmap>()

private fun loadIconBitmap(icon: String): ImageBitmap? {
    bitmapCache[icon]?.let { return it }
    val file = File(Repo.iconsDir, icon.removePrefix("img:"))
    if (!file.exists()) return null
    val bmp = BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() ?: return null
    bitmapCache[icon] = bmp
    return bmp
}

/** Exercise icon on a rounded tile: a built-in path icon or the user's picture. */
@Composable
fun ExerciseIcon(
    icon: String,
    modifier: Modifier = Modifier,
    tileSize: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    tile: Color = C.Surface2,
    tint: Color = C.Lime,
    corner: Dp = 12.dp,
    strokeWidth: Float = 1.7f,
) {
    Box(
        modifier
            .size(tileSize)
            .clip(RoundedCornerShape(corner))
            .background(tile),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = if (icon.startsWith("img:")) remember(icon) { loadIconBitmap(icon) } else null
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            PathIcon(ExerciseIcons.path(icon), tint, size = iconSize, strokeWidth = strokeWidth)
        }
    }
}

// ---------- buttons ----------

@Composable
fun RoundIconButton(
    path: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Color.Transparent,
    tint: Color = C.Text,
    iconSize: Dp = 22.dp,
    filled: Boolean = false,
    strokeWidth: Float = 2.2f,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        PathIcon(path, tint, size = iconSize, filled = filled, strokeWidth = strokeWidth)
    }
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconFilled: Boolean = false,
    container: Color = C.Lime,
    content: Color = C.OnLime,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    fontSize: TextUnit = 16.sp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = C.Surface2,
            disabledContentColor = C.Muted,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            PathIcon(icon, LocalContentColor.current, size = 20.dp, strokeWidth = 2.6f, filled = iconFilled)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, maxLines = 1, softWrap = false)
    }
}

@Composable
fun PillButton(text: String, icon: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(C.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PathIcon(icon, C.Lime, size = 18.dp, strokeWidth = 2.2f)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Text, maxLines = 1, softWrap = false)
    }
}

// ---------- steppers ----------

@Composable
private fun StepButton(symbol: String, label: String, onClick: () -> Unit, width: Dp) {
    Box(
        Modifier
            .size(width, 44.dp)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = C.Text, fontSize = 22.sp)
    }
}

@Composable
fun Stepper(
    text: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = C.Text,
    onTextClick: (() -> Unit)? = null,
    buttonWidth: Dp = 34.dp,
    minTextWidth: Dp = 44.dp,
    fontSize: TextUnit = 22.sp,
    background: Color = C.Surface2,
    minusLabel: String = "Decrease",
    plusLabel: String = "Increase",
) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", minusLabel, onMinus, buttonWidth)
        val textModifier = if (onTextClick != null) Modifier.clickable(onClick = onTextClick) else Modifier
        Text(
            text,
            modifier = textModifier.widthIn(min = minTextWidth).padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontFamily = Condensed,
            fontSize = fontSize,
            color = textColor,
            maxLines = 1,
        )
        StepButton("+", plusLabel, onPlus, buttonWidth)
    }
}

/** −/+ stepper in 5 second steps; tapping the time opens an exact-time dialog. */
@Composable
fun TimeStepper(
    seconds: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 5,
    textColor: Color = C.Text,
    buttonWidth: Dp = 34.dp,
    background: Color = C.Surface2,
    title: String = "Set time",
) {
    var dialog by remember { mutableStateOf(false) }
    Stepper(
        text = formatTime(seconds),
        onMinus = { onChange((seconds - 5).coerceAtLeast(min)) },
        onPlus = { onChange((seconds + 5).coerceAtMost(3600)) },
        modifier = modifier,
        textColor = textColor,
        onTextClick = { dialog = true },
        buttonWidth = buttonWidth,
        background = background,
        minusLabel = "Shorter",
        plusLabel = "Longer",
    )
    if (dialog) {
        TimeDialog(title, seconds, min, onDismiss = { dialog = false }, onConfirm = {
            onChange(it)
            dialog = false
        })
    }
}

@Composable
fun TimeDialog(title: String, initial: Int, min: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var minutes by remember { mutableStateOf((initial / 60).toString()) }
    var secs by remember { mutableStateOf((initial % 60).toString()) }
    val total = (minutes.toIntOrNull() ?: 0) * 60 + (secs.toIntOrNull() ?: 0)
    val valid = total >= min && total <= 3600
    val minutesFocus = remember { FocusRequester() }
    val secondsFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { minutesFocus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text(title, color = C.Text) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectAllNumberField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = "min",
                    focusRequester = minutesFocus,
                    imeAction = ImeAction.Next,
                    onFilled = { secondsFocus.requestFocus() },
                    onAction = { secondsFocus.requestFocus() },
                    modifier = Modifier.weight(1f),
                )
                Text(":", fontSize = 28.sp, color = C.Text)
                SelectAllNumberField(
                    value = secs,
                    onValueChange = { secs = it },
                    label = "sec",
                    focusRequester = secondsFocus,
                    imeAction = ImeAction.Done,
                    onFilled = {},
                    onAction = { if (valid) onConfirm(total) },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(total.coerceIn(min, 3600)) }, enabled = valid) {
                Text("OK", color = if (valid) C.Lime else C.Muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = C.Text) }
        },
    )
}

/** Two-digit number field: its whole value is selected when it gets focus, so typing replaces it. */
@Composable
private fun SelectAllNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    focusRequester: FocusRequester,
    imeAction: ImeAction,
    onFilled: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(0, value.length))) }
    // The tap that focuses the field also moves the cursor; keep the full selection for that first event.
    var keepSelection by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = field,
        onValueChange = { next ->
            val digits = next.text.filter { it.isDigit() }.take(2)
            if (keepSelection && digits == field.text) {
                keepSelection = false
                field = field.copy(selection = TextRange(0, digits.length))
            } else {
                keepSelection = false
                val changed = digits != field.text
                field = if (digits == next.text) next else TextFieldValue(digits, TextRange(digits.length))
                if (changed) {
                    onValueChange(digits)
                    if (digits.length == 2) onFilled()
                }
            }
        },
        label = { Text(label) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 22.sp, color = C.Text),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onAction() }, onDone = { onAction() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.Lime,
            focusedLabelColor = C.Lime,
            cursorColor = C.Lime,
        ),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    keepSelection = true
                    field = field.copy(selection = TextRange(0, field.text.length))
                }
            },
    )
}

// ---------- layout pieces ----------

@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    backIcon: String = UiIcons.BACK,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(backIcon, backLabel, onBack, size = 48.dp, iconSize = 24.dp)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted, maxLines = 1)
            }
        }
        actions()
    }
}

@Composable
fun TabHeader(label: String, title: String, action: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Muted, letterSpacing = 1.2.sp, maxLines = 1)
            Text(title, fontFamily = Condensed, fontSize = 40.sp, color = C.Text, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.width(8.dp))
        action()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
        Text(
            text.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = C.Muted,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) Text(trailing, fontSize = 12.sp, color = C.Muted)
    }
}

@Composable
fun ProgressStrip(fraction: Float) {
    Box(
        Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(C.Surface2),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(C.Lime),
        )
    }
}

@Composable
fun ActionBar(content: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.NavBg)) {
        HorizontalDivider(color = C.Surface2)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, textAlign = TextAlign.Center)
        Text(text, fontSize = 14.sp, color = C.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        titleContentColor = C.Text,
        textContentColor = C.Muted,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) C.Red else C.Lime, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = C.Text) }
        },
    )
}

/** Small "⋮" button with a dropdown menu. */
@Composable
fun OverflowMenu(label: String, entries: List<Pair<String, () -> Unit>>, size: Dp = 40.dp) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIconButton(UiIcons.MORE, label, { open = true }, size = size, tint = C.Muted, iconSize = 20.dp, strokeWidth = 2.8f)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for ((text, action) in entries) {
                DropdownMenuItem(
                    text = { Text(text, color = C.Text) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}

/** A selectable exercise row with a checkbox, used in pickers. */
@Composable
fun CheckRow(
    exercise: Exercise,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    background: Color = C.Surface,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (checked) C.LimeBg else background)
            .border(1.5.dp, if (checked) C.LimeBorder else Color.Transparent, shape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onToggle)
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseIcon(exercise.icon, tileSize = 44.dp, iconSize = 24.dp, tile = C.Surface2)
        Spacer(Modifier.width(12.dp))
        Text(exercise.name, modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Text)
        trailing()
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = C.Lime, checkmarkColor = C.OnLime, uncheckedColor = C.Muted),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
