package com.eterultimate.eteruee.ui.components.cyberpunk

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.eterultimate.eteruee.ui.theme.presets.NeonCyan
import com.eterultimate.eteruee.ui.theme.presets.NeonPink
import com.eterultimate.eteruee.ui.theme.presets.NeonPurple
import kotlin.math.sin
import kotlin.random.Random

/**
 * 纭湕璧涘崥鏈嬪厠缃戞牸鑳屾櫙
 * 鐩寸嚎缃戞牸 + 鏁版嵁娴佹晥鏋?
 */
@Composable
fun GridBackground(
    modifier: Modifier = Modifier,
    lineColor: Color = NeonCyan.copy(alpha = 0.08f),
    lineSpacing: Float = 48f,
    highlightColor: Color = NeonCyan.copy(alpha = 0.15f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 涓荤綉鏍肩嚎
        var y = 0f
        while (y < height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += lineSpacing
        }

        var x = 0f
        while (x < width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += lineSpacing
        }

        // 姣?鏉＄嚎鍔犵矖涓€鏉?
        y = 0f
        while (y < height) {
            drawLine(
                color = highlightColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.5f
            )
            y += lineSpacing * 5
        }
    }
}

/**
 * 鎵弿绾挎晥鏋?- 纭湕鐗堟湰
 */
@Composable
fun ScanLineEffect(
    modifier: Modifier = Modifier,
    scanLineColor: Color = NeonCyan.copy(alpha = 0.4f),
    durationMillis: Int = 4000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val y = progress * size.height
        // 涓绘壂鎻忕嚎
        drawLine(
            color = scanLineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2f
        )
        // 鎵弿绾垮熬閮ㄦ笎鍙?
        drawLine(
            color = scanLineColor.copy(alpha = 0.1f),
            start = Offset(0f, y - 40f),
            end = Offset(size.width, y - 40f),
            strokeWidth = 1f
        )
    }
}

/**
 * 鏁版嵁娴佹晥鏋?- 鍨傜洿涓嬭惤鐨勫厜鐐?
 */
@Composable
fun DataStreamEffect(
    modifier: Modifier = Modifier,
    streamCount: Int = 20,
    color: Color = NeonCyan
) {
    val streams = remember { List(streamCount) { DataStream.random(color) } }
    val infiniteTransition = rememberInfiniteTransition(label = "datastream")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stream_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        streams.forEach { stream ->
            val y = (stream.startY + progress * stream.speed * size.height) % size.height
            val alpha = if (y < size.height * 0.1f) y / (size.height * 0.1f) else 1f

            // 缁樺埗鏁版嵁娴佺嚎娈?
            drawLine(
                color = stream.color.copy(alpha = stream.alpha * alpha * 0.6f),
                start = Offset(stream.x, y - stream.length),
                end = Offset(stream.x, y),
                strokeWidth = stream.width
            )
            // 澶撮儴浜偣
            drawLine(
                color = stream.color.copy(alpha = stream.alpha * alpha),
                start = Offset(stream.x, y - 4f),
                end = Offset(stream.x, y),
                strokeWidth = stream.width + 1f
            )
        }
    }
}

/**
 * 绮掑瓙鏁版嵁绫?- 宸插簾寮冿紝淇濈暀鍏煎鎬?
 */
@Composable
fun ParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    colors: List<Color> = listOf(NeonCyan, NeonPink, NeonPurple)
) {
    // 鏀圭敤鏁版嵁娴佹晥鏋滄浛浠ｆ煍鍜岀矑瀛?
    DataStreamEffect(
        modifier = modifier,
        streamCount = particleCount / 2,
        color = colors.first()
    )
}

private data class DataStream(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val length: Float,
    val width: Float,
    val color: Color,
    val alpha: Float
) {
    companion object {
        fun random(baseColor: Color): DataStream {
            return DataStream(
                x = Random.nextFloat() * 1000f,
                startY = Random.nextFloat() * -2000f,
                speed = Random.nextFloat() * 0.8f + 0.2f,
                length = Random.nextFloat() * 60f + 20f,
                width = Random.nextFloat() * 2f + 1f,
                color = baseColor,
                alpha = Random.nextFloat() * 0.5f + 0.3f
            )
        }
    }
}

