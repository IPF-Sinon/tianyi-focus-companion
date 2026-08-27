# UI 覆写插件开发指南（UiOverridePlugin）

UI 覆写插件可以**完全接管应用界面**（底栏 + 全部页面），用原生 Compose 写。

---

## 1. 能力边界

- 实现 `UiOverridePlugin` 接口 → 插件生效时，官方界面自动让位
- 通过 `UiHostApi` 访问宿主能力：插件列表、导航项、配置读写、卸载、插件动作、主题快照、应用信息
- **互斥**：系统中同一时刻只有一个 UI 覆写插件生效


## 2. 互斥与切换规则

| 情况 | 行为 |
|------|------|
| 0 个 UI 覆写插件 | 官方界面 |
| 1 个 UI 覆写插件 | 自动启用，官方界面让位 |
| 多个 UI 覆写插件 | 按安装时间启用**最晚安装**的一个，其余自动禁用 |
| 用户手动切换 | 在插件页点「启用此界面」，立即切换（旧的自动禁用） |

---

## 3. 接口

```kotlin
package top.funcun.companion.plugin.mytheme

import androidx.compose.runtime.Composable
import top.funcun.companion.sdk.*

class MyUiPlugin : UiOverridePlugin {
    override val id = PluginId("top.funcun.companion.plugin.mytheme")
    override val name = "我的界面"
    override val version = SemVer(1, 0, 0)
    override val description = "完全自定义的界面"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()
    override val iconEmoji = "🎨"

    override suspend fun onLoad(context: PluginContext) {}
    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    @Composable
    override fun RenderApp(host: UiHostApi) {
        // 在这里画你的整个应用界面（底栏 + 页面）
        // 用 host.getPlugins() / host.getNavItems() 等读取数据
    }
}
```

---

## 4. UiHostApi 可用方法

```kotlin
interface UiHostApi {
    fun getPlugins(): List<PluginSummary>       // 插件列表
    fun getNavItems(): List<NavItem>            // 插件贡献的导航项
    fun getConfigSchema(pluginId: String): ConfigSchema?
    fun readConfig(pluginId, key, default): String
    fun writeConfig(pluginId, key, value)
    fun uninstallPlugin(pluginId): Boolean
    fun invokeAction(pluginId, actionId): String?
    fun requestNavData(pluginId, navId): String?  // 插件页面数据
    fun getThemeSnapshot(): ThemeSnapshot         // 跟随主题配色
    fun getAppInfo(): AppInfo
}
```

`ThemeSnapshot` 提供 `seedColorArgb` / `isDark` / `cardCornerRadiusDp` / `backgroundEnabled`，
让第三方界面也能跟随用户选择的主题。

---

## 5. 打包与安装

1. 编译成 dex（`classes.dex`）+ 写 `plugin.toml`：

```toml
id = top.funcun.companion.plugin.mytheme
entry = top.funcun.companion.plugin.mytheme.MyUiPlugin
```

2. 放到应用插件目录，宿主通过 `DexClassLoader` 加载
3. 重启应用，宿主 `resolveActiveUiOverride()` 自动识别并启用

---

## 6. 注意事项

- **线程**：`UiHostApi` 的方法同步调用，重活请在你的插件内部起协程
- **安全**：插件是任意代码，用户应只安装可信来源
- **主题跟随**：建议从 `getThemeSnapshot()` 取 `seedColorArgb` 生成配色，或用 `dynamicColorScheme`
- **返回键**：用 Compose 的 `BackHandler` 自行处理