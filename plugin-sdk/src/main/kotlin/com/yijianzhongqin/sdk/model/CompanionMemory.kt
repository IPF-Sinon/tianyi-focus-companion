package com.yijianzhongqin.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class CompanionMemory(
    val id: Long = 0,
    val fact: String,
    val category: MemoryCategory,
    val confidence: Float = 1.0f,
    val createdAtEpochMs: Long,
    val lastTriggeredAtEpochMs: Long? = null,
)

@Serializable
enum class MemoryCategory {
    HABIT,
    MILESTONE,
    INCIDENT,
    PREFERENCE
}
