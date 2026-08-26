package top.funcun.companion.sdk

import top.funcun.companion.sdk.util.ServiceToken

/**
 * 主题宿主服务。
 *
 * 由宿主（host）注册，主题插件通过该接口获取插件列表、导航项、
 * 插件配置 schema/值，以及触发卸载/自定义动作。
 *
 * 这是「第三方主题」与宿主之间的唯一契约，主题不直接依赖 host 类。
 */
interface ThemeHostService {

    /** 插件列表 JSON：{"plugins":[{id,name,description,version,enabled,builtin,icon,hasConfig,actions:[...]}]} */
    fun getPluginsJson(): String

    /** 导航项 JSON：{"items":[{id,label,icon,order,pluginId}]} */
    fun getNavItemsJson(): String

    /** 指定插件的配置 schema JSON（无配置返回 null） */
    fun getConfigSchemaJson(pluginId: String): String?

    /** 读取插件配置值 */
    fun readConfig(pluginId: String, key: String, defaultValue: String): String

    /** 写入插件配置值 */
    fun writeConfig(pluginId: String, key: String, value: String)

    /** 卸载插件（内置插件返回 false） */
    fun uninstallPlugin(pluginId: String): Boolean

    /** 触发插件自定义动作，返回插件回执 JSON（可为 null） */
    fun invokeAction(pluginId: String, actionId: String): String?

    /** 请求插件导航页数据 JSON */
    fun requestNavData(pluginId: String, navId: String): String?

    /** 应用信息 JSON：{"appName","versionName","versionCode"} */
    fun getAppInfoJson(): String
}

/** 主题宿主服务 Token */
val ThemeHostServiceToken = ServiceToken.create<ThemeHostService>("theme_host_service")