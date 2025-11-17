package com.hifnawy.alquran.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

object ModifierExt {
    enum class AnimationType {
        RiseUp, FallDown, None
    }

    fun Modifier.animateItemPosition(
            duration: Int = 300,
            animationType: AnimationType = AnimationType.FallDown
    ): Modifier = composed {
        val alpha = remember { Animatable(0f) }
        val scale = remember { Animatable(1.5f) }
        val translation = remember {
            Animatable(
                    when (animationType) {
                        AnimationType.RiseUp   -> -1f
                        AnimationType.FallDown -> 1f
                        AnimationType.None     -> 0f
                    }
            )
        }

        LaunchedEffect(Unit) {
            launch {
                alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                )
            }

            launch {
                scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                )
            }

            launch {
                translation.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
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
}
