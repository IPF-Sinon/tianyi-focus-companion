package top.funcun.companion.sdk

import androidx.compose.runtime.Composable

/**
 * UI 覆写插件。
 *
 * 实现此接口的插件可以**完全接管应用界面**（底栏 + 全部页面）。
 *
 * 互斥规则（由宿主保证）：
 * - 系统中同时只有一个 UI 覆写插件生效
 * - 存在多个时，按安装时间自动启用最晚安装的一个，其余禁用
 * - 用户可在插件页手动切换生效的界面插件
 * - 有 UI 覆写插件生效时，官方原生界面自动让位
 */
interface UiOverridePlugin : Plugin {

    /** 覆写界面的展示名（插件页显示用） */
    val overrideLabel: String
        get() = "自定义界面"

    /**
     * 渲染整个应用界面。
     *
     * 插件在此绘制自己的底栏与全部页面，通过 [host] 访问宿主能力
     * （插件列表、配置读写、卸载、导航数据、主题快照等）。
     */
    @Composable
    fun RenderApp(host: UiHostApi)
}