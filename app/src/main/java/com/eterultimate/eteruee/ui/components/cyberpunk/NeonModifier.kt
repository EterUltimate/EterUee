package com.eterultimate.eteruee.ui.components.cyberpunk

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.ui.theme.presets.NeonCyan

/**
 * 纭湕闇撹櫣鍙戝厜鏁堟灉 Modifier
 * 鐩磋杈规 + 閿愬埄鍏夋檿锛屾棤鍦嗚
 */
fun Modifier.neonGlow(
    color: Color = NeonCyan,
    radius: Dp = 8.dp,
    strokeWidth: Dp = 1.5.dp
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawNeonStroke(color, radius, strokeWidth)
    }
)

/**
 * 闇撹櫣杈规缁樺埗 - 鐩磋鐗堟湰
 */
private fun ContentDrawScope.drawNeonStroke(
    color: Color,
    radius: Dp,
    strokeWidth: Dp
) {
    // 澶栧彂鍏夊眰
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.style = PaintingStyle.Stroke
            this.strokeWidth = strokeWidth.toPx()
        }
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = color.copy(alpha = 0f).toArgb()
        frameworkPaint.setShadowLayer(
            radius.toPx(), 0f, 0f,
            color.copy(alpha = 0.6f).toArgb()
        )
        canvas.drawRect(
            0f, 0f, size.width, size.height,
            paint
        )
    }
    // 瀹炰綋鐩磋杈规
    drawRect(
        color = color,
        size = size,
        style = Stroke(width = strokeWidth.toPx())
    )
}

/**
 * 纭湕瑙掓爣瑁呴グ Modifier
 * 鍦ㄥ洓涓缁樺埗 L 褰㈡爣璁?
 */
fun Modifier.cornerBrackets(
    color: Color = NeonCyan,
    strokeWidth: Dp = 2.dp,
    bracketLength: Dp = 12.dp
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawCornerBrackets(color, strokeWidth, bracketLength)
    }
)

private fun ContentDrawScope.drawCornerBrackets(
    color: Color,
    strokeWidth: Dp,
    bracketLength: Dp
) {
    val len = bracketLength.toPx()
    val sw = strokeWidth.toPx()
    val w = size.width
    val h = size.height

    // 宸︿笂
    drawLine(color, Offset(0f, 0f), Offset(len, 0f), sw)
    drawLine(color, Offset(0f, 0f), Offset(0f, len), sw)
    // 鍙充笂
    drawLine(color, Offset(w - len, 0f), Offset(w, 0f), sw)
    drawLine(color, Offset(w, 0f), Offset(w, len), sw)
    // 宸︿笅
    drawLine(color, Offset(0f, h - len), Offset(0f, h), sw)
    drawLine(color, Offset(0f, h), Offset(len, h), sw)
    // 鍙充笅
    drawLine(color, Offset(w - len, h), Offset(w, h), sw)
    drawLine(color, Offset(w, h - len), Offset(w, h), sw)
}

/**
 * 宸ヤ笟椋庢牸杈规 - 鏃犲彂鍏夛紝绾嚎鏉?
 */
fun Modifier.industrialBorder(
    color: Color = SteelGray,
    strokeWidth: Dp = 1.dp
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawRect(
            color = color,
            size = size,
            style = Stroke(width = strokeWidth.toPx())
        )
    }
)

