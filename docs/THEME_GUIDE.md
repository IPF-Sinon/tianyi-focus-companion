# 主题开发指南（Theme Development Guide）

依见钟勤的**全部界面**（底栏 + 专注/统计/插件/设置等页面）由一个 WebView「主题包」渲染。
第三方开发者可以编写纯 HTML/CSS/JS 主题，完全替换默认界面，甚至接管任何插件的展示。

---

## 1. 主题包规范

### 1.1 目录结构

```
themes/current/            ← 放在应用外部文件目录 Android/data/top.funcun.companion.app/files/
├── index.html             ← 必填，主题入口
├── style.css              ← 可选
├── app.js                 ← 可选
└── 其它资源（图片/字体等）  ← 可选
```

### 1.2 加载优先级

| 优先级 | 位置 | 说明 |
|--------|------|------|
| 1 | `Android/data/.../files/themes/current/` | 用户自定义主题（通过虚拟域名 `https://theme.local/` 加载，支持相对资源引用） |
| 2 | APK 内置 `assets/theme/` | 默认主题 |

> 自定义主题放到外部目录即自动生效（优先于内置），无需改代码。

### 1.3 文件访问

- 自定义主题：通过 `https://theme.local/index.html` 加载，CSS/JS/图片用**相对路径**引用，由宿主拦截转换。
- 内置主题：通过 `file:///android_asset/theme/index.html` 加载。
- 建议始终写相对路径，两种加载方式都兼容。

---

## 2. Bridge API（window.TianyiHost）

宿主注入全局对象 `window.TianyiHost`。所有方法**返回 JSON 字符串**，主题用 `JSON.parse` 解析。

### 2.1 插件列表

```js
const plugins = JSON.parse(TianyiHost.getPlugins()).plugins;
// [{id, name, description, version, enabled, builtin, icon, hasConfig, actions}]
```

- `builtin: true` 表示内置核心插件（不可卸载，应隐藏卸载按钮）
- `actions: [{id, label, icon, destructive}]` 插件声明的额外按钮

### 2.2 导航项（底栏）

```js
const items = JSON.parse(TianyiHost.getNavItems()).items;
// [{id, label, icon, order, pluginId}]
```

- `pluginId` 为空 = 内置导航项（专注/插件/设置），主题自行渲染对应页面
- `pluginId` 非空 = 插件贡献的导航页，切换时用 `requestNavData` 拉数据

### 2.3 导航页数据

```js
const data = JSON.parse(TianyiHost.requestNavData(pluginId, navId));
// 结构由插件决定（如统计插件返回 {todayMinutes, weekMinutes, streakDays, daily:[...]})
```

### 2.4 插件配置

```js
// 获取配置 schema（决定渲染哪些控件）
const schema = JSON.parse(TianyiHost.getConfigSchema(pluginId));
// {sections:[{title, fields:[{key,label,type,defaultValue,value,description,options,min,max}]}], customHtml}

// 读写配置值
const val = TianyiHost.readConfig(pluginId, key, defaultVal);
TianyiHost.writeConfig(pluginId, key, "newValue");
```

字段类型（`type`）：

| type | 控件建议 | 说明 |
|------|---------|------|
| `BOOLEAN` | 开关 | 值 `"true"` / `"false"` |
| `TEXT` | 单行输入 | 任意字符串 |
| `INT` | 数字输入 | 支持 `min` / `max` |
| `SELECT` | 单选下拉 | 用 `options[{value,label}]` |
| `MULTI_SELECT` | 多选 | 值用逗号分隔 |

> 若 schema 含 `customHtml`（插件指定了自定义配置界面 HTML），主题应加载渲染该 HTML 接管配置页。

### 2.5 插件动作 & 卸载

```js
// 触发插件声明的动作按钮
const result = TianyiHost.invokeAction(pluginId, actionId); // null 或插件回执 JSON

// 卸载插件（返回 false 表示是内置插件或失败）
const ok = TianyiHost.uninstallPlugin(pluginId);
```

### 2.6 应用信息 / 主题信息

```js
const info = JSON.parse(TianyiHost.getAppInfo());
// {appName, versionName, versionCode}

const theme = JSON.parse(TianyiHost.getThemeInfo());
// {installed:true/false, source:"user"|"builtin", dir:"/storage/..."}
```

### 2.7 主题包管理（导入 / 恢复）

```js
TianyiHost.importTheme();   // 打开系统文件选择器选择 zip，返回是否已触发
TianyiHost.resetTheme();    // 删除用户主题，恢复内置默认主题
```

导入完成后宿主会在主界面 `onResume` 时检测主题目录指纹变化并自动重载。

### 2.8 插件自定义配置界面

若插件的 schema 含 `customHtml`，主题应用以下方式渲染：

```js
const schema = JSON.parse(TianyiHost.getConfigSchema(pluginId));
if (schema.customHtml) {
  const html = TianyiHost.getCustomConfigHtml(pluginId); // 宿主读取插件 assets 内容
  // 提取 <style> + <body> 内容内联到主题 DOM（保持同源，片段内可用 TianyiHost）
  container.innerHTML = extractFragment(html);
  runInlineScripts(container); // innerHTML 插入的 <script> 不会自动执行
}
```

> **重要**：插件自定义配置 HTML 必须是「片段式」——只包含 `<style>` 与 body 内容，
> 不要写完整的 `<html>/<head>`。不能用 iframe srcdoc 加载（跨源拿不到 `TianyiHost`）。

### 2.9 返回键约定

宿主按返回键时会调用主题的 `window.onTianyiBackPressed()`：

```js
window.onTianyiBackPressed = function () {
  if (configPanelOpen) { closeConfig(); return true; }  // 已消费
  if (currentTab !== 'home') { navigate(0); return true; }
  return false;  // 未消费 → 宿主退出应用
};
```

### 2.10 调试日志

```js
TianyiHost.log('主题调试信息'); // 输出到 logcat（tag: ThemeJS）
```

---

## 3. 最小主题示例

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { font-family: sans-serif; padding: 16px 16px 80px; background: #eef3fa; }
  .card { background:#fff; border-radius:20px; padding:16px; margin-bottom:12px; }
  nav { position:fixed; bottom:0; left:0; right:0; display:flex; background:#fff; }
  nav button { flex:1; padding:12px; border:none; background:none; }
</style>
</head>
<body>
  <div id="content"></div>
  <nav id="nav"></nav>
  <script>
    const items = JSON.parse(TianyiHost.getNavItems()).items;
    const nav = document.getElementById('nav');
    items.forEach((item, i) => {
      const b = document.createElement('button');
      b.textContent = item.label;
      b.onclick = () => show(item);
      nav.appendChild(b);
    });

    function show(item) {
      const c = document.getElementById('content');
      if (item.pluginId) {
        const data = TianyiHost.requestNavData(item.pluginId, item.id);
        c.innerHTML = '<div class="card"><pre>' + (data || '无数据') + '</pre></div>';
      } else if (item.id === 'plugins') {
        const ps = JSON.parse(TianyiHost.getPlugins()).plugins;
        c.innerHTML = ps.map(p =>
          '<div class="card"><b>' + p.name + '</b> v' + p.version +
          (p.hasConfig ? ' <button data-cfg="' + p.id + '">配置</button>' : '') +
          (p.builtin ? '' : ' <button data-del="' + p.id + '">卸载</button>') +
          '</div>'
        ).join('');
      } else {
        c.innerHTML = '<div class="card">' + item.label + ' 页面</div>';
      }
    }

    window.config = function (id) {
      const schema = JSON.parse(TianyiHost.getConfigSchema(id));
      alert(JSON.stringify(schema, null, 2));
    };
    window.del = function (id) {
      if (confirm('卸载?')) alert(TianyiHost.uninstallPlugin(id));
    };

    // 事件委托：避免在 HTML 里拼接 onclick
    document.addEventListener('click', e => {
      const cfg = e.target.dataset && e.target.dataset.cfg;
      const del = e.target.dataset && e.target.dataset.del;
      if (cfg) window.config(cfg);
      if (del) window.del(del);
    });

    show(items[0]);
  </script>
</body>
</html>
```

---

## 4. 插件如何声明配置/动作/导航

插件作者在 `Plugin` 接口中声明（见 SDK）：

```kotlin
class MyPlugin : Plugin {
    // 内置插件不可卸载（主题隐藏卸载按钮）
    override val builtin = false
    override val iconEmoji = "🌟"

    // 配置项（主题据此渲染表单）
    override val configSchema = ConfigSchema(
        sections = listOf(ConfigSection("常规", fields = listOf(
            ConfigField("key", "显示名", ConfigFieldType.BOOLEAN, defaultValue = "true"),
        ))),
        // 或指定自定义配置界面（片段式 HTML，放在插件 assets 根目录）
        // customHtml = "config.html",
    )

    // 额外动作按钮（显示在插件卡片上）
    override val actions = listOf(PluginAction("export", "导出数据", "📤"))

    // 贡献导航项（出现在底栏）
    override val navItems = listOf(NavItem("myPage", "我的页", "🌟", order = 30))

    // 动作/导航数据处理（非 suspend：在 JS 线程同步调用，必须快速返回）
    override fun onAction(actionId: String): String? = """{"ok":true,"message":"已导出"}"""
    override fun getNavData(navId: String): String? = """{"count":42}"""
}
```

> `onAction` 返回的 JSON 若含 `message` 字段，默认主题会用 Toast 展示。

---

## 5. 安全与限制

- 统一通过虚拟域名 `https://theme.local/` 加载：宿主拦截请求，优先读用户主题目录，缺失回退内置 assets。内置与自定义主题行为完全一致。
- WebView 已加固：禁用 `file://` 直接访问、禁止混合内容、路径穿越校验。
- 主题内引用外部网络 URL 会被拦截（离线主题）。
- 主题仅能通过 `TianyiHost` 与宿主交互，不能直接访问文件系统或 Android API。
- 卸载/配置等操作宿主层做了权限校验（`builtin` 插件不可卸载）。
- 建议主题遵循系统深色模式：`@media (prefers-color-scheme: dark)`。
- `alert()` / `confirm()` 已由宿主 `WebChromeClient` 承接，但建议主题自绘对话框以获得一致视觉。

---

## 6. 内置默认主题结构（参考实现）

```
assets/theme/
├── index.html   ← 入口：<main> 页面容器 + <nav> 底栏 + app.js
├── style.css    ← 全部样式（卡片/按钮/表单/底栏/对话框/Toast，支持深色模式）
└── app.js       ← 动态渲染：底栏、首页、插件页(配置 toggle/动作/卸载)、
                    设置页、插件导航页(权限/统计)、返回键处理
```

默认主题实现要点，可作为第三方主题参考：

| 能力 | 实现方式 |
|------|---------|
| 配置面板展开/收起 | 记录 `openConfigPluginId`，同一按钮再点即收起（CSS max-height 动画） |
| 自定义配置界面 | `getCustomConfigHtml` → 提取 `<style>`+body → 内联 → 手动执行 `<script>` |
| 卸载确认 | 自绘 `.dialog-mask` 对话框（Promise），不依赖 `window.confirm` |
| Tab 保持 | `sessionStorage` 记住当前导航 id，重载后恢复 |
| 返回键 | `window.onTianyiBackPressed()` 依次尝试：收起配置 → 回首页 → 交还宿主 |
| 插件数据渲染 | 按 `data.type`（如 `permissions`）或字段特征（如 `daily`）选择渲染器，未知结构走通用键值表 |

---

## 7. 主题包打包与导入

1. 把 `index.html`（必需）与其它资源放在同一目录下
2. 打包为 zip（**index.html 需在 zip 根目录或单层文件夹内**，宿主会自动去掉单层顶层目录）
3. 应用内：插件页 → 「主题（HTML 主界面）」→ **配置** → **导入主题包** → 选择 zip
4. 导入完成后返回主界面即自动生效（宿主检测主题目录指纹变化触发重载）
5. 需要回退时在同一配置页点 **恢复内置默认主题**