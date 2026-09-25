package com.airplayr.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AirPlayrLongListFastScrollHandle(state: LazyListState, bottomPadding: androidx.compose.ui.unit.Dp = 72.dp) {
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    val total = state.layoutInfo.totalItemsCount
    val visible = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    if (total <= visible + 4) return
    val maxFirst = (total - visible).coerceAtLeast(1)
    val fraction = (state.firstVisibleItemIndex.toFloat() / maxFirst).coerceIn(0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize().padding(end = 0.dp, top = 18.dp, bottom = bottomPadding)) {
        val trackHeight = maxHeight - 12.dp
        val thumbHeight = 58.dp
        val rangePx = with(LocalDensity.current) { (trackHeight - thumbHeight).toPx().coerceAtLeast(1f) }
        val y = (trackHeight - thumbHeight).coerceAtLeast(0.dp) * fraction
        AnimatedVisibility(
            visible = state.isScrollInProgress,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = y),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FastScrollThumb(
                modifier = Modifier.pointerInput(total, visible) {
                    var dragFraction = fraction
                    detectDragGestures(
                        onDragStart = { dragFraction = fraction },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragFraction = (dragFraction + dragAmount.y / rangePx).coerceIn(0f, 1f)
                            scope.launch {
                                state.scrollToItem((dragFraction * maxFirst).roundToInt().coerceIn(0, maxFirst))
                            }
                        },
                    )
                }
            )
        }
    }
}

@Composable
fun AirPlayrLongGridFastScrollHandle(state: LazyGridState, bottomPadding: androidx.compose.ui.unit.Dp = 72.dp) {
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    val total = state.layoutInfo.totalItemsCount
    val visible = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    if (total <= visible + 4) return
    val maxFirst = (total - visible).coerceAtLeast(1)
    val fraction = (state.firstVisibleItemIndex.toFloat() / maxFirst).coerceIn(0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize().padding(end = 0.dp, top = 18.dp, bottom = bottomPadding)) {
        val trackHeight = maxHeight - 12.dp
        val thumbHeight = 58.dp
        val rangePx = with(LocalDensity.current) { (trackHeight - thumbHeight).toPx().coerceAtLeast(1f) }
        val y = (trackHeight - thumbHeight).coerceAtLeast(0.dp) * fraction
        AnimatedVisibility(
            visible = state.isScrollInProgress,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = y),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FastScrollThumb(
                modifier = Modifier.pointerInput(total, visible) {
                    var dragFraction = fraction
                    detectDragGestures(
                        onDragStart = { dragFraction = fraction },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragFraction = (dragFraction + dragAmount.y / rangePx).coerceIn(0f, 1f)
                            scope.launch {
                                state.scrollToItem((dragFraction * maxFirst).roundToInt().coerceIn(0, maxFirst))
                            }
                        },
                    )
                }
            )
        }
    }
}

@Composable
private fun FastScrollThumb(modifier: Modifier = Modifier) {
    val colors = LocalAirPlayrColors.current
    Surface(
        modifier = modifier.size(58.dp).shadow(12.dp, CircleShape, clip = false),
        shape = CircleShape,
        color = colors.surfaceContainerHighest.copy(alpha = .96f),
        tonalElevation = 2.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.KeyboardArrowUp, "Drag upward", tint = colors.primary, modifier = Modifier.size(22.dp))
                Icon(Icons.Default.KeyboardArrowDown, "Drag downward", tint = colors.primary, modifier = Modifier.size(22.dp))
            }
        }
    }
}
