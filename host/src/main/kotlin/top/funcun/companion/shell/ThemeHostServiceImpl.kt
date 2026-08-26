package top.funcun.companion.shell

import android.content.Context
import top.funcun.companion.sdk.ThemeHostService

/**
 * [ThemeHostService] 的宿主实现。
 *
 * 把 PluginManager 的能力以 JSON 形式暴露给主题插件（WebView 主题）。
 */
class ThemeHostServiceImpl(
    private val context: Context,
    private val pluginManager: PluginManager,
) : ThemeHostService {

    override fun getPluginsJson(): String {
        val plugins = pluginManager.getBuiltinPluginInfo()
        val items = plugins.joinToString(",") { info ->
            val actions = info.actions.joinToString(",") { action ->
                """{"id":${jsonStr(action.id)},"label":${jsonStr(action.label)},""" +
                    """"icon":${jsonStr(action.icon)},"destructive":${action.destructive}}"""
            }
            """{"id":${jsonStr(info.id)},"name":${jsonStr(info.name)},""" +
                """"description":${jsonStr(info.description)},"version":${jsonStr(info.version)},""" +
                """"enabled":${info.enabled},"builtin":${info.builtin},""" +
                """"icon":${jsonStr(info.icon)},"hasConfig":${info.hasConfig},""" +
                """"actions":[$actions]}"""
        }
        return """{"plugins":[$items]}"""
    }

    override fun getNavItemsJson(): String {
        val items = pluginManager.getNavItems().joinToString(",") { item ->
            """{"id":${jsonStr(item.id)},"label":${jsonStr(item.label)},""" +
                """"icon":${jsonStr(item.icon)},"order":${item.order},""" +
                """"pluginId":${jsonStr(item.pluginId)}}"""
        }
        return """{"items":[$items]}"""
    }

    override fun getConfigSchemaJson(pluginId: String): String? {
        val schema = pluginManager.getConfigSchema(pluginId) ?: return null
        val sections = schema.sections.joinToString(",") { section ->
            val fields = section.fields.joinToString(",") { field ->
                val options = field.options.joinToString(",") { option ->
                    """{"value":${jsonStr(option.value)},"label":${jsonStr(option.label)}}"""
                }
                val currentValue = pluginManager.readConfigValue(
                    pluginId, field.key, field.defaultValue,
                )
                buildString {
                    append("{")
                    append(""""key":${jsonStr(field.key)},""")
                    append(""""label":${jsonStr(field.label)},""")
                    append(""""type":${jsonStr(field.type.name)},""")
                    append(""""defaultValue":${jsonStr(field.defaultValue)},""")
                    append(""""value":${jsonStr(currentValue)},""")
                    append(""""description":${jsonStr(field.description)},""")
                    append(""""options":[$options]""")
                    field.min?.let { append(""","min":$it""") }
                    field.max?.let { append(""","max":$it""") }
                    append("}")
                }
            }
            """{"title":${jsonStr(section.title)},"fields":[$fields]}"""
        }
        val customHtml = schema.customHtml?.let { jsonStr(it) } ?: "null"
        return """{"sections":[$sections],"customHtml":$customHtml}"""
    }

    override fun readConfig(pluginId: String, key: String, defaultValue: String): String =
        pluginManager.readConfigValue(pluginId, key, defaultValue)

    override fun writeConfig(pluginId: String, key: String, value: String) {
        pluginManager.writeConfigValue(pluginId, key, value)
    }

    override fun uninstallPlugin(pluginId: String): Boolean =
        kotlinx.coroutines.runBlocking {
            pluginManager.uninstall(pluginId).isSuccess
        }

    override fun invokeAction(pluginId: String, actionId: String): String? =
        kotlinx.coroutines.runBlocking {
            pluginManager.invokeAction(pluginId, actionId)
        }

    override fun requestNavData(pluginId: String, navId: String): String? =
        kotlinx.coroutines.runBlocking {
            pluginManager.requestNavData(pluginId, navId)
        }

    override fun getAppInfoJson(): String {
        val pkg = context.packageName
        val info = runCatching {
            context.packageManager.getPackageInfo(pkg, 0)
        }.getOrNull()
        val versionName = info?.versionName ?: "unknown"
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
            info?.longVersionCode ?: 0L
        } else {
            @Suppress("DEPRECATION")
            (info?.versionCode ?: 0).toLong()
        }
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        return """{"appName":${jsonStr(appName)},"versionName":${jsonStr(versionName)},"versionCode":$versionCode}"""
    }

    /** 最简 JSON 字符串转义 */
    private fun jsonStr(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}