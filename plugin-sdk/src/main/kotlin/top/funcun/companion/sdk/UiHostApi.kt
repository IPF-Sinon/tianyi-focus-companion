package top.funcun.companion.sdk

/**
 * 宿主能力接口，供 UI 覆写插件（[UiOverridePlugin]）访问。
 *
 * 只暴露与 Compose 无关的纯数据/操作，第三方界面据此渲染。
 */
interface UiHostApi {

    /** 全部已加载插件摘要 */
    fun getPlugins(): List<PluginSummary>

    /** 全部插件贡献的导航项（按 order 排序） */
    fun getNavItems(): List<NavItem>

    /** 指定插件的配置 schema（无则 null） */
    fun getConfigSchema(pluginId: String): ConfigSchema?

    /** 读配置值 */
    fun readConfig(pluginId: String, key: String, defaultValue: String): String

    /** 写配置值 */
    fun writeConfig(pluginId: String, key: String, value: String)

    /** 卸载插件（内置插件返回 false） */
    fun uninstallPlugin(pluginId: String): Boolean

    /** 触发插件动作 */
    fun invokeAction(pluginId: String, actionId: String): String?

    /** 请求插件导航页数据 */
    fun requestNavData(pluginId: String, navId: String): String?

    /** 当前主题快照（第三方界面也可跟随主题配色） */
    fun getThemeSnapshot(): ThemeSnapshot

    /** 应用信息 */
    fun getAppInfo(): AppInfo
}

/** 插件摘要 */
data class PluginSummary(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val enabled: Boolean,
    val builtin: Boolean,
    val icon: String,
    val hasConfig: Boolean,
    val isUiOverride: Boolean,
    val actions: List<PluginAction>,
)

/** 应用信息 */
data class AppInfo(
    val appName: String,
    val versionName: String,
    val versionCode: Long,
)

/**
 * 主题快照：把当前主题的关键色/参数以中性形式暴露，
 * 供第三方 UI 覆写插件跟随主题。
 */
data class ThemeSnapshot(
    val seedColorArgb: Int,
    val isDark: Boolean,
    val cardCornerRadiusDp: Int,
    val backgroundEnabled: Boolean,
)