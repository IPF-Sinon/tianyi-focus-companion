package top.funcun.companion.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class PatrolResult(
    val timestampEpochMs: Long,
    val trigger: PatrolTrigger,
    val imageUrl: String? = null,
    val aiDescription: String = "",
    val studying: Boolean = true,
    val activity: String = "",
    val severity: PatrolSeverity = PatrolSeverity.NONE,
    val affectionDelta: Int = 0,
    val companionResponse: String = "",
    val companionAudioUrl: String? = null,
)

@Serializable
enum class PatrolTrigger {
    MOTION,
    APP_SWITCH,
    TIMER
}

@Serializable
enum class PatrolSeverity {
    NONE,
    MILD,
    MODERATE,
    SEVERE
}
