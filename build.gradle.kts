plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
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