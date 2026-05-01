package com.eterultimate.eteruee.ui.components.cyberpunk

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.ui.theme.presets.NeonCyan
import com.eterultimate.eteruee.ui.theme.presets.NeonPink
import com.eterultimate.eteruee.ui.theme.presets.TextPrimary

/**
 * 纭湕鏁呴殰鏂囧瓧鏁堟灉
 * 閿愬埄鍋忕Щ + 鏃犲钩婊戣繃娓★紝宸ヤ笟鎰?
 */
@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    glitchIntensity: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitch_offset"
    )

    Box(modifier = modifier) {
        // 闈掕壊鍋忕Щ灞?
        Text(
            text = text,
            style = style.copy(fontWeight = FontWeight.Black),
            color = NeonCyan.copy(alpha = 0.8f),
            modifier = Modifier.offset(x = (offsetX * glitchIntensity).dp)
        )
        // 绮夎壊鍋忕Щ灞?
        Text(
            text = text,
            style = style.copy(fontWeight = FontWeight.Black),
            color = NeonPink.copy(alpha = 0.8f),
            modifier = Modifier.offset(x = (-offsetX * glitchIntensity).dp)
        )
        // 涓绘枃瀛楀眰
        Text(
            text = text,
            style = style.copy(fontWeight = FontWeight.Black),
            color = TextPrimary
        )
    }
}

/**
 * 闈欐€佹晠闅滄枃瀛楋紙鏃犲姩鐢伙級
 */
@Composable
fun GlitchTextStatic(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    glitchColor1: Color = NeonCyan,
    glitchColor2: Color = NeonPink
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            style = style.copy(fontWeight = FontWeight.Black),
            color = glitchColor1.copy(alpha = 0.6f),
            modifier = Modifier.offset(x = (-2).dp)
        )
        Text(
            text = text,
            style = style.copy(fontWeight = FontWeight.Black),
            color = glitchColor2.copy(alpha = 0.6f),
            modifier = Modifier.offset(x = 2.dp)
        )
        Text(
            text = text,
            style = style.copy(fontWeight = FontWeight.Black),
            color = TextPrimary
        )
    }
}

