package top.funcun.companion.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 依见钟勤形状系统。
 * 蓝白设计稿风格：大圆角、胶囊按钮、卡片 24dp。
 */
object TianyiShapes {
    val Small = RoundedCornerShape(12.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val XLarge = RoundedCornerShape(32.dp)
    val Full = RoundedCornerShape(percent = 50)

    /** 气泡形状：大圆角 + 左下小圆角 */
    val Bubble = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 4.dp,
    )

    /** 转换为 Material3 Shapes */
    fun toMaterial3(): Shapes = Shapes(
        extraSmall = Small,
        small = Small,
        medium = Medium,
        large = Large,
        extraLarge = XLarge,
    )
}