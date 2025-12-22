package com.hifnawy.alquran.view.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.times
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Displays a set of vertical bars with continuously animating heights,
 * resembling an audio visualizer. The animation for each bar is independent and randomized.
 *
 * @param modifier [Modifier] The modifier to be applied to the layout.
 * @param barCount [Int] The number of vertical bars to display.
 * @param height [Dp] The total height of the composable.
 * @param barWidth [Dp] The width of each individual bar.
 * @param barSpacing [Dp] The spacing between each bar.
 * @param durationRangeMs [IntRange] The range of duration in milliseconds for each height change
 *   animation. A random value within this range is chosen for each animation cycle of each bar.
 * @param color [Color] The color of the bars.
 */
@Composable
fun AnimatedAudioBars(
        modifier: Modifier = Modifier,
        barCount: Int = 7,
        height: Dp = 40.dp,
        barWidth: Dp = 3.dp,
        barSpacing: Dp = 2.dp,
        durationRangeMs: IntRange = 100..150,
        color: Color = MaterialTheme.colorScheme.primary
) {
    val barMinHeight = 5.dp
    val barMaxHeight = height - 5.dp
    val width = barCount * (barWidth + barSpacing)

    Row(
            modifier = modifier
                .requiredWidth(width)
                .requiredHeight(height)
                .clipToBounds(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
    ) {
        repeat(barCount) { index ->
            RandomHeightBar(
                    color = color,
                    barWidth = barWidth,
                    barMinHeight = barMinHeight,
                    barMaxHeight = barMaxHeight,
                    durationRangeMs = durationRangeMs
            )

            if (index < barCount - 1) Spacer(Modifier.width(barSpacing))
        }
    }
}

/**
 * Displays a single vertical bar with a randomly animated height.
 * The bar's height continuously animates between a minimum and maximum value over a random duration.
 *
 * @param color [Color] The [Color] of the bar.
 * @param barWidth [Dp] The width of the bar.
 * @param barMinHeight [Dp] The minimum height the bar can animate to.
 * @param barMaxHeight [Dp] The maximum height the bar can animate to.
 * @param durationRangeMs [IntRange] The range of possible durations for a single height animation cycle,
 *   in milliseconds. A random value from this range will be chosen for each animation.
 */
@Composable
private fun RandomHeightBar(
        color: Color,
        barWidth: Dp,
        barMinHeight: Dp,
        barMaxHeight: Dp,
        durationRangeMs: IntRange
) {
    val animationDuration = durationRangeMs.randomInt
    val animation = tween<Float>(durationMillis = animationDuration, easing = LinearEasing)
    val fraction = remember { Animatable(initialValue = randomFloat) }
    val height = lerp(start = barMinHeight, stop = barMaxHeight, fraction = fraction.value)

    LaunchedEffect(Unit) {
        while (true) fraction.animateTo(targetValue = randomFloat, animationSpec = animation)
    }

    Box(
            modifier = Modifier
                .width(barWidth)
                .height(height)
                .clip(RoundedCornerShape(percent = 50))
                .background(color)
    )
}

/**
 * A utility property to get the next random [Float] value uniformly distributed between
 * `0f` **`inclusive`** and `1f` **`exclusive`**.
 *
 * @return [Float] A random [Float] value between `0f` **`inclusive`** and `1f` **`exclusive`**.
 */
private inline val randomFloat get() = Random.nextFloat()

/**
 * A utility property to generate an [Int] random value uniformly distributed in the specified
 * [IntRange]: from [IntRange.start] **`inclusive`** to [IntRange.endInclusive] **`inclusive`**.
 *
 * @receiver [IntRange] The [IntRange] to get a random [Int] from.
 *
 * @return [Int] A random [Int] value between [IntRange.first] **`inclusive`** and [IntRange.last] **`exclusive`**.
 */
private inline val IntRange.randomInt get() = Random.nextInt(this)
