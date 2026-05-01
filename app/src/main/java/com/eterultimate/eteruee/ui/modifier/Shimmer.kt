package com.eterultimate.eteruee.ui.modifier

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

/**
 * 涓?Composable 娣诲姞 Shimmer 鍔犺浇鏁堟灉鐨?Modifier.
 *
 * @param isLoading 鏄惁鏄剧ず Shimmer 鏁堟灉銆?
 * @param shimmerColor 闂厜鐨勪寒鑹查儴鍒嗐€?
 * @param backgroundColor Shimmer 娓愬彉鐨勮儗鏅壊锛堥€氬父鏄崐閫忔槑鐨勶紝浠ユ贩鍚堝師濮嬪唴瀹癸級銆?
 * @param durationMillis 鍔ㄧ敾瀹屾垚涓€娆℃壂鎻忕殑鏃堕暱锛堟绉掞級銆?
 * @param angle 闂厜鏁堟灉鐨勮搴︼紙搴︼級銆? 搴︽槸浠庡乏鍒板彸锛?0 搴︽槸浠庝笂鍒颁笅銆?
 * @param gradientWidthRatio 闂厜娓愬彉瀹藉害鐩稿浜庣粍浠跺昂瀵哥殑姣斾緥銆備緥濡傦紝0.5f 琛ㄧず闂厜瀹藉害涓虹粍浠跺搴︾殑涓€鍗娿€?
 */
@Composable
fun Modifier.shimmer(
    isLoading: Boolean,
    shimmerColor: Color = LocalContentColor.current.copy(alpha = 0.3f), // 杈冧寒鐨勯棯鍏夐鑹?
    backgroundColor: Color = LocalContentColor.current.copy(alpha = 0.9f), // 杈冩殫鐨勮儗鏅?鍩虹棰滆壊
    durationMillis: Int = 1200,
    angle: Float = 20f, // 绋嶅井鍊炬枩鐨勮搴?
    gradientWidthRatio: Float = 0.5f // 闂厜瀹藉害涓虹粍浠跺搴︾殑涓€鍗?
): Modifier = composed { // 浣跨敤 composed 浠ヤ究鍦?Modifier 鍐呴儴浣跨敤 remember 鍜?LaunchedEffect 绛?
    if (!isLoading) {
        // 濡傛灉涓嶅浜庡姞杞界姸鎬侊紝鍒欎笉搴旂敤浠讳綍鏁堟灉
        this
    } else {
        // 璁颁綇缁勪欢鐨勫昂瀵革紝浠ヤ究璁＄畻娓愬彉
        var size by remember { mutableStateOf(IntSize.Zero) }
        // 鍒涘缓鏃犻檺寰幆鍔ㄧ敾
        val transition = rememberInfiniteTransition(label = "ShimmerTransition")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f, // 鍔ㄧ敾鍊间粠 0 鍒?1
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart // 姣忔閮戒粠澶村紑濮?
            ),
            label = "ShimmerTranslate"
        )
        // 灏嗚搴﹁浆鎹负寮у害
        val angleRad = Math.toRadians(angle.toDouble()).toFloat()
        // 璁＄畻娓愬彉棰滆壊鐨勫垪琛?
        val colors = remember(shimmerColor, backgroundColor) {
            listOf(
                backgroundColor, // 寮€濮嬬殑鑳屾櫙鑹?
                shimmerColor,    // 涓棿鐨勪寒鑹?
                backgroundColor  // 缁撴潫鐨勮儗鏅壊
            )
        }
        // 搴旂敤缁樺埗鏁堟灉
        this
            .onGloballyPositioned { layoutCoordinates ->
                // 鑾峰彇缁勪欢鐨勫疄闄呭昂瀵?
                size = layoutCoordinates.size
            }
            .graphicsLayer { alpha = 0.99f } // 寮€鍚贩鍚?
            .drawWithContent { // 浣跨敤 drawWithContent 鑾峰彇缁樺埗涓婁笅鏂?
                if (size == IntSize.Zero) {
                    // 濡傛灉灏哄鏈煡锛屽厛缁樺埗鍘熷鍐呭
                    drawContent()
                    return@drawWithContent
                }
                val width = size.width.toFloat()
                val height = size.height.toFloat()
                // 璁＄畻娓愬彉鐨勫疄闄呭搴︼紙鍍忕礌锛?
                // 鎴戜滑闇€瑕佽€冭檻瀵硅绾块暱搴︼紝浠ョ‘淇濆€炬枩鏃惰兘瀹屽叏瑕嗙洊
                val diagonal = kotlin.math.sqrt(width * width + height * height)
                val gradientWidth = diagonal * gradientWidthRatio
                // 璁＄畻鍔ㄧ敾褰撳墠浣嶇疆鐨勫亸绉婚噺
                // 鍔ㄧ敾鍊间粠 0 鍒?1锛屾槧灏勫埌绉诲姩璺濈
                // 鎬荤Щ鍔ㄨ窛绂婚渶瑕佽鐩栫粍浠跺姞涓婃笎鍙樺搴︼紝纭繚瀹屽叏鎵繃
                // 鎴戜滑璁╁畠浠庡畬鍏ㄥ湪缁勪欢宸?涓婁晶寮€濮嬶紝绉诲姩鍒板畬鍏ㄥ湪鍙?涓嬩晶缁撴潫
                val totalDistance = diagonal + gradientWidth
                val currentOffset = translateAnimation.value * totalDistance - gradientWidth
                // 璁＄畻娓愬彉鐨勮捣濮嬬偣鍜岀粨鏉熺偣锛岃€冭檻瑙掑害
                val startX = currentOffset * kotlin.math.cos(angleRad)
                val startY = currentOffset * kotlin.math.sin(angleRad)
                val endX = (currentOffset + gradientWidth) * kotlin.math.cos(angleRad)
                val endY = (currentOffset + gradientWidth) * kotlin.math.sin(angleRad)
                // 鍒涘缓绾挎€ф笎鍙?Brush
                val shimmerBrush = Brush.linearGradient(
                    colors = colors,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    tileMode = TileMode.Clamp // Clamp 妯″紡纭繚娓愬彉棰滆壊鍦ㄨ竟缂樺鍥哄畾
                )
                // 1. 鍏堢粯鍒跺師濮嬪唴瀹?
                drawContent()
                // 2. 鍦ㄥ師濮嬪唴瀹逛箣涓婄粯鍒朵竴涓煩褰紝浣跨敤 Shimmer Brush 鍜?DstIn 娣峰悎妯″紡
                // BlendMode.DstIn: 鍙繚鐣欑洰鏍囷紙鍘熷鍐呭锛変笌婧愶紙Shimmer娓愬彉锛夐噸鍙犵殑閮ㄥ垎锛?
                // 骞朵笖浣跨敤婧愮殑 Alpha 鍊笺€傝繖浣垮緱娓愬彉浜儴鏄剧ず鍐呭锛屾殫閮紙閫忔槑閮級闅愯棌鍐呭銆?
                drawRect(
                    brush = shimmerBrush,
                    blendMode = BlendMode.DstIn
                )
            }
    }
}

