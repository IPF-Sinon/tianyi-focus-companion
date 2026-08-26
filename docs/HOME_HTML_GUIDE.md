# 主界面 HTML 编写指南

依见钟勤的主界面由 **home-html 插件** 通过 WebView 渲染 HTML 实现。
用户可以通过编写/替换 HTML 文件来自定义主界面，HTML 中的特定标识（`data-tianyi-*`）会自动对接真实功能。

---

## 1. HTML 文件放在哪里？

插件按以下优先级加载主界面 HTML：

| 优先级 | 位置 | 说明 |
|--------|------|------|
| 1 | 应用外部文件目录 `Android/data/top.funcun.companion.app/files/home.html` | 用户自定义，优先级最高 |
| 2 | 插件内置 `assets/home.html` | 默认界面，随 APK 打包 |

> **自定义方法**：用文件管理器/ADB 把 `home.html` 放到上述外部目录即可覆盖默认界面。
> 修改后需重启应用（或重新进入主界面 Tab）生效。

---

## 2. 功能标识（data-tianyi-* 属性）

在 HTML 中，给任意元素加上 `data-tianyi-action` 属性，点击该元素即可触发对应功能。
插件会在页面加载后自动扫描并绑定点击事件。

### 2.1 开始专注

```html
<button data-tianyi-action="focus-start" data-tianyi-minutes="25">开始专注 25 分钟</button>
```

- `data-tianyi-action="focus-start"`：触发开始专注
- `data-tianyi-minutes="25"`（可选，默认 25）：专注时长（分钟）
- 可用时长：25 / 45 / 60 / 120

### 2.2 结束专注

```html
<button data-tianyi-action="focus-stop">结束专注</button>
```

### 2.3 完整示例

```html
<div class="card">
  <h1>我的主界面</h1>

  <button data-tianyi-action="focus-start" data-tianyi-minutes="25">🍅 专注 25 分钟</button>
  <button data-tianyi-action="focus-start" data-tianyi-minutes="45">专注 45 分钟</button>
  <button data-tianyi-action="focus-stop">结束</button>
</div>
```

---

## 3. 专注状态回调（onTianyiFocusState）

宿主会持续推送专注状态到页面，页面需定义全局函数 `window.onTianyiFocusState` 接收：

```javascript
window.onTianyiFocusState = function (state) {
  // state: "IDLE" | "ACTIVE" | "PAUSED" | "COMPLETED"
  console.log("当前专注状态:", state);
  if (state === "ACTIVE") {
    document.getElementById("stopBtn").style.display = "block";
  } else {
    document.getElementById("stopBtn").style.display = "none";
  }
};
```

> 状态推送目前仅包含阶段（IDLE/ACTIVE/PAUSED/COMPLETED），
> 剩余秒数实时推送将在后续版本加入（届时通过 `onTianyiFocusTick(minutes, seconds)` 提供）。

---

## 4. 样式建议

WebView 默认使用系统 WebKit，完整支持 CSS3。建议：

- 使用 `viewport` 声明移动端适配（已自动注入）
- 支持浅色/深色：可用 `prefers-color-scheme`

```css
@media (prefers-color-scheme: dark) {
  body { background: #111; color: #eee; }
}
```

- 点击反馈：`-webkit-tap-highlight-color: transparent` + `:active` 样式

---

## 5. 安全说明

- 页面中 **不要** 引用外部网络资源（被 WebView 默认拦截）
- `data-tianyi-action` 仅绑定已实现的真实功能，未实现的不会生效
- 不要在 HTML 中执行 `window.TianyiBridge` 之外的敏感操作

---

## 6. 已实现功能一览（当前版本）

| 标识 | 功能 | 参数 |
|------|------|------|
| `focus-start` | 开始专注 | `data-tianyi-minutes`（25/45/60/120，默认 25） |
| `focus-stop` | 结束/停止专注 | 无 |

> 后续版本将逐步开放：切换 Tab、好感度查询、荣誉徽章展示、语音对话等。

---

## 7. 调试

- WebView 内 console 日志可通过 `adb logcat | grep chromium` 查看
- HTML 语法错误不会导致崩溃，但功能标识不会生效
- 若点击无反应，先确认元素是否在 `<body>` 内且未被遮挡/禁用