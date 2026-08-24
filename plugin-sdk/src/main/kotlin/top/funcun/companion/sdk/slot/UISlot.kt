package top.funcun.companion.sdk.slot

/**
 * UI 插槽。
 * 插件将 Compose 组件注册到指定插槽中，宿主负责渲染。
 */
enum class UISlot {
    // 专注全屏页
    FOCUS_FULLSCREEN,
    FOCUS_TIMER,
    FOCUS_AFFECTION_BAR,
    FOCUS_BOTTOM_CONTROLS,

    // 浮窗
    OVERLAY_COMPANION,
    OVERLAY_WARNING,

    // 首页
    HOME_TOP,
    HOME_CARD,

    // 统计页
    STATS_CHART,
    STATS_CARD,

    // 设置页
    SETTINGS_SECTION,

    // 通知栏
    NOTIFICATION_CONTENT,
}
