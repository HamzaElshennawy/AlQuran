package com.hifnawy.alquran.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.hifnawy.alquran.utils.DeviceConfiguration.Companion.deviceConfiguration
import com.hifnawy.alquran.utils.FlowEx.throttleFirst
import com.hifnawy.alquran.utils.ModifierEx.AnimationType.FallDown
import com.hifnawy.alquran.utils.ModifierEx.AnimationType.None
import com.hifnawy.alquran.utils.ModifierEx.AnimationType.RiseUp
import com.hifnawy.alquran.utils.ModifierEx.onTouch
import com.hifnawy.alquran.utils.ModifierEx.verticalDraggable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.window.core.layout.WindowSizeClass

/**
 * Util class providing extension functions for [Modifier].
 *
 * @author AbdElMoniem ElHifnawy
 */
object ModifierEx {

    /**
     * Animation type
     *
     * @property RiseUp - Animated item [translationY][GraphicsLayerScope.translationY] will rise up from below it's final position
     * @property FallDown - Animated item [translationY][GraphicsLayerScope.translationY] will fall down from above it's final position
     * @property None - Animated item [translationY][GraphicsLayerScope.translationY] will not be animated
     *
     * @author AbdElMoniem ElHifnawy
     */
    enum class AnimationType(val value: Float) {

        /**
         * item [translationY][GraphicsLayerScope.translationY] will rise up from below it's final position
         */
        RiseUp(-1f),

        /**
         * item [translationY][GraphicsLayerScope.translationY] will fall down from above it's final position
         */
        FallDown(1f),

        /**
         * item [translationY][GraphicsLayerScope.translationY] will not be animated
         */
        None(0f)
    }

    /**
     * Applies padding to a composable to accommodate the Input Method Editor (IME) or on-screen keyboard,
     * ensuring that the content remains visible and is not obscured when the keyboard is displayed.
     *
     * This modifier uses [WindowAdaptiveInfo], [WindowSizeClass] and [deviceConfiguration] to determine
     * if the keyboard should be accommodated. which includes system bars, display cutouts, and the IME. It then
     * specifically applies padding only for the bottom inset, effectively pushing the content up by the height of the keyboard.
     *
     * It also applies the padding only when the device is not a compact device or a phone in landscape orientation. to help
     * prevent the content from being obscured by the keyboard on compact devices and phones in landscape orientation.
     *
     * Example usage:
     * ```
     * Column(modifier = Modifier.fillMaxSize().safeImePadding()) {
     *     // Content that should be pushed up by the keyboard
     * }
     * ```
     *
     * @return [Modifier] A [Modifier] that adds bottom padding equal to the IME's height.
     *
     * @see WindowAdaptiveInfo
     * @see WindowSizeClass
     * @see deviceConfiguration
     */
    fun Modifier.safeImePadding(windowAdaptiveInfo: WindowAdaptiveInfo) = when (windowAdaptiveInfo.windowSizeClass.deviceConfiguration) {
        DeviceConfiguration.COMPACT,
        DeviceConfiguration.PHONE_LANDSCAPE -> this

        else                                -> imePadding()
    }

    /**
     * A [Modifier] that captures all raw pointer input events in the initial pass.
     *
     * This is useful for observing touch events like down, up, and move actions
     * before they are consumed by other modifiers or composables, such as `clickable` or `draggable`.
     * The provided [onTouch] lambda will be invoked for every [PointerEvent] that occurs within
     * the bounds of the composable this modifier is applied to.
     *
     * @param onTouch [(event: PointerEvent) -> Unit][onTouch] A lambda that will be called with the [PointerEvent] for each pointer event.
     * @return [Modifier] A [Modifier] that listens for pointer input.
     */
    fun Modifier.onTouch(onTouch: (event: PointerEvent) -> Unit) = pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                onTouch(event)
            }
        }
    }

    /**
     * Animate list item position, scale and alpha
     *
     * @param durationMs Duration of the animation in milliseconds
     * @param animationType Type of animation to perform
     */
    fun Modifier.animateItemPosition(durationMs: Int = 300, animationType: AnimationType = FallDown) = composed {
        val alpha = remember { Animatable(0f) }
        val scale = remember { Animatable(1.5f) }
        val translation = remember { Animatable(animationType.value) }

        LaunchedEffect(Unit) {
            launch {
                alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
                )
            }

            launch {
                scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
                )
            }

            launch {
                translation.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
                )
            }
        }

        graphicsLayer {
            this.alpha = alpha.value

            this.scaleX = scale.value
            this.scaleY = scale.value

            // translationY works in *pixels*, so multiply by height
            this.translationY = translation.value * size.height
        }
    }

    /**
     * A modifier that enables vertical dragging on a composable to control its height, typically
     * for creating an expandable/collapsible panel.
     *
     * This modifier handles the drag gestures, updates the height of the component, and determines
     * whether the component should snap to an expanded or minimized state when the drag is released.
     * It also provides callbacks for drag direction changes and when the final snap state is determined.
     *
     * @param heightPx [Animatable<Float, *>][Animatable] The animatable current height of the component in pixels. This state is updated during the drag.
     * @param minHeightPx [Float] The minimum height the component can be dragged to.
     * @param maxHeightPx [Float] The maximum height the component can be dragged to.
     * @param minimizeThreshold [Float] The height in pixels below which the component will snap to its minimized state if it was previously expanded.
     * @param expandThreshold [Float] The height in pixels above which the component will snap to its expanded state if it was previously minimized.
     * @param isExpanded [Boolean] The current state (expanded or minimized) of the component.
     * @param onSnapped [(shouldExpand: Boolean) -> Unit][onSnapped] A callback invoked when the drag gesture ends, providing a boolean indicating whether the component should
     *                  expand (`true`) or minimize (`false`). This is typically used to trigger an animation to the final state.
     * @param onHeight [(isExpanded: Boolean) -> Unit][onHeight] A callback invoked when the drag gesture ends, providing the target expansion state.
     * @param onHeightChanged [(progress: Float) -> Unit][onHeightChanged] A callback invoked when the height is changed due to an active drag, providing the current
     *                  progress (`0.0f` to `1.0f`).
     * @param onDragDirectionChanged [(isDraggingUp: Boolean, isDraggingDown: Boolean) -> Unit][onDragDirectionChanged] A debounced callback that indicates the current drag
     *                  direction (up or down).
     * @return [Modifier] A [Modifier] that applies the vertical drag gesture handling.
     */
    @Composable
    fun Modifier.verticalDraggable(
            heightPx: Animatable<Float, *>,
            minHeightPx: Float,
            maxHeightPx: Float,
            minimizeThreshold: Float,
            expandThreshold: Float,
            isExpanded: Boolean,
            onSnapped: (shouldExpand: Boolean) -> Unit = {},
            onHeight: (isExpanded: Boolean) -> Unit = {},
            onHeightChanged: (progress: Float) -> Unit = {},
            onDragDirectionChanged: (isDraggingUp: Boolean, isDraggingDown: Boolean) -> Unit = { _, _ -> }
    ) = run {
        data class DragDirection(val isDraggingUp: Boolean, val isDraggingDown: Boolean)

        val dragDebounce = 300.milliseconds
        val coroutineScope = rememberCoroutineScope()
        val dragEvents = remember { MutableSharedFlow<DragDirection>(extraBufferCapacity = 1) }

        LaunchedEffect(Unit) {
            dragEvents
                .throttleFirst(dragDebounce)
                .onEach { onDragDirectionChanged(it.isDraggingUp, it.isDraggingDown) }
                .collect()
        }

        draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    val oldHeight = heightPx.value
                    val newHeight = (heightPx.value - delta).coerceIn(minHeightPx, maxHeightPx)
                    val draggingUp = newHeight >= oldHeight

                    coroutineScope.launch {
                        heightPx.snapTo(newHeight)
                        val progress = ((heightPx.value - minHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)
                        onHeightChanged(progress)
                    }
                    dragEvents.tryEmit(DragDirection(isDraggingUp = draggingUp, isDraggingDown = !draggingUp))
                },
                onDragStopped = {
                    dragEvents.tryEmit(DragDirection(isDraggingUp = false, isDraggingDown = false))

                    val shouldExpand = shouldExpand(
                            heightPx = heightPx.value,
                            minimizeThreshold = minimizeThreshold,
                            expandThreshold = expandThreshold,
                            maxHeightPx = maxHeightPx,
                            minHeightPx = minHeightPx,
                            isExpanded = isExpanded
                    )

                    onHeight(shouldExpand)
                    onSnapped(shouldExpand)
                }
        )
    }

    /**
     * Determines whether a vertically draggable component should expand or collapse
     * based on its current height and state after a drag gesture has finished.
     *
     * The logic is as follows:
     * 1. If the component is currently collapsed (![isExpanded]) and has been dragged
     *    above the [expandThreshold], it should expand.
     * 2. If the component is currently expanded ([isExpanded]) and has been dragged
     *    below the [minimizeThreshold], it should collapse.
     * 3. In all other cases (i.e., when the drag ends between the thresholds), the
     *    final state is determined by whether the current height is closer to the
     *    [maxHeightPx] or [minHeightPx]. If [heightPx] is greater than the midpoint,
     *    it should expand; otherwise, it should collapse.
     *
     * @param heightPx [Float] The current height of the component in pixels at the end of the drag.
     * @param minimizeThreshold [Float] The height in pixels below which an expanded component should collapse.
     * @param expandThreshold [Float] The height in pixels above which a collapsed component should expand.
     * @param maxHeightPx [Float] The maximum possible height of the component.
     * @param minHeightPx [Float] The minimum possible height of the component.
     * @param isExpanded [Boolean] `true` if the component was in an expanded state before the drag started, `false` otherwise.
     *
     * @return [Boolean] `true` if the component should snap to the expanded state, `false` if it should snap to the collapsed state.
     *
     * @see verticalDraggable
     */
    private fun shouldExpand(heightPx: Float, minimizeThreshold: Float, expandThreshold: Float, maxHeightPx: Float, minHeightPx: Float, isExpanded: Boolean) = when {
        heightPx > expandThreshold && !isExpanded -> true
        heightPx < minimizeThreshold && isExpanded -> false

        else -> {
            val middle = (maxHeightPx + minHeightPx) / 2f
            when {
                heightPx > middle -> true
                else              -> false
            }
        }
    }
}
