package com.hifnawy.alquran.view.gridItems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.view.player.AnimatedAudioBars

@Composable
fun MoshafCard(
        modifier: Modifier = Modifier,
        reciter: Reciter,
        moshaf: Moshaf,
        isPlaying: Boolean = false,
        onMoshafClick: (Reciter, Moshaf) -> Unit = { _, _ -> }
) {
    val animationDurationMillis = 500
    val floatAnimationSpec = tween<Float>(durationMillis = animationDurationMillis)
    val intSizeAnimationSpec = tween<IntSize>(durationMillis = animationDurationMillis)

    ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(20.dp),
            onClick = { onMoshafClick(reciter, moshaf) },
    ) {
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                    painter = painterResource(id = R.drawable.book_24px),
                    contentDescription = "Moshaf Icon"
            )

            Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .basicMarquee(),
                    text = "${moshaf.name} - ${pluralStringResource(R.plurals.surah_count, moshaf.surahsCount, moshaf.surahsCount)}",
            )

            AnimatedVisibility(
                    visible = isPlaying,
                    enter = scaleIn(
                            animationSpec = floatAnimationSpec,
                            transformOrigin = TransformOrigin(0f, 0f)
                    ) + fadeIn(animationSpec = floatAnimationSpec) + expandIn(
                            animationSpec = intSizeAnimationSpec,
                            expandFrom = Alignment.TopStart
                    ),
                    exit = scaleOut(
                            animationSpec = floatAnimationSpec,
                            transformOrigin = TransformOrigin(0f, 0f)
                    ) + fadeOut(animationSpec = floatAnimationSpec) + shrinkOut(
                            animationSpec = intSizeAnimationSpec,
                            shrinkTowards = Alignment.TopStart
                    )

            ) {
                AnimatedAudioBars()
            }
        }
    }
}

@Composable
@Preview(locale = "ar")
fun MoshafCardPreview() {
    val reciter = sampleReciters.random()
    val moshaf = reciter.moshaf.random()
    MoshafCard(
            reciter = reciter,
            moshaf = moshaf
    )
}