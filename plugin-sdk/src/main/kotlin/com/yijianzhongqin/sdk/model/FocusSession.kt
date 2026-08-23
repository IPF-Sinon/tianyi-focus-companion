package com.yijianzhongqin.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class FocusSession(
    val id: Long = 0,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long? = null,
    val targetMinutes: Int,
    val actualMinutes: Int? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val affectionStart: Int = 70,
    val affectionEnd: Int? = null,
    val patrolResults: List<PatrolResult> = emptyList(),
)

@Serializable
enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    ABORTED
}
