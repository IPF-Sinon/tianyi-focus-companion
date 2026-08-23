package com.yijianzhongqin.plugin.enforce

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yijianzhongqin.sdk.Plugin
import com.yijianzhongqin.sdk.PluginContext
import com.yijianzhongqin.sdk.event.AppEvent
import com.yijianzhongqin.sdk.slot.UISlot
import com.yijianzhongqin.sdk.util.PluginId
import com.yijianzhongqin.sdk.util.SemVer

/**
 * 强制拦截插件。
 * 通过 AccessibilityService 检测前台 App 切换，
 * 如果用户打开黑名单 App，则浮窗警告并尝试返回。
 */
class EnforceBlockPlugin : Plugin {

    override val id = PluginId("com.yijianzhongqin.plugin.enforce.block")
    override val name = "强制拦截"
    override val version = SemVer(1, 0, 0)
    override val description = "检测并拦截黑名单 App"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        Log.i(TAG, "EnforceBlockPlugin loaded")
    }

    override suspend fun onEnable() {
        // 注册浮窗警告 UI
        ctx.registerUI(UISlot.OVERLAY_WARNING) {
            BlockWarning()
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "EnforceBlockPlugin"
        val BLACKLIST = listOf(
            "com.ss.android.ugc.aweme",      // 抖音
            "com.tencent.tmgp.sgame",         // 王者荣耀
            "com.tencent.tmgp.pubgmhd",       // 和平精英
            "com.zhihu.android",              // 知乎
            "com.taobao.taobao",              // 淘宝
            "com.taobao.tmall",               // 天猫
            "com.jingdong.app.mall",          // 京东
            "com.netease.cloudmusic",         // 网易云音乐
            "com.tencent.qqmusic",            // QQ 音乐
            "com.tencent.qqlive",             // 腾讯视频
            "com.youku.phone",                // 优酷
            "com.qiyi.video",                 // 爱奇艺
        )
    }
}

/**
 * 无障碍服务。
 * 需要在 AndroidManifest.xml 中声明。
 */
class FocusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.i(TAG, "AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val appName = event.text?.firstOrNull()?.toString() ?: packageName

        // 检查是否在黑名单中
        if (packageName in EnforceBlockPlugin.BLACKLIST) {
            Log.i(TAG, "Blocked app detected: $appName ($packageName)")

            // 发送拦截事件
            // 注意：这里不能直接访问 EventBus，需要通过 Binder 或其他方式
            // 目前简化处理，实际项目会通过 Binder 与宿主通信

            // 尝试返回
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "AccessibilityService interrupted")
    }

    companion object {
        private const val TAG = "FocusAccessibilityService"
    }
}

/**
 * 浮窗警告 UI。
 */
@Composable
fun BlockWarning() {
    var show by remember { mutableStateOf(false) }
    var currentApp by remember { mutableStateOf("") }

    // 实际项目中会通过事件总线控制显示
    LaunchedEffect(show) {
        if (show) {
            kotlinx.coroutines.delay(3000)
            show = false
        }
    }

    if (show) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "⚠️",
                fontSize = 48.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "天依发现你打开了不合时宜的 App",
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentApp,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { show = false },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8A0BF),
                ),
            ) {
                Text("我知道了", color = Color.White)
            }
        }
    }
}
