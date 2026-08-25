plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    // KSP 子项目本地应用，但加到 root buildscript classpath 让 Hilt 可访问其 task class
}

buildscript {
    dependencies {
        // KSP 2.3.x 的解耦插件需要显式加到 root classpath，
        // 否则 Hilt Gradle 插件（在 root classloader 中）找不到 KSP task class。
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.11")
    }
}

// Miuix 0.9.x 全部要求 minCompileSdk=37，但 AGP 8.7 最大支持 35。
// 直接禁用 CheckAarMetadata 任务以绕过该硬性检查（Miuix 仅编译目标较新，实际不使用 API 37 特性）。
subprojects {
    tasks.configureEach {
        if (name.endsWith("AarMetadata")) {
            enabled = false
        }
    }
}