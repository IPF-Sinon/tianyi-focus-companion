# 依见钟勤

> 遇见天依之后，对「番茄钟 + 勤奋学习」一见钟情。

一个基于插件化架构的 Android 洛天依 AI 学习伴侣应用。

## 状态

Phase 1 开发中：Gradle 多模块骨架 + plugin-sdk + Host 壳 + MIUI 风格主题

## 模块

```
host/          壳 APK（插件管理 + UI 壳 + 主题）
plugin-sdk/    插件 SDK（接口 + 事件总线 + 数据模型）
plugins/       官方插件（后续添加）
```

## 构建

需要 Android SDK 35，使用 Gradle 构建：

```bash
./gradlew :host:assembleDebug
```

## 文档

- [设计文档](DESIGN.md)

## 开源协议

Apache 2.0
