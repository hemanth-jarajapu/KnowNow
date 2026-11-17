package com.passion.knownow.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Drag from the left edge to go back, with the page following the finger.
 *
 * Going back is a motion, not a button press: the screen behaves like a card the reader pushes
 * away. While dragging it tracks the finger (with a little resistance, so it never flies off on
 * its own), lifts off the surface with a growing corner radius and shadow, and shrinks slightly
 * away from the left edge so the page behind can be glimpsed. Past roughly a third of the width
 * — or a quick flick — a single haptic tick confirms the release and the navigation pop carries
 * the card the rest of the way out; anything shorter springs back exactly where it came from.
 *
 * Android 14+ already animates the system back gesture (`enableOnBackInvokedCallback`), so this
 * exists for three-button navigation and for the habit of swiping inside the app. It stays
 * completely inert when [enabled] is false, which is the case for top-level tabs that have
 * nowhere to go back to.
 */
@Composable
fun SwipeBackHost(
    enabled: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val scope = rememberCoroutineScope()
        val haptics = LocalHapticFeedback.current

        var offset by remember(enabled) { mutableFloatStateOf(0f) }
        var dragging by remember(enabled) { mutableStateOf(false) }
        var armed by remember(enabled) { mutableStateOf(false) }

        val progress = if (widthPx > 0f) (offset / widthPx).coerceIn(0f, 1f) else 0f

        fun settleBack() {
            scope.launch {
                animate(
                    initialValue = offset,
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { value, _ -> offset = value }
            }
        }

        // A resting surface behind the card, so pushing the page away reveals depth instead of
        // a transparent hole.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    val shrink = 1f - 0.05f * progress
                    scaleX = shrink
                    scaleY = shrink
                    if (progress > 0.01f) {
                        clip = true
                        shape = RoundedCornerShape((22f * progress).dp)
                        shadowElevation = 26.dp.toPx() * progress
                    }
                }
                .pointerInput(enabled, widthPx) {
                    if (!enabled || widthPx <= 0f) return@pointerInput
                    val edge = 32.dp.toPx()
                    val armAt = widthPx * 0.3f
                    var travelled = 0f
                    var startedAt = 0L

                    detectHorizontalDragGestures(
                        onDragStart = { position ->
                            dragging = position.x <= edge
                            travelled = 0f
                            startedAt = System.currentTimeMillis()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!dragging) return@detectHorizontalDragGestures
                            if (dragAmount < 0f && offset <= 0f) return@detectHorizontalDragGestures
                            change.consume()
                            travelled += dragAmount
                            // Resistance keeps the card glued to the finger without letting a
                            // single long drag shove it entirely off screen.
                            offset = (offset + dragAmount * 0.45f).coerceIn(0f, widthPx)
                            if (!armed && travelled >= armAt) {
                                armed = true
                                runCatching {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                            } else if (armed && travelled < armAt * 0.6f) {
                                armed = false
                            }
                        },
                        onDragEnd = {
                            if (!dragging) return@detectHorizontalDragGestures
                            dragging = false
                            val quickFlick = System.currentTimeMillis() - startedAt < 260L &&
                                travelled > widthPx * 0.12f
                            val goBack = armed || travelled >= armAt || quickFlick
                            armed = false
                            if (goBack) {
                                // Hand the motion to navigation immediately — its pop
                                // transition starts from where the card is now — and let the
                                // drag offset dissolve underneath it.
                                onBack()
                                scope.launch {
                                    animate(
                                        initialValue = offset,
                                        targetValue = 0f,
                                        animationSpec = tween(150, easing = FastOutSlowInEasing),
                                    ) { value, _ -> offset = value }
                                }
                            } else {
                                settleBack()
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            armed = false
                            settleBack()
                        },
                    )
                },
        ) {
            content()
        }
    }
}
