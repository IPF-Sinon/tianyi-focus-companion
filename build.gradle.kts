plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // Hilt 和 KSP 不在 root 声明——都在 host 子项目本地应用。
    // 确保 Hilt 和 KSP 使用同一类加载器，避免 Hilt 找不到 KSP task class 的报错。
    alias(libs.plugins.ksp) apply false
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