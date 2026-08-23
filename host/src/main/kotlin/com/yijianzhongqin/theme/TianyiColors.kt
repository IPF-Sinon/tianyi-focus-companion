package com.yijianzhongqin.theme

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
