package top.funcun.companion.sdk

import kotlinx.serialization.Serializable

/**
 * 主题导航项声明。
 *
 * 宿主聚合所有插件注册的导航项 + 内置项，供主题渲染底栏/侧栏。
 *
 * @param id 唯一标识，主题切换页面时回传给 Bridge
 * @param label 显示文字
 * @param icon emoji 或图标名，主题自行解析
 * @param order 排序权重（小的在前）
 * @param pluginId 提供该导航项的插件 id（内置项为空）
 */
@Serializable
data class NavItem(
    val id: String,
    val label: String,
    val icon: String = "",
    val order: Int = 100,
    val pluginId: String = "",
)