package top.funcun.companion.sdk

import kotlinx.serialization.Serializable

/**
 * 插件配置项类型。
 * 主题通过 Bridge 读取 schema 后自行渲染表单控件。
 */
@Serializable
enum class ConfigFieldType {
    /** 开关 */
    BOOLEAN,

    /** 单行文本 */
    TEXT,

    /** 整数 */
    INT,

    /** 单选（options 中选一个） */
    SELECT,

    /** 多选（options 中选多个） */
    MULTI_SELECT,
}

/**
 * 插件配置项声明。
 *
 * @param key 存储键（写入插件私有 dataStore）
 * @param label 显示名称
 * @param type 控件类型
 * @param defaultValue 默认值（字符串形式，BOOLEAN 用 "true"/"false"）
 * @param description 说明文字（可选）
 * @param options SELECT / MULTI_SELECT 的候选项（value 到 label）
 * @param min INT 类型的最小值（可选）
 * @param max INT 类型的最大值（可选）
 */
@Serializable
data class ConfigField(
    val key: String,
    val label: String,
    val type: ConfigFieldType,
    val defaultValue: String = "",
    val description: String = "",
    val options: List<ConfigOption> = emptyList(),
    val min: Int? = null,
    val max: Int? = null,
)

/** 配置候选项 */
@Serializable
data class ConfigOption(
    val value: String,
    val label: String,
)

/**
 * 插件配置分组。
 */
@Serializable
data class ConfigSection(
    val title: String,
    val fields: List<ConfigField> = emptyList(),
)

/**
 * 插件配置 Schema。
 *
 * 插件通过 [Plugin.configSchema] 声明自己的配置项；
 * 主题从 Bridge 拿到 JSON 后渲染表单，用户修改后写回插件 dataStore。
 *
 * 若插件希望完全自定义配置界面，可在 [customHtml] 指定 assets 中的 HTML 文件名，
 * 主题应改为渲染该 HTML（接管配置界面）。
 */
@Serializable
data class ConfigSchema(
    val sections: List<ConfigSection> = emptyList(),
    /** 自定义配置界面 HTML（相对插件 assets 根目录），非空时主题应渲染此 HTML */
    val customHtml: String? = null,
)

/**
 * 插件声明的额外动作（显示为插件卡片上的按钮）。
 *
 * @param id 动作标识，Bridge 调用 invokePluginAction(pluginId, actionId) 时传回
 * @param label 按钮文字
 * @param icon 可选 emoji/图标标识，主题自行决定如何渲染
 * @param destructive 是否为危险操作（主题可用红色强调）
 */
@Serializable
data class PluginAction(
    val id: String,
    val label: String,
    val icon: String = "",
    val destructive: Boolean = false,
)