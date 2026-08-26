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

### 2.6 应用信息

```js
const info = JSON.parse(TianyiHost.getAppInfo());
// {appName, versionName, versionCode}
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
  body { font-family: sans-serif; padding: 16px; background: #eef3fa; }
  .card { background:#fff; border-radius:20px; padding:16px; margin-bottom:12px; }
  nav { position:fixed; bottom:0; left:0; right:0; display:flex; background:#fff; }
  nav button { flex:1; padding:10px; border:none; background:none; }
</style>
</head>
<body>
  <div id="content"></div>
  <nav id="nav"></nav>
  <script>
    const items = JSON.parse(TianyiHost.getNavItems()).items;
    const nav = document.getElementById('nav');
    items.forEach(item => {
      const b = document.createElement('button');
      b.textContent = item.label;
      b.onclick = () => show(item);
      nav.appendChild(b);
    });

    function show(item) {
      const c = document.getElementById('content');
      if (item.pluginId) {
        const data = TianyiHost.requestNavData(item.pluginId, item.id);
        c.innerHTML = '<div class="card">' + data + '</div>';
      } else if (item.id === 'plugins') {
        const ps = JSON.parse(TianyiHost.getPlugins()).plugins;
        c.innerHTML = ps.map(p =>
          '<div class="card"><b>' + p.name + '</b> v' + p.version +
          (p.hasConfig ? ' <button onclick="config(\'' + p.id + '\')">配置</button>' : '') +
          (p.builtin ? '' : ' <button onclick="del(\'' + p.id + '\')">卸载</button>') +
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
    // 配置项（主题据此渲染表单）
    override val configSchema = ConfigSchema(
        sections = listOf(ConfigSection("常规", fields = listOf(
            ConfigField("key", "显示名", ConfigFieldType.BOOLEAN, defaultValue = "true"),
        ))),
    )

    // 额外动作按钮（显示在插件卡片上）
    override val actions = listOf(PluginAction("export", "导出数据", "📤"))

    // 贡献导航项（出现在底栏）
    override val navItems = listOf(NavItem("myPage", "我的页", "🌟", order = 30))

    // 动作/导航数据处理
    override suspend fun onAction(actionId: String) = "{...}"
    override suspend fun getNavData(navId: String) = "{...}"
}
```

---

## 5. 安全与限制

- WebView 默认**不开启**网络资源加载，主题内引用外部 URL 会被拦截（离线主题）。
- 主题仅能通过 `TianyiHost` 与宿主交互，不能直接访问文件系统或 Android API。
- 卸载/配置等操作宿主层做了权限校验（内置插件不可卸载）。
- 建议主题遵循系统深色模式：`@media (prefers-color-scheme: dark)`。

---

## 6. 内置默认主题结构（参考实现）

```
assets/theme/
├── index.html   ← 入口：<main> 页面容器 + <nav> 底栏 + app.js
├── style.css    ← 全部样式（卡片/按钮/表单/底栏，支持深色模式）
└── app.js       ← 动态渲染：底栏、专注页、插件页(含配置/卸载)、统计导航数据
```