# 插件 SDK 相关混淆规则
-keep class com.yijianzhongqin.sdk.** { *; }
-keep class com.yijianzhongqin.plugin.** { *; }

# Plugin 实现通过反射加载，保留构造器
-keepclasseswithmembers class * implements com.yijianzhongqin.sdk.Plugin {
    public <init>();
}
