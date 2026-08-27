package top.funcun.companion.shell

import androidx.compose.ui.graphics.toArgb
import top.funcun.companion.sdk.AppInfo
import top.funcun.companion.sdk.ConfigSchema
import top.funcun.companion.sdk.NavItem
import top.funcun.companion.sdk.PluginSummary
import top.funcun.companion.sdk.ThemeSnapshot
import top.funcun.companion.sdk.UiHostApi
import top.funcun.companion.theme.ColorSchemeGenerator
import top.funcun.companion.theme.ThemeManager

/**
 * [UiHostApi] 的宿主实现。
 * 把 PluginManager 与主题能力暴露给 UI 覆写插件。
 */
class UiHostApiImpl(
    private val pluginManager: PluginManager,
) : UiHostApi {

    override fun getPlugins(): List<PluginSummary> =
        pluginManager.getBuiltinPluginInfo().map { info ->
            PluginSummary(
                id = info.id,
                name = info.name,
                description = info.description,
                version = info.version,
                enabled = info.enabled,
                builtin = info.builtin,
                icon = info.icon,
                hasConfig = info.hasConfig,
                isUiOverride = pluginManager.isUiOverride(info.id),
                actions = info.actions,
            )
        }

    override fun getNavItems(): List<NavItem> = pluginManager.getNavItems()

    override fun getConfigSchema(pluginId: String): ConfigSchema? =
        pluginManager.getConfigSchema(pluginId)

    override fun readConfig(pluginId: String, key: String, defaultValue: String): String =
        pluginManager.readConfigValue(pluginId, key, defaultValue)

    override fun writeConfig(pluginId: String, key: String, value: String) {
        pluginManager.writeConfigValue(pluginId, key, value)
    }

    override fun uninstallPlugin(pluginId: String): Boolean =
        pluginManager.uninstall(pluginId).isSuccess

    override fun invokeAction(pluginId: String, actionId: String): String? =
        pluginManager.invokeAction(pluginId, actionId)

    override fun requestNavData(pluginId: String, navId: String): String? =
        pluginManager.requestNavData(pluginId, navId)

    override fun getThemeSnapshot(): ThemeSnapshot {
        val cfg = ThemeManager.config
        val seed = ColorSchemeGenerator.parseColor(cfg.customColor)?.toArgb() ?: 0xFF4A90E2.toInt()
        return ThemeSnapshot(
            seedColorArgb = seed,
            isDark = cfg.nightModeEnabled,
            cardCornerRadiusDp = cfg.cardCornerRadius,
            backgroundEnabled = cfg.isBackgroundEnabled,
        )
    }

    override fun getAppInfo(): AppInfo {
        val ctx = pluginManager.getHostContextCompat()
        val pkg = ctx.packageName
        val info = runCatching { ctx.packageManager.getPackageInfo(pkg, 0) }.getOrNull()
        val appName = ctx.applicationInfo.loadLabel(ctx.packageManager).toString()
        return AppInfo(
            appName = appName,
            versionName = info?.versionName ?: "unknown",
            versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info?.longVersionCode ?: 0L
            } else {
                @Suppress("DEPRECATION")
                (info?.versionCode ?: 0).toLong()
            },
        )
    }
}