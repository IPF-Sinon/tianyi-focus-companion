package top.funcun.companion.sdk.event

import top.funcun.companion.sdk.model.AffectionTier
import top.funcun.companion.sdk.model.BadgeLevel
import top.funcun.companion.sdk.model.Emotion
import top.funcun.companion.sdk.model.PatrolResult
import top.funcun.companion.sdk.model.PatrolSeverity
import top.funcun.companion.sdk.model.PatrolTrigger
import top.funcun.companion.sdk.model.RenderMode

/**
 * 全局应用事件。
 * 所有插件间通信都通过 [EventBus] 发送这些事件。
 */
sealed interface AppEvent {

    // ── 专注生命周期 ──

    data class FocusStarted(
        val targetMinutes: Int,
        val startTimeEpochMs: Long = System.currentTimeMillis(),
    ) : AppEvent

    data class FocusTick(
        val elapsedMinutes: Int,
        val remainingMinutes: Int,
    ) : AppEvent

    data class FocusCompleted(
        val totalMinutes: Int,
        val affectionDelta: Int = 3,
    ) : AppEvent

    data class FocusPaused(
        val reason: PauseReason,
    ) : AppEvent

    object FocusResumed : AppEvent

    data class FocusAborted(
        val reason: AbortReason,
    ) : AppEvent

    // ── 巡查 ──

    data class PatrolTriggered(
        val trigger: PatrolTrigger,
    ) : AppEvent

    data class PatrolResulted(
        val result: PatrolResult,
    ) : AppEvent

    // ── 好感度 ──

    data class AffectionChanged(
        val oldValue: Int,
        val newValue: Int,
        val reason: String,
    ) : AppEvent

    data class AffectionTierChanged(
        val oldTier: AffectionTier,
        val newTier: AffectionTier,
    ) : AppEvent

    // ── 荣誉里程碑 ──

    data class MilestoneReached(
        val badge: BadgeLevel,
    ) : AppEvent

    data class StreakUpdated(
        val days: Int,
    ) : AppEvent

    // ── 伴侣 ──

    data class CompanionSpeech(
        val text: String,
        val emotion: Emotion,
        val audioUrl: String? = null,
    ) : AppEvent

    data class CompanionAction(
        val type: ActionType,
    ) : AppEvent

    // ── 强制拦截 ──

    data class AppBlocked(
        val packageName: String,
        val appName: String,
    ) : AppEvent

    // ── 渲染模式切换 ──

    data class RenderModeChanged(
        val mode: RenderMode,
    ) : AppEvent
}

enum class PauseReason {
    USER, INCOMING_CALL, NOTIFICATION
}

enum class AbortReason {
    MANUAL, HEARTBROKEN, SYSTEM
}

enum class ActionType {
    SMILE, WAVE, LOOK_AWAY, SIGH, HAPPY_DANCE, SHAKE_HEAD, BLUSH
}
