package com.trainingtimer.app.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * A column whose items can be reordered by holding and dragging.
 * [itemContent] receives a modifier to put on the part of the item that starts the drag (the "handle").
 */
@Composable
fun <T> DraggableColumn(
    items: List<T>,
    keyOf: (T) -> String,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    itemContent: @Composable (item: T, isDragging: Boolean, handle: Modifier) -> Unit,
) {
    val heights = remember { mutableStateMapOf<String, Int>() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val haptic = LocalHapticFeedback.current
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentKeyOf by rememberUpdatedState(keyOf)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        for (item in items) {
            val k = keyOf(item)
            key(k) {
                val dragging = draggingKey == k
                val handle = Modifier.pointerInput(k) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingKey = k
                            offset = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            draggingKey = null
                            offset = 0f
                        },
                        onDragCancel = {
                            draggingKey = null
                            offset = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            offset += amount.y
                            val list = currentItems
                            val i = list.indexOfFirst { currentKeyOf(it) == k }
                            if (i >= 0) {
                                if (offset > 0f && i < list.lastIndex) {
                                    val step = (heights[currentKeyOf(list[i + 1])] ?: 0) + spacingPx
                                    if (offset > step / 2f) {
                                        currentOnMove(i, i + 1)
                                        offset -= step
                                    }
                                } else if (offset < 0f && i > 0) {
                                    val step = (heights[currentKeyOf(list[i - 1])] ?: 0) + spacingPx
                                    if (-offset > step / 2f) {
                                        currentOnMove(i, i - 1)
                                        offset += step
                                    }
                                }
                            }
                        },
                    )
                }
                Box(
                    Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) offset else 0f
                            scaleX = if (dragging) 1.02f else 1f
                            scaleY = if (dragging) 1.02f else 1f
                        }
                        .then(if (dragging) Modifier.shadow(12.dp, RoundedCornerShape(18.dp)) else Modifier)
                        .onSizeChanged { heights[k] = it.height },
                ) {
                    itemContent(item, dragging, handle)
                }
            }
        }
    }
}
