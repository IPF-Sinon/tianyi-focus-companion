package com.yijianzhongqin.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class AffectionState(
    val currentValue: Int = 70,
    val tier: AffectionTier = AffectionTier.FRIENDLY,
    val history: List<AffectionChange> = emptyList(),
)

@Serializable
data class AffectionChange(
    val timestampEpochMs: Long,
    val delta: Int,
    val reason: String,
    val resultingValue: Int,
)

@Serializable
enum class AffectionTier(val minValue: Int, val label: String) {
    INTIMATE(90, "亲密"),
    FRIENDLY(70, "友好"),
    NEUTRAL(50, "普通"),
    COLD(30, "冷淡"),
    HEARTBROKEN(0, "心碎");

    companion object {
        fun fromValue(value: Int): AffectionTier {
            return entries.firstOrNull { value >= it.minValue } ?: HEARTBROKEN
        }
    }
}
