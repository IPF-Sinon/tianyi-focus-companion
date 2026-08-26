package top.funcun.companion.plugin.onboarding

/**
 * 权限分级，参考 Operit 的 AndroidPermissionLevel 概念。
 * 不同级别对应不同的授权方式与用户操作成本。
 */
enum class PermissionLevel(
    val label: String,
    val hint: String,
) {
    /** 基础权限：运行时弹窗直接授权 */
    BASIC("基础", "弹窗授权"),

    /** 设置页权限：跳转系统设置页开启 */
    SETTINGS("设置", "跳转设置页"),

    /** 高级权限：跳转特殊系统设置页 */
    ADVANCED("高级", "特殊设置页"),

    /** 无障碍权限：需在无障碍设置中手动开启服务 */
    ACCESSIBILITY("无障碍", "开启辅助服务"),
}
