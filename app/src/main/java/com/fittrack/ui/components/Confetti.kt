package com.fittrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fittrack.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 彩带粒子庆祝动画 —— 训练完成、升级、成就解锁时触发
 *
 * @param active 是否触发放射
 * @param particleCount 粒子数量
 * @param durationMillis 动画持续时间
 */
@Composable
fun ConfettiEffect(
    active: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 80,
    durationMillis: Int = 2500
) {
    if (!active) return

    val particles = remember { List(particleCount) { ConfettiParticle.random() } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(active) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, easing = LinearEasing)
        )
    }

    val animProgress by progress.asState()

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height * 0.3f

        particles.forEach { particle ->
            val t = (animProgress * particle.speed).coerceIn(0f, 1f)
            val x = centerX + particle.vx * t * size.width * 0.6f +
                    particle.gravityX * t * t * size.width * 0.2f
            val y = centerY + particle.vy * t * size.height * 0.5f +
                    0.5f * particle.gravityY * t * t * size.height
            val alpha = if (t < 0.7f) 1f else 1f - (t - 0.7f) / 0.3f
            val rotation = particle.rotationSpeed * t * 360f
            val scale = if (t < 0.1f) t / 0.1f else 1f

            drawCircle(
                color = particle.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = particle.size * scale,
                center = Offset(x, y)
            )

            // 小矩形碎片
            drawRect(
                color = particle.color2.copy(alpha = alpha.coerceIn(0f, 1f)),
                topLeft = Offset(
                    x + cos(rotation) * particle.size * 2,
                    y + sin(rotation) * particle.size * 2
                ),
                size = androidx.compose.ui.geometry.Size(
                    particle.size * scale * 0.8f,
                    particle.size * scale * 0.4f
                )
            )
        }
    }
}

private data class ConfettiParticle(
    val color: Color,
    val color2: Color,
    val size: Float,
    val vx: Float,
    val vy: Float,
    val gravityX: Float,
    val gravityY: Float,
    val speed: Float,
    val rotationSpeed: Float
) {
    companion object {
        fun random(): ConfettiParticle {
            val angle = Random.nextFloat() * Math.PI * 2
            val velocity = Random.nextFloat() * 0.8f + 0.2f
            val colors = listOf(
                FitGreen, FitBlue, FitOrange, FitYellow,
                FitPurple, FitRed, Color(0xFF00D9C0), Color(0xFFFF6B9D)
            )
            return ConfettiParticle(
                color = colors.random(),
                color2 = colors.random(),
                size = Random.nextFloat() * 8f + 4f,
                vx = (cos(angle) * velocity).toFloat(),
                vy = (sin(angle) * velocity - 0.5f).toFloat(),
                gravityX = (Random.nextFloat() - 0.5f) * 0.3f,
                gravityY = 1.2f,
                speed = Random.nextFloat() * 0.4f + 0.8f,
                rotationSpeed = Random.nextFloat() * 2f - 1f
            )
        }
    }
}
