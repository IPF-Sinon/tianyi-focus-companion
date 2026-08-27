package top.funcun.companion.sdk

import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 插件接口。
 * 所有功能必须以插件形式实现此接口，通过 PluginManager 注册。
 */
interface Plugin {

    /** 唯一标识，如 "top.funcun.companion.plugin.focus" */
    val id: PluginId

    /** 显示名称，如 "番茄钟引擎" */
    val name: String

    /** 语义化版本号 */
    val version: SemVer

    /** 简短描述 */
    val description: String

    /** 图标资源 ID */
    val icon: Int

    /** 依赖的其他插件 ID */
    val dependencies: List<PluginId>

    /** 需要的 Android 运行时权限 */
    val permissions: List<String>

    /**
     * 插件是否为内置核心插件（不可卸载）。
     * 主题据此隐藏「卸载」按钮。
     */
    val builtin: Boolean
        get() = false

    /**
     * 图标标识（emoji 或图标名），主题渲染插件卡片时使用。
     * 优先级高于 [icon] 资源 ID。
     */
    val iconEmoji: String
        get() = "🧩"

    /**
     * 配置项声明。返回 null 表示该插件无可配置项（主题隐藏「配置」按钮）。
     *
     * 主题从 Bridge 获取 schema JSON 后渲染表单；
     * 若 schema 指定了 customHtml，则主题应渲染该 HTML 接管配置界面。
     */
    val configSchema: ConfigSchema?
        get() = null

    /**
     * 插件声明的额外动作（显示为插件卡片上的按钮）。
     * 主题渲染这些按钮，点击后通过 Bridge 回调 [onAction]。
     */
    val actions: List<PluginAction>
        get() = emptyList()

    /**
     * 插件贡献的导航项（主题底栏/侧栏页面）。
     * 主题通过 Bridge 获取全部导航项后自行渲染。
     */
    val navItems: List<NavItem>
        get() = emptyList()

    /**
     * 主题请求某个导航页的数据时调用。
     *
     * 该方法在 WebView 的 JS 线程同步调用，实现必须**快速返回**（读缓存/prefs），
     * 耗时工作请在插件内部异步进行并缓存结果。
     *
     * @param navId 对应 [navItems] 中声明的 id
     * @return JSON 字符串，结构由插件与主题约定
     */
    fun getNavData(navId: String): String? = null

    /**
     * 处理主题触发的自定义动作。
     *
     * 同样在 JS 线程同步调用，需快速返回；重活请插件内部起协程处理。
     *
     * @param actionId 对应 [actions] 中声明的 id
     * @return 可选返回值（JSON 字符串），主题可据此更新界面
     */
    fun onAction(actionId: String): String? = null

    /**
     * 插件被加载时调用。
     * 在此方法中注册事件监听器、UI 插槽、服务等。
     */
    suspend fun onLoad(context: PluginContext)

    /**
     * 插件被启用时调用。
     * 此时插件开始正常工作。
     */
    suspend fun onEnable()

    /**
     * 插件被禁用时调用。
     * 此时插件应停止所有工作，但保留数据。
     */
    suspend fun onDisable()

    /**
     * 插件被卸载时调用。
     * 此时插件应清理所有资源。
     */
    suspend fun onUnload()
}