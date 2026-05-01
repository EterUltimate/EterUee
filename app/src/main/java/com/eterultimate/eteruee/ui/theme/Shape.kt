package com.eterultimate.eteruee.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 璧涘崥鏈嬪厠涓婚锛氶浂鍦嗚锛屽叏鐩磋
// 浣跨敤 0.dp 鐨?RoundedCornerShape 浣滀负 CornerBasedShape 鐨勫疄渚?
val CyberpunkShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

// MaterialExpressiveTheme 浣跨敤 expressive shapes (largeIncreased 绛?
// 杩欎簺閫氳繃 Shapes() 鏋勯€犲嚱鏁扮殑 large/extraLarge 宸茶鐩栦负 0.dp

