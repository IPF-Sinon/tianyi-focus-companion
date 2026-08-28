package top.funcun.companion.theme

import kotlinx.serialization.Serializable

/**
 * 主题配置。
 *
 * 字段名与 FolkPatch 的 theme.json 对齐，以便直接读取 FolkPatch 主题包（.fpt）。
 * 不认识的字段忽略，缺失字段用默认值（向前兼容）。
 */
@Serializable
data class ThemeConfig(
    // 元信息
    val metaName: String = "默认主题",
    val metaAuthor: String = "",
    val metaVersion: String = "1.0",
    val metaDescription: String = "",

    // 配色
    val customColor: String = "#4A90E2",           // 种子色
    val useSystemDynamicColor: Boolean = false,     // Material You 动态取色
    val colorGenerationMode: String = "classic",    // classic / dynamic
    val colorStandard: String = "MD3_2021",         // MD3_2021 / MD3_2025
    val colorStyle: String = "TONAL_SPOT",          // TONAL_SPOT / VIBRANT / EXPRESSIVE ...

    // 深色模式
    val nightModeEnabled: Boolean = false,
    val nightModeFollowSys: Boolean = true,

    // 背景
    val isBackgroundEnabled: Boolean = false,
    val backgroundOpacity: Float = 1.0f,
    val backgroundBlur: Float = 0f,
    val backgroundDim: Float = 0.2f,
    val isDualBackgroundDimEnabled: Boolean = false,
    val backgroundDayDim: Float = 0.1f,
    val backgroundNightDim: Float = 0.4f,

    // 字体
    val isFontEnabled: Boolean = false,

    // 布局
    val homeLayoutStyle: String = "dashboard",       // dashboard / simple
    val cardCornerRadius: Int = 24,

    // 底栏
    val navBarStyle: String = "floating",             // floating（悬浮胶囊）/ standard（常规通栏）
    val navBarCompact: Boolean = true,                // true 胶囊 / false 大圆角
) {
    companion object {
        /** theme.json 里资源文件名约定（与 FolkPatch 对齐） */
        const val CONFIG_FILE = "theme.json"
        const val BACKGROUND_FILE = "background.jpg"
        const val FONT_FILE = "font.ttf"
        const val PREVIEW_FILE = "preview.png"

        /** 默认主题（蓝白） */
        val DEFAULT = ThemeConfig()
    }
}