package com.fittrack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextMotion

/**
 * 数字滚动动画组件（Slot Machine 效果）
 * 数字变化时像老虎机一样滚动切换
 */
@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    prefix: String = "",
    suffix: String = ""
) {
    AnimatedContent(
        targetState = count,
        modifier = modifier,
        transitionSpec = {
            if (targetState > initialState) {
                slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { fullHeight -> fullHeight }
                ) togetherWith slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { fullHeight -> -fullHeight }
                )
            } else {
                slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { fullHeight -> -fullHeight }
                ) togetherWith slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { fullHeight -> fullHeight }
                )
            }
        },
        label = "animatedCounter"
    ) { targetCount ->
        Text(
            text = "$prefix$targetCount$suffix",
            style = style.copy(textMotion = TextMotion.Animated),
            maxLines = 1
        )
    }
}
