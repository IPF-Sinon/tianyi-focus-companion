package top.funcun.companion.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 依见钟勤形状系统。
 * MIUI 风格：大圆角、胶囊按钮。
 */
object TianyiShapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val Full = RoundedCornerShape(percent = 50)

    /** 转换为 Material3 Shapes */
    fun toMaterial3(): Shapes = Shapes(
        extraSmall = Small,
        small = Small,
        medium = Medium,
        large = Large,
        extraLarge = Large,
    )
}
