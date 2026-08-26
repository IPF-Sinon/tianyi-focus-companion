package top.funcun.companion.theme

import androidx.compose.ui.graphics.Color

/**
 * 依见钟勤 MIUI 风格色彩系统。
 * 暖粉色调，低饱和，轻盈感。
 */
object TianyiColors {
    // 主色调
    val Primary = Color(0xFFE8A0BF)        // 天依粉
    val PrimaryLight = Color(0xFFF5D0E0)   // 浅粉
    val PrimaryDark = Color(0xFFC47A9A)    // 深粉

    // 背景
    val Background = Color(0xFFFAFAFA)     // 浅灰白
    val Surface = Color(0xFFFFFFFF)        // 纯白卡片

    // 文字
    val TextPrimary = Color(0xFF1A1A1A)    // 近黑
    val TextSecondary = Color(0xFF8A8A8A)  // 灰色辅助
    val TextOnPrimary = Color(0xFFFFFFFF)  // 粉色底上的文字

    // 分割线
    val Divider = Color(0xFFF0F0F0)        // 极浅分割

    // 状态色
    val Error = Color(0xFFE05555)          // 警告红
    val Success = Color(0xFF6BBF6B)        // 完成绿
    val Warning = Color(0xFFE8A840)        // 警告黄

    // 好感度等级色
    val AffectionIntimate = Color(0xFFFF6B9D)   // 亲密
    val AffectionFriendly = Color(0xFFFF8FA3)   // 友好
    val AffectionNeutral = Color(0xFFB0B0B0)    // 普通
    val AffectionCold = Color(0xFF6E8BB8)       // 冷淡
    val AffectionHeartbroken = Color(0xFF4A4A4A) // 心碎
}

/**
 * 依见钟勤暗色变体。
 * 深底 + 提亮粉，用于 Material3 darkColorScheme。
 */
object TianyiDarkColors {
    // 主色调（暗色下提亮）
    val Primary = Color(0xFFE5A9C4)        // 天依粉（亮）
    val PrimaryLight = Color(0xFF8E4A6E)   // 中粉
    val PrimaryDark = Color(0xFFF5D3E2)    // 亮粉

    // 背景
    val Background = Color(0xFF171417)     // 深暖灰
    val Surface = Color(0xFF211D21)        // 深卡片

    // 文字
    val TextPrimary = Color(0xFFECEAEC)    // 近白
    val TextSecondary = Color(0xFF9A949A)  // 灰辅助
    val TextOnPrimary = Color(0xFF3A1426)  // 粉色底上的深字

    // 分割线
    val Divider = Color(0xFF2E2A2E)        // 深分割

    // 状态色
    val Error = Color(0xFFFF8A80)          // 亮红
    val Success = Color(0xFF85C985)        // 亮绿
    val Warning = Color(0xFFFFC968)        // 亮黄
}