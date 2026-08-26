package top.funcun.companion.theme

import androidx.compose.ui.graphics.Color

/**
 * 依见钟勤 蓝白系色彩系统。
 * 对标 Web 设计稿：清爽蓝白渐变、圆角卡片、柔和阴影。
 */
object TianyiColors {
    // 主色调（蓝）
    val Primary = Color(0xFF4A90E2)        // 主蓝
    val PrimaryLight = Color(0xFF66CCFF)   // 亮蓝（渐变起点）
    val PrimaryDark = Color(0xFF2E6DB4)    // 深蓝

    // 背景
    val Background = Color(0xFFEEF3FA)     // 浅蓝灰背景
    val Surface = Color(0xFFFFFFFF)        // 纯白
    val CardSurface = Color(0xFFF7FBFF)    // 卡片底（浅蓝白）

    // 文字
    val TextPrimary = Color(0xFF0B1A33)    // 深蓝黑
    val TextSecondary = Color(0xFF6B7A93)  // 蓝灰
    val TextTertiary = Color(0xFF8A9BB5)   // 浅蓝灰
    val TextOnPrimary = Color(0xFFFFFFFF)  // 蓝底白字

    // 分割线
    val Divider = Color(0xFFE4ECF5)        // 浅蓝分割

    // 状态色
    val Error = Color(0xFFE05555)          // 警告红
    val Success = Color(0xFF4ADE80)        // 完成绿
    val Warning = Color(0xFFF5A623)        // 警告橙

    // 好感度等级色
    val AffectionIntimate = Color(0xFFFF6B9D)   // 亲密
    val AffectionFriendly = Color(0xFFFF8FA3)   // 友好
    val AffectionNeutral = Color(0xFFB0C4DE)    // 普通
    val AffectionCold = Color(0xFF6E8BB8)       // 冷淡
    val AffectionHeartbroken = Color(0xFF4A4A4A) // 心碎
}

/**
 * 依见钟勤暗色变体（蓝黑）。
 */
object TianyiDarkColors {
    val Primary = Color(0xFF66CCFF)        // 亮蓝
    val PrimaryLight = Color(0xFF2E6DB4)   // 中蓝
    val PrimaryDark = Color(0xFFA8D8FF)    // 极亮蓝

    val Background = Color(0xFF0F1620)     // 深蓝黑
    val Surface = Color(0xFF1A2332)        // 深卡片
    val CardSurface = Color(0xFF1E293B)    // 深卡片底

    val TextPrimary = Color(0xFFECF2FA)    // 近白
    val TextSecondary = Color(0xFF8CA3C0)  // 蓝灰
    val TextTertiary = Color(0xFF64748B)   // 浅蓝灰
    val TextOnPrimary = Color(0xFF0B1A33)  // 蓝底深字

    val Divider = Color(0xFF2A3A52)        // 深分割

    val Error = Color(0xFFFF8A80)          // 亮红
    val Success = Color(0xFF6EE7A0)        // 亮绿
    val Warning = Color(0xFFFFC968)        // 亮橙
}