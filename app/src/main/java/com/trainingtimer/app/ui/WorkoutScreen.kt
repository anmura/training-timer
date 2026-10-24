package com.trainingtimer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.trainingtimer.app.data.UiIcons
import com.trainingtimer.app.data.Repo
import com.trainingtimer.app.data.formatTime
import com.trainingtimer.app.data.formatWeight
import com.trainingtimer.app.workout.Phase
import com.trainingtimer.app.workout.PhaseType
import com.trainingtimer.app.workout.WorkoutEngine
import com.trainingtimer.app.workout.WorkoutState

@Composable
fun WorkoutScreen(onExit: () -> Unit) {
    val state by WorkoutEngine.state.collectAsStateWithLifecycle()
    var confirmEnd by remember { mutableStateOf(false) }
    KeepScreenOn()

    val s = state
    BackHandler {
        if (s == null || s.finished) {
            WorkoutEngine.stop()
            onExit()
        } else {
            confirmEnd = true
        }
    }

    if (s == null) {
        LaunchedEffect(Unit) { onExit() }
        Box(Modifier.fillMaxSize().background(C.Bg))
        return
    }

    when {
        s.finished -> FinishedView(s, onDone = {
            WorkoutEngine.stop()
            onExit()
        })
        s.phase.type == PhaseType.WORK -> WorkView(s, onClose = { confirmEnd = true })
        else -> RestView(s, onClose = { confirmEnd = true })
    }

    if (confirmEnd) {
        ConfirmDialog(
            title = "End workout?",
            text = "The timer will stop and this session ends.",
            confirmLabel = "End",
            onConfirm = {
                confirmEnd = false
                WorkoutEngine.stop()
                onExit()
            },
            onDismiss = { confirmEnd = false },
        )
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun WorkoutHeader(
    s: WorkoutState,
    onClose: () -> Unit,
    muted: Color,
    badgeBg: Color,
    badgeFg: Color,
    flash: Boolean = false,
) {
    val p = s.phase
    val badgeScale by animateFloatAsState(if (flash) 1.15f else 1f, tween(300), label = "badgeScale")
    val bg by animateColorAsState(if (flash) C.Lime else badgeBg, tween(300), label = "badgeBg")
    val fg by animateColorAsState(if (flash) C.OnLime else badgeFg, tween(300), label = "badgeFg")
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(UiIcons.CLOSE, "End workout", onClose, size = 48.dp)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(s.complexName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val progress = if (p.type == PhaseType.WORK) {
                "Exercise ${p.workNumber}/${s.totalWork}"
            } else {
                "${p.workNumber - 1}/${s.totalWork} done"
            }
            val plus = if (s.repsSetsAhead > 0 || p.isReps) "+" else ""
            Text("$progress · ${formatTime(s.totalLeftSec)}$plus left", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = muted, maxLines = 1)
        }
        if (p.rounds > 0) {
            Row(
                Modifier
                    .padding(end = 8.dp)
                    .scale(badgeScale)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PathIcon(UiIcons.REPEAT, fg, size = 16.dp, strokeWidth = 2.2f)
                Spacer(Modifier.width(6.dp))
                Text("${p.round}/${p.rounds}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = fg, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SegmentBar(s: WorkoutState, color: Color) {
    val works = remember(s.phases) { s.phases.filter { it.type == PhaseType.WORK } }
    if (works.isEmpty()) return
    // 0-based index of the current (or, during rest, the upcoming) set; every set before it is done.
    val cur = (s.phase.workNumber - 1).coerceIn(0, works.lastIndex)
    val working = s.phase.type == PhaseType.WORK
    val curColor = if (s.phase.isReps) C.Sky else C.Text
    Canvas(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .height(14.dp),
    ) {
        val px = density
        val (units, widths) = progressUnits(works, cur, size.width / px)
        var x = 0f
        val xs = FloatArray(units.size)
        for (k in units.indices) {
            xs[k] = x
            x += widths[k] + if (k < units.lastIndex) unitGap(units[k], units[k + 1]) else 0f
        }
        val h = size.height
        // Cycles sit on a soft band; the band of the cycle you are in is highlighted.
        var k = 0
        while (k < units.size) {
            var e = k
            while (e + 1 < units.size && units[e + 1].block == units[k].block) e++
            if (units[k].cycle > 0) {
                val active = cur >= units[k].from && cur < units[e].to
                val left = (xs[k] - 4f) * px
                val right = (xs[e] + widths[e] + 4f) * px
                val r = androidx.compose.ui.geometry.CornerRadius(h / 2f)
                drawRoundRect(if (active) BandActive else BandIdle, Offset(left, 0f), Size(right - left, h), r)
                drawRoundRect(
                    if (active) BandActiveBorder else BandIdleBorder, Offset(left + 0.5f * px, 0.5f * px),
                    Size(right - left - px, h - px), r, style = Stroke(px),
                )
            }
            k = e + 1
        }
        for (j in units.indices) {
            val u = units[j]
            val uh = (if (u.cycle > 0) 6f else 4f) * px
            val top = (h - uh) / 2f
            val left = xs[j] * px
            val w = widths[j] * px
            val r = androidx.compose.ui.geometry.CornerRadius(uh / 2f)
            val n = u.to - u.from
            val done = (u.from until u.to).count { it < cur }
            val isCur = working && cur >= u.from && cur < u.to
            if (n == 1) {
                drawRoundRect(if (done == 1) color else if (isCur) curColor else C.Surface3, Offset(left, top), Size(w, uh), r)
            } else {
                // A whole round or block as one segment that fills up.
                val path = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(left, top, left + w, top + uh, r))
                }
                clipPath(path) {
                    drawRect(C.Surface3, Offset(left, top), Size(w, uh))
                    val dw = w * done / n
                    drawRect(color, Offset(left, top), Size(dw, uh))
                    if (isCur) drawRect(curColor, Offset(left + dw, top), Size(maxOf(w / n, 3f * px), uh))
                }
            }
        }
    }
}

private val BandActive = Color(0xFF1E2412)
private val BandActiveBorder = Color(0xFF6E8A22)
private val BandIdle = Color(0xFF181B1F)
private val BandIdleBorder = Color(0xFF2A2F34)

/** A piece of the progress line: one set, one round of a cycle, or a whole block. [to] is exclusive. */
private class BarUnit(val block: Int, val cycle: Int, val from: Int, val to: Int, val round: Int, val perSet: Boolean)

private fun unitGap(a: BarUnit, b: BarUnit): Float = when {
    a.block != b.block -> 10f
    a.cycle == 0 -> 4f
    a.perSet && b.perSet -> if (a.round != b.round) 6f else 2f
    else -> 3f
}

/**
 * Lays out the progress line ("magnifier"): if every set fits, each set is its own segment.
 * Otherwise the block you are in keeps per-set detail and gets at least 55% of the width,
 * while other cycles shrink to one segment per round (or one per cycle when even that is too thin).
 * Widths are in dp.
 */
private fun progressUnits(works: List<com.trainingtimer.app.workout.Phase>, cur: Int, width: Float): Pair<List<BarUnit>, FloatArray> {
    val n = works.size
    val blocks = ArrayList<IntRange>()
    var i = 0
    while (i < n) {
        var j = i
        while (j + 1 < n && works[j + 1].cycle == works[i].cycle) j++
        blocks += i..j
        i = j + 1
    }
    fun sets(b: Int) = blocks[b].map { BarUnit(b, works[it].cycle, it, it + 1, works[it].round, true) }
    fun rounds(b: Int): List<BarUnit> {
        val r = blocks[b]
        if (works[r.first].cycle == 0) return listOf(BarUnit(b, 0, r.first, r.last + 1, 0, false))
        val out = ArrayList<BarUnit>()
        var a = r.first
        while (a <= r.last) {
            var e = a
            while (e + 1 <= r.last && works[e + 1].round == works[a].round) e++
            out += BarUnit(b, works[a].cycle, a, e + 1, works[a].round, false)
            a = e + 1
        }
        return out
    }
    fun whole(b: Int) = listOf(BarUnit(b, works[blocks[b].first].cycle, blocks[b].first, blocks[b].last + 1, 0, false))
    fun gaps(us: List<BarUnit>) = (0 until us.size - 1).sumOf { unitGap(us[it], us[it + 1]).toDouble() }.toFloat()

    // Everything fits: plain per-set line.
    val all = blocks.indices.flatMap { sets(it) }
    val each = (width - gaps(all)) / n
    if (each >= 5f) return all to FloatArray(n) { each }

    val cb = blocks.indexOfFirst { cur in it }.coerceAtLeast(0)
    val curSize = blocks[cb].count()
    val share = if (blocks.size == 1) 1f else maxOf(0.55f, curSize.toFloat() / n)
    val curUnits = if (width * share / curSize - 2f >= 4f || works[blocks[cb].first].cycle == 0) sets(cb) else rounds(cb)

    fun build(collapse: Boolean): Pair<List<BarUnit>, FloatArray> {
        val us = ArrayList<BarUnit>()
        for (b in blocks.indices) us += if (b == cb) curUnits else if (collapse) whole(b) else rounds(b)
        val avail = width - gaps(us)
        val other = n - curSize
        val w = FloatArray(us.size) { k ->
            val u = us[k]
            val sz = (u.to - u.from).toFloat()
            if (u.block == cb) avail * share * sz / curSize else avail * (1f - share) * sz / other.coerceAtLeast(1)
        }
        return us to w
    }
    val first = build(false)
    val tooThin = first.first.indices.any { first.first[it].block != cb && first.second[it] < 2.5f }
    return if (tooThin) build(true) else first
}

@Composable
private fun TimerRing(
    fraction: Float,
    color: Color,
    track: Color,
    size: Dp,
    stroke: Dp,
    content: @Composable () -> Unit,
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2f
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(sw))
            drawArc(
                color,
                -90f,
                360f * fraction.coerceIn(0f, 1f),
                false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

private fun fractionLeft(s: WorkoutState): Float {
    val total = s.phase.seconds * 1000f
    return if (total <= 0f) 0f else (s.remainingMs / total)
}

// ---------- exercise ----------

@Composable
private fun WorkView(s: WorkoutState, onClose: () -> Unit) {
    val p = s.phase
    // Variant E: at the start of a cycle round the next segment of the round ring draws in,
    // "Round 2 of 3" shows inside the timer and the round badge flashes.
    val sweep = remember(s.index) { Animatable(if (p.roundStart) 0f else 1f) }
    var announcing by remember(s.index) { mutableStateOf(p.roundStart) }
    LaunchedEffect(s.index) {
        if (p.roundStart) {
            sweep.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
            delay(1000)
            announcing = false
        }
    }
    val rounds = if (p.rounds > 0) RoundRingState(p.round, p.rounds, sweep.value, announcing) else null
    // Long names that don't fit in three lines open in a card over the timer (tap again to close).
    var nameCut by remember(p.exercise.id) { mutableStateOf(false) }
    var nameOpen by remember(s.index) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        WorkoutHeader(s, onClose, muted = C.Muted, badgeBg = C.LimeDim, badgeFg = C.Lime, flash = announcing)
        SegmentBar(s, C.Lime)
        // The name always gets the same room (up to 3 lines, the font shrinks for long names),
        // so the timer below has the same size and place for every exercise.
        Box(
            Modifier
                .fillMaxWidth()
                .height(TITLE_BLOCK)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = if (nameCut) {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClickLabel = "Show full name") { nameOpen = true }
                } else {
                    Modifier
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExerciseIcon(p.exercise.icon, tileSize = 52.dp, iconSize = 30.dp, tile = C.Surface, corner = 16.dp, strokeWidth = 1.6f)
                Spacer(Modifier.width(12.dp))
                FitText(
                    p.exercise.name,
                    maxFontSize = 32.sp,
                    minFontSize = 20.sp,
                    maxLines = 3,
                    fontFamily = Condensed,
                    modifier = Modifier.weight(1f, fill = false),
                    onOverflow = { nameCut = it },
                )
                if (nameCut) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .align(Alignment.Bottom)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(C.Surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        PathIcon(UiIcons.CHEVRON_DOWN, C.Lime, size = 18.dp, strokeWidth = 2.6f)
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // One timer size for every set: timed or reps, in a cycle or not, with weight or not.
            // Room is kept for the round ring, and for the weight and reps controls if any set of this workout has them.
            val below = remember(s.phases) { belowRing(s.phases) }
            val inner = minOf(
                maxWidth - 64.dp - ROUND_RING_ROOM * 2,
                maxHeight - below - ROUND_RING_ROOM * 2,
                290.dp,
            ).coerceAtLeast(110.dp)
            val ringSize = inner
            val repsRingSize = inner
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (p.isReps) {
                    RepsRing(s, repsRingSize, rounds)
                    p.weight?.let { WeightPill(it, p) }
                    BigButton(
                        "Done",
                        onClick = { WorkoutEngine.done() },
                        icon = UiIcons.CHECK,
                        height = 76.dp,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        RoundIconButton(UiIcons.PREV, "Previous", { WorkoutEngine.previous() }, size = 52.dp, background = C.Surface, filled = true)
                        RoundIconButton(UiIcons.NEXT, "Skip", { WorkoutEngine.skip() }, size = 52.dp, background = C.Surface, filled = true)
                    }
                    return@Column
                }
                WithRoundRing(rounds, ringSize) {
                TimerRing(
                    fraction = fractionLeft(s),
                    color = if (s.paused) C.Faint else C.Lime,
                    track = Color(0xFF1F2327),
                    size = ringSize,
                    stroke = 16.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatTime(s.remainingSec),
                            fontFamily = Condensed,
                            fontSize = (ringSize.value * 0.33f).sp,
                            color = C.Text,
                        )
                        Text(
                            if (s.paused) "PAUSED" else "WORK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = if (s.paused) C.Orange else C.Lime,
                        )
                    }
                }
                }
                p.weight?.let { WeightPill(it, p) }
                Controls(
                    paused = s.paused,
                    accent = C.Lime,
                    onAccent = C.OnLime,
                    sideBg = C.Surface,
                )
            }
        }
        NextCard(s)
    }
    FullNameCard(p, visible = nameOpen, onClose = { nameOpen = false })
    }
}

/** Variant A: the full exercise name in a card over the timer; the rest of the screen is dimmed. Any tap closes it. */
@Composable
private fun FullNameCard(p: Phase, visible: Boolean, onClose: () -> Unit) {
    val quiet = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    androidx.compose.animation.AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x9E08090A))
                .clickable(interactionSource = quiet, indication = null, onClickLabel = "Collapse name") { onClose() },
        )
    }
    androidx.compose.animation.AnimatedVisibility(
        visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.96f),
        exit = fadeOut(tween(160)),
    ) {
        Column(
            Modifier
                .padding(start = 12.dp, end = 12.dp, top = 84.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(C.Surface)
                .border(1.dp, C.Surface3, RoundedCornerShape(24.dp))
                .clickable(interactionSource = quiet, indication = null, onClickLabel = "Collapse name") { onClose() }
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                ExerciseIcon(p.exercise.icon, tileSize = 52.dp, iconSize = 30.dp, tile = C.Surface2, corner = 16.dp, strokeWidth = 1.6f)
                Spacer(Modifier.width(12.dp))
                Text(p.exercise.name, fontFamily = Condensed, fontSize = 34.sp, lineHeight = 37.sp, color = C.Text)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.Surface2),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PathIcon(UiIcons.CHEVRON_UP, C.Lime, size = 16.dp, strokeWidth = 2.6f)
                Spacer(Modifier.width(6.dp))
                Text("Tap to collapse", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = C.Lime)
            }
        }
    }
}

/** A reps set: no countdown, the target number of reps and a small stopwatch. */
@Composable
private fun RepsRing(s: WorkoutState, ringSize: Dp, rounds: RoundRingState?) {
    val size = ringSize
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        WithRoundRing(rounds, size) {
        TimerRing(fraction = 0f, color = C.Sky, track = C.SkyTrack, size = size, stroke = 16.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DO", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = C.Sky)
                Text("${s.phase.reps ?: 0}", fontFamily = Condensed, fontSize = (size.value * 0.4f).sp, color = C.Text)
                Text(
                    if (s.paused) "PAUSED" else "REPS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = if (s.paused) C.Orange else C.Sky,
                )
            }
        }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PathIcon(UiIcons.TIMER, C.Muted, size = 16.dp, strokeWidth = 2f)
            Spacer(Modifier.width(6.dp))
            Text("${formatTime((s.elapsedMs / 1000).toInt())} on this set", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Muted)
        }
    }
}

private fun weightLabel(w: Double): String = formatWeight(w, Repo.state.value.settings.weightUnit)

/** Planned weight for the current set; the workout only shows it (it is set in the complex). */
@Composable
private fun WeightPill(weight: Double, p: Phase) {
    Row(
        Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(C.VioletBg)
            .border(1.5.dp, C.VioletBorder, RoundedCornerShape(24.dp))
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PathIcon(UiIcons.WEIGHT, C.Violet, size = 22.dp, strokeWidth = 2f)
        Spacer(Modifier.width(10.dp))
        Text(weightLabel(weight), fontFamily = Condensed, fontSize = 30.sp, color = C.Violet)
        if (p.rounds > 0) {
            Spacer(Modifier.width(10.dp))
            Text("round ${p.round} of ${p.rounds}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Muted)
        }
    }
}

/** "0:30" for a timed set, "×12" for a reps set. */
private fun Phase.valueLabel(): String = if (reps != null) "×$reps" else formatTime(seconds)

private val ROUND_RING_ROOM = 18.dp

/** Height kept for the exercise name: three lines of the title font. */
private val TITLE_BLOCK = 124.dp

/**
 * Space kept under the timer. It depends only on what the whole workout contains, so the timer
 * keeps one size for the entire workout but doesn't shrink for controls this workout never shows.
 * Timed set: top gap + play/pause controls. Reps set: stopwatch line + Done + previous/skip.
 * Plus the weight pill when any exercise uses weight, and a bottom margin.
 */
private fun belowRing(phases: List<Phase>): Dp {
    val work = phases.filter { it.type == PhaseType.WORK }
    val timed = 12.dp + 14.dp + 88.dp
    val reps = 12.dp + 14.dp + 20.dp + 14.dp + 76.dp + 14.dp + 52.dp
    val controls = if (work.any { it.isReps }) maxOf(timed, reps) else timed
    val weight = if (work.any { it.weight != null }) 48.dp + 14.dp else 0.dp
    return controls + weight + 16.dp
}

/** Round progress of a cycle shown around the timer. [sweep] draws in the current round's segment. */
private data class RoundRingState(val round: Int, val rounds: Int, val sweep: Float, val announcing: Boolean)

@Composable
private fun WithRoundRing(rounds: RoundRingState?, innerSize: Dp, content: @Composable () -> Unit) {
    val outer = innerSize + ROUND_RING_ROOM * 2
    if (rounds == null) {
        // Same footprint without a cycle, so the timer stays in place.
        Box(Modifier.size(outer), contentAlignment = Alignment.Center) { content() }
        return
    }
    Box(Modifier.size(outer), contentAlignment = Alignment.Center) {
        RoundSegments(rounds, outer)
        content()
        androidx.compose.animation.AnimatedVisibility(
            visible = rounds.announcing,
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.8f),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = ROUND_RING_ROOM - 7.dp), // sits on the bottom of the timer ring
        ) {
            Box(
                Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(C.Lime)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Round ${rounds.round} of ${rounds.rounds}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = C.OnLime, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RoundSegments(r: RoundRingState, size: Dp) {
    Canvas(Modifier.size(size)) {
        val stroke = 5.dp.toPx()
        val inset = stroke / 2f + 3.dp.toPx()
        val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        val n = r.rounds.coerceAtLeast(1)
        val gap = if (n == 1) 0f else (if (n > 12) 3f else 8f)
        // Round caps would close the narrow gaps between many segments, so use flat ends then.
        val cap = if (n > 12) StrokeCap.Butt else StrokeCap.Round
        val span = 360f / n - gap
        for (i in 0 until n) {
            val start = -90f + i * (360f / n) + gap / 2f
            val done = i < r.round - 1
            val current = i == r.round - 1
            drawArc(if (done) C.Lime else C.Surface3, start, span, false, topLeft, arcSize, style = Stroke(stroke, cap = cap))
            if (current) {
                val sweep = span * r.sweep.coerceIn(0f, 1f)
                if (r.announcing) {
                    // Soft glow while the segment draws in.
                    drawArc(C.Text.copy(alpha = 0.25f), start, sweep, false, topLeft, arcSize, style = Stroke(stroke * 3f, cap = cap))
                }
                drawArc(C.Text, start, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = cap))
            }
        }
    }
}

@Composable
private fun Controls(paused: Boolean, accent: Color, onAccent: Color, sideBg: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        RoundIconButton(UiIcons.PREV, "Previous", { WorkoutEngine.previous() }, size = 56.dp, background = sideBg, filled = true)
        RoundIconButton(
            if (paused) UiIcons.PLAY else UiIcons.PAUSE,
            if (paused) "Resume" else "Pause",
            { WorkoutEngine.togglePause() },
            size = 88.dp,
            background = accent,
            tint = onAccent,
            iconSize = 36.dp,
            filled = true,
        )
        RoundIconButton(UiIcons.NEXT, "Skip", { WorkoutEngine.skip() }, size = 56.dp, background = sideBg, filled = true)
    }
}

@Composable
private fun NextCard(s: WorkoutState) {
    val next = s.nextWork
    Row(
        Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
            .fillMaxWidth()
            // Fixed height (fits a two-line name) so the timer above never changes size.
            .height(88.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(C.Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (next == null) {
            PathIcon(UiIcons.CHECK, C.Lime, size = 26.dp, strokeWidth = 2.6f)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("LAST EXERCISE", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = C.Muted)
                Text("Finish strong!", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = C.Text)
            }
        } else {
            ExerciseIcon(next.exercise.icon, tileSize = 48.dp, iconSize = 26.dp, tile = C.Surface2, corner = 14.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                val label = if (s.restIsNext) {
                    "NEXT · AFTER ${formatTime(s.phases[s.index + 1].seconds)} REST"
                } else {
                    "NEXT"
                }
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = C.Muted)
                Text(next.exercise.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = C.Text, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(next.valueLabel(), fontFamily = Condensed, fontSize = 26.sp, color = if (next.reps != null) C.Sky else C.Text)
                next.weight?.let { Text(weightLabel(it), fontFamily = Condensed, fontSize = 18.sp, color = C.Violet) }
            }
        }
    }
}

// ---------- rest ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestView(s: WorkoutState, onClose: () -> Unit) {
    val p = s.phase
    val after = s.phases.drop(s.index + 1).filter { it.type == PhaseType.WORK }.drop(1).take(2)
    Column(
        Modifier
            .fillMaxSize()
            .background(C.RestBg),
    ) {
        WorkoutHeader(s, onClose, muted = C.RestMuted, badgeBg = C.RestSurface2, badgeFg = C.Orange)
        SegmentBar(s, C.Orange)
        Row(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerRing(
                fraction = fractionLeft(s),
                color = if (s.paused) C.Faint else C.Orange,
                track = C.RestSurface2,
                size = 150.dp,
                stroke = 10.dp,
            ) {
                Text(formatTime(s.remainingSec), fontFamily = Condensed, fontSize = 52.sp, color = C.Text)
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FitText(
                    when {
                        s.paused -> "PAUSED"
                        s.phase.type == PhaseType.PREPARE -> "GET READY"
                        else -> "REST"
                    },
                    maxFontSize = 52.sp,
                    minFontSize = 24.sp,
                    maxLines = 1,
                    color = C.Orange,
                    fontFamily = Condensed,
                )
                Text(
                    if (s.phase.type == PhaseType.PREPARE) "Get into position for the first exercise." else "Get into position for the next exercise.",
                    fontSize = 14.sp,
                    color = C.RestMuted,
                    maxLines = 3,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BigButton(
                "+10 s",
                onClick = { WorkoutEngine.addTime(10) },
                container = C.RestSurface2,
                content = C.Text,
                height = 48.dp,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.RestSurface2),
                contentAlignment = Alignment.Center,
            ) {
                RoundIconButton(
                    if (s.paused) UiIcons.PLAY else UiIcons.PAUSE,
                    if (s.paused) "Resume" else "Pause",
                    { WorkoutEngine.togglePause() },
                    size = 48.dp,
                    filled = true,
                    iconSize = 22.dp,
                )
            }
            BigButton(
                "Skip",
                onClick = { WorkoutEngine.skip() },
                icon = UiIcons.NEXT,
                iconFilled = true,
                container = C.Orange,
                content = C.OnOrange,
                height = 48.dp,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(C.RestSurface)
                .border(1.5.dp, C.RestLine, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("UP NEXT", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp, color = C.Orange)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseIcon(p.exercise.icon, tileSize = 104.dp, iconSize = 64.dp, tile = C.RestSurface2, corner = 30.dp, strokeWidth = 1.4f)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FitText(p.exercise.name, maxFontSize = 36.sp, minFontSize = 18.sp, maxLines = 5, fontFamily = Condensed)
                    Text(p.valueLabel(), fontFamily = Condensed, fontSize = 30.sp, color = if (p.reps != null) C.Sky else C.Lime)
                    p.weight?.let { Text(weightLabel(it), fontFamily = Condensed, fontSize = 24.sp, color = C.Violet) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (p.rounds > 0) InfoChip("Round ${p.round} of ${p.rounds}", UiIcons.REPEAT)
                InfoChip("Exercise ${p.workNumber} of ${s.totalWork}", null)
            }
            if (after.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(C.RestLine),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("THEN", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = C.RestMuted)
                    for (a in after) ThenRow(a)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, icon: String?) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(C.RestSurface2)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            PathIcon(icon, C.Lime, size = 15.dp, strokeWidth = 2.2f)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Text)
    }
}

@Composable
private fun ThenRow(p: Phase) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExerciseIcon(p.exercise.icon, tileSize = 44.dp, iconSize = 26.dp, tile = C.RestSurface2, corner = 12.dp)
        Spacer(Modifier.width(12.dp))
        val round = if (p.rounds > 0) " · round ${p.round}" else ""
        Text("${p.exercise.name}$round", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD9D2CB), lineHeight = 19.sp)
        Text(p.valueLabel() + (p.weight?.let { " · " + weightLabel(it) } ?: ""), fontFamily = Condensed, fontSize = 20.sp, color = C.RestMuted)
    }
}

// ---------- finished ----------

@Composable
private fun FinishedView(s: WorkoutState, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(C.LimeDim),
            contentAlignment = Alignment.Center,
        ) {
            PathIcon(UiIcons.CHECK, C.Lime, size = 56.dp, strokeWidth = 2.6f)
        }
        Spacer(Modifier.height(24.dp))
        FitText("Workout complete", maxFontSize = 40.sp, minFontSize = 24.sp, maxLines = 2, fontFamily = Condensed, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(s.complexName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(formatTime(s.actualSeconds), "total time")
            Stat("${s.totalWork}", "exercise sets")
        }
        Spacer(Modifier.height(40.dp))
        BigButton("Done", onClick = onDone, icon = UiIcons.CHECK, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = Condensed, fontSize = 32.sp, color = C.Text)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
    }
}
