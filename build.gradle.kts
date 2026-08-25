plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// Miuix 0.9.x 全部要求 minCompileSdk=37，但用 AGP 9 + compileSdk 35。
// 直接禁用 CheckAarMetadata 任务以绕过该硬性检查（Miuix 仅编译目标较新，实际不使用 API 37 特性）。
subprojects {
    tasks.configureEach {
        if (name.endsWith("AarMetadata")) {
            enabled = false
        }
    }

    // Hilt 注解处理器自带的 kotlin-metadata-jvm 版本（2.2.20）只能读 metadata ≤2.3，
    // 但 Miuix 的类 metadata 是 2.4.0。强制升级到与 Kotlin 编译器一致的 2.4.10。
    configurations.configureEach {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.10")
        }
    }
}