package com.yijianzhongqin.plugin.honor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yijianzhongqin.sdk.Plugin
import com.yijianzhongqin.sdk.PluginContext
import com.yijianzhongqin.sdk.event.AppEvent
import com.yijianzhongqin.sdk.model.BadgeLevel
import com.yijianzhongqin.sdk.model.HonorProfile
import com.yijianzhongqin.sdk.slot.UISlot
import com.yijianzhongqin.sdk.util.PluginId
import com.yijianzhongqin.sdk.util.SemVer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HonorPlugin : Plugin {

    override val id = PluginId("com.yijianzhongqin.plugin.honor")
    override val name = "荣誉系统"
    override val version = SemVer(1, 0, 0)
    override val description = "8 级徽章与连续天数"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private lateinit var honorEngine: HonorEngine

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        honorEngine = HonorEngine(context.eventBus)
        Log.i(TAG, "HonorPlugin loaded")
    }

    override suspend fun onEnable() {
        ctx.eventBus.subscribe<AppEvent.FocusCompleted> { event ->
            honorEngine.addMinutes(event.totalMinutes)
        }

        // 注册荣誉卡片到首页
        ctx.registerUI(UISlot.HOME_CARD) {
            HonorCard(honorEngine.profile)
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "HonorPlugin"
    }
}

class HonorEngine(private val eventBus: com.yijianzhongqin.sdk.event.EventBus) {
    private val _profile = MutableStateFlow(HonorProfile())
    val profile: StateFlow<HonorProfile> = _profile

    fun addMinutes(minutes: Int) {
        val old = _profile.value
        val newTotal = old.totalMinutes + minutes
        val newBadge = BadgeLevel.fromHours((newTotal / 60).toInt())

        _profile.value = old.copy(
            totalMinutes = newTotal,
            completedCount = old.completedCount + 1,
            badgeLevel = newBadge,
        )

        if (newBadge != old.badgeLevel) {
            eventBus.emit(AppEvent.MilestoneReached(newBadge))
        }
    }
}

@Composable
fun HonorCard(profile: StateFlow<HonorProfile>) {
    val p by profile.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5D0E0)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = p.badgeLevel.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC47A9A),
                    )
                    Text(
                        text = "累计 ${p.totalMinutes} 分钟",
                        fontSize = 12.sp,
                        color = Color(0xFF8A8A8A),
                    )
                }
                Text(
                    text = "已完成 ${p.completedCount} 次",
                    fontSize = 12.sp,
                    color = Color(0xFF8A8A8A),
                )
            }
        }
    }
}
