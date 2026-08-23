package com.yijianzhongqin.plugin.affection

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yijianzhongqin.sdk.Plugin
import com.yijianzhongqin.sdk.PluginContext
import com.yijianzhongqin.sdk.event.AppEvent
import com.yijianzhongqin.sdk.model.AffectionState
import com.yijianzhongqin.sdk.model.AffectionTier
import com.yijianzhongqin.sdk.slot.UISlot
import com.yijianzhongqin.sdk.util.PluginId
import com.yijianzhongqin.sdk.util.SemVer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 好感度系统插件。
 * 管理好感度的增减、等级、历史记录。
 */
class AffectionPlugin : Plugin {

    override val id = PluginId("com.yijianzhongqin.plugin.affection")
    override val name = "好感度系统"
    override val version = SemVer(1, 0, 0)
    override val description = "管理天依对用户的好感度"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private lateinit var affectionEngine: AffectionEngine

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        affectionEngine = AffectionEngine(context.eventBus)
        Log.i(TAG, "AffectionPlugin loaded")
    }

    override suspend fun onEnable() {
        // 订阅专注完成事件
        ctx.eventBus.subscribe<AppEvent.FocusCompleted> { event ->
            affectionEngine.add(3, "完成 ${event.totalMinutes} 分钟专注")
        }

        // 订阅巡查结果事件
        ctx.eventBus.subscribe<AppEvent.PatrolResulted> { event ->
            affectionEngine.add(event.result.affectionDelta, event.result.aiDescription)
        }

        // 订阅拦截事件
        ctx.eventBus.subscribe<AppEvent.AppBlocked> { event ->
            affectionEngine.add(-10, "打开 ${event.appName}")
        }

        // 注册好感度 UI 到专注页底部
        ctx.registerUI(UISlot.FOCUS_AFFECTION_BAR) {
            AffectionBar(
                stateFlow = affectionEngine.state,
            )
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "AffectionPlugin"
    }
}

/**
 * 好感度引擎。
 */
class AffectionEngine(private val eventBus: com.yijianzhongqin.sdk.event.EventBus) {

    private val _state = MutableStateFlow(
        AffectionState(currentValue = 70, tier = AffectionTier.FRIENDLY)
    )
    val state: StateFlow<AffectionState> = _state

    fun add(delta: Int, reason: String) {
        val old = _state.value
        val newValue = (old.currentValue + delta).coerceIn(0, 100)
        val newTier = AffectionTier.fromValue(newValue)

        _state.value = old.copy(
            currentValue = newValue,
            tier = newTier,
            history = old.history + com.yijianzhongqin.sdk.model.AffectionChange(
                timestampEpochMs = System.currentTimeMillis(),
                delta = delta,
                reason = reason,
                resultingValue = newValue,
            ),
        )

        eventBus.emit(AppEvent.AffectionChanged(
            oldValue = old.currentValue,
            newValue = newValue,
            reason = reason,
        ))

        if (newTier != old.tier) {
            eventBus.emit(AppEvent.AffectionTierChanged(
                oldTier = old.tier,
                newTier = newTier,
            ))
        }

        Log.i(TAG, "好感度: ${old.currentValue} → $newValue ($reason)")
    }

    companion object {
        private const val TAG = "AffectionEngine"
    }
}

private val AffectionIntimate = Color(0xFFFF6B9D)
private val AffectionFriendly = Color(0xFFFF8FA3)
private val AffectionNeutral = Color(0xFFB0B0B0)
private val AffectionCold = Color(0xFF6E8BB8)
private val AffectionHeartbroken = Color(0xFF4A4A4A)

@Composable
fun AffectionBar(stateFlow: StateFlow<AffectionState>) {
    val state by stateFlow.collectAsState()

    val barColor = when (state.tier) {
        AffectionTier.INTIMATE -> AffectionIntimate
        AffectionTier.FRIENDLY -> AffectionFriendly
        AffectionTier.NEUTRAL -> AffectionNeutral
        AffectionTier.COLD -> AffectionCold
        AffectionTier.HEARTBROKEN -> AffectionHeartbroken
    }

    val tierIcon = when (state.tier) {
        AffectionTier.INTIMATE -> "💕"
        AffectionTier.FRIENDLY -> "😊"
        AffectionTier.NEUTRAL -> "😐"
        AffectionTier.COLD -> "😔"
        AffectionTier.HEARTBROKEN -> "💔"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$tierIcon 好感度",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
            Text(
                text = "${state.currentValue}/100",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.2f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.currentValue / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }
    }
}
