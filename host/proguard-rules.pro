# 插件 SDK 相关混淆规则
-keep class top.funcun.companion.sdk.** { *; }
-keep class top.funcun.companion.plugin.** { *; }

# Plugin 实现通过反射加载，保留构造器
-keepclasseswithmembers class * implements top.funcun.companion.sdk.Plugin {
    public <init>();
}
