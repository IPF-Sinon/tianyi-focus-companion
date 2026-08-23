package com.yijianzhongqin.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class HonorProfile(
    val totalMinutes: Long = 0,
    val streakDays: Int = 0,
    val badgeLevel: BadgeLevel = BadgeLevel.CADET,
    val completedCount: Int = 0,
)

@Serializable
enum class BadgeLevel(val label: String, val requiredHours: Int) {
    CADET("列兵", 0),
    PRIVATE("上等兵", 5),
    CORPORAL("下士", 15),
    SERGEANT("中士", 40),
    LIEUTENANT("少尉", 100),
    CAPTAIN("大尉", 250),
    MAJOR("少校", 600),
    COLONEL("上校", 1500);

    companion object {
        fun fromHours(hours: Int): BadgeLevel {
            return entries.lastOrNull { hours >= it.requiredHours } ?: CADET
        }
    }
}
