# 主题包制作指南（Theme Pack Guide）

依见钟勤采用**原生 Compose 界面 + 配置式主题**（对标 FolkPatch 生态）。
主题包是一个 zip 文件（或 FolkPatch 的 `.fpt` 加密包），包含 `theme.json` 配置 + 可选资源文件。

**你只需要改 JSON + 换图片，不需要写代码。**

---

## 1. 主题包结构

```
my-theme.zip
├── theme.json          ← 必需：主题配置
├── background.jpg      ← 可选：全局背景图
├── font.ttf            ← 可选：自定义字体
└── preview.png         ← 可选：预览图
```

> 若从 FolkPatch 主题商店下载的是 `.fpt` 文件，应用能**直接解密应用**，无需手动解压。

---

## 2. theme.json 字段说明

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `metaName` | string | 默认主题 | 主题名 |
| `metaAuthor` | string | "" | 作者 |
| `metaVersion` | string | "1.0" | 版本 |
| `metaDescription` | string | "" | 描述 |
| `customColor` | string | "#4A90E2" | 种子色（十六进制） |
| `useSystemDynamicColor` | bool | false | 使用系统 Material You 动态取色 |
| `colorStyle` | string | "TONAL_SPOT" | 取色风格：TONAL_SPOT / VIBRANT / EXPRESSIVE / NEUTRAL / FIDELITY / CONTENT / RAINBOW / FRUIT_SALAD / MONOCHROME |
| `nightModeEnabled` | bool | false | 深色模式（`nightModeFollowSys=false` 时生效） |
| `nightModeFollowSys` | bool | true | 深色模式跟随系统 |
| `isBackgroundEnabled` | bool | false | 启用背景图 |
| `backgroundOpacity` | float | 1.0 | 背景不透明度 |
| `backgroundBlur` | float | 0 | 背景模糊度 |
| `backgroundDim` | float | 0.2 | 背景暗化 |
| `isDualBackgroundDimEnabled` | bool | false | 日夜分别暗化 |
| `backgroundDayDim` | float | 0.1 | 白天暗化 |
| `backgroundNightDim` | float | 0.4 | 夜间暗化 |
| `isFontEnabled` | bool | false | 启用自定义字体 |
| `homeLayoutStyle` | string | "dashboard" | 首页布局：dashboard / simple |
| `cardCornerRadius` | int | 24 | 卡片圆角（dp） |

> 字段名与 FolkPatch 的 theme.json **兼容**：FolkPatch 主题包里没有的字段用默认值，
> 我们专有的字段（如 `cardCornerRadius`）FolkPatch 里没有也不影响。

---

## 3. 最小示例

`theme.json`：

```json
{
  "metaName": "天依蓝",
  "metaAuthor": "你",
  "metaVersion": "1.0",
  "customColor": "#4A90E2",
  "colorStyle": "TONAL_SPOT",
  "nightModeFollowSys": true,
  "cardCornerRadius": 24
}
```

把 `theme.json` 单独打包成 zip（或直接选择这个 json 文件？不行，必须是 zip）。
用文件管理器把这个 json 和（可选）图片打成 zip。

---

## 4. 如何应用主题包

应用内两条路径：

1. **设置 → 外观主题 → 导入主题包**：选择 `.zip` / `.fpt` 文件
2. **设置 → 主题商店**：在线浏览 FolkPatch 社区主题，点「应用」一键下载并应用

---

## 5. 如何制作 FolkPatch 兼容的 .fpt

FolkPatch 主题包 = AES/CBC 加密的 zip（前 16 字节 IV，密钥 SHA-256("FolkPatchThemeSecretKey2025")）。

- 想给 FolkPatch 用：用 FolkPatch 自己的导出功能
- 想给本应用用：直接用**明文 zip** 即可，应用两种都支持

---

## 6. 常见问题

- **导入报错「不是有效的主题包」**：确认是 zip（含 theme.json），不是单独图片
- **配色没变**：确认 `customColor` 是 `#RRGGBB` 格式；若开了 `useSystemDynamicColor` 会优先用系统取色
- **第三方 FolkPatch 主题的「布局风格」字段对不上**：无法识别时回退默认布局，不影响配色/背景
- **背景图不显示**：确认文件名是 `background.jpg` 且在 zip 根目录（或单层文件夹内）