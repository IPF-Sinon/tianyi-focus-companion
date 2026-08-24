package top.funcun.companion.sdk.model

import kotlinx.serialization.Serializable

/** 天依情绪 */
@Serializable
enum class Emotion {
    IDLE,
    ENCOURAGING,
    WORRIED,
    STERN,
    CELEBRATING,
    DISAPPOINTED,
    PLAYFUL
}
