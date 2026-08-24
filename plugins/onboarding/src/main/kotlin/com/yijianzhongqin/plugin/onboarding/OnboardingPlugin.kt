package com.yijianzhongqin.plugin.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yijianzhongqin.sdk.Plugin
import com.yijianzhongqin.sdk.PluginContext
import com.yijianzhongqin.sdk.event.AppEvent
import com.yijianzhongqin.sdk.event.ActionType
import com.yijianzhongqin.sdk.slot.UISlot
import com.yijianzhongqin.sdk.util.PluginId
import com.yijianzhongqin.sdk.util.SemVer

/**
 * 开屏权限引导插件。
 * 第一屏集中索要所有权限，授权完才进入主界面。
 */
class OnboardingPlugin : Plugin {

    override val id = PluginId("com.yijianzhongqin.plugin.onboarding")
    override val name = "开屏权限引导"
    override val version = SemVer(1, 0, 0)
    override val description = "首次启动时集中引导用户授予权限"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = listOf(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.PACKAGE_USAGE_STATS,
        Manifest.permission.CAMERA,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    )

    private lateinit var ctx: PluginContext

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        Log.i(TAG, "OnboardingPlugin loaded")
    }

    override suspend fun onEnable() {
        ctx.registerUI(UISlot.HOME_TOP) {
            PermissionScreen(
                hostContext = ctx.getHostContext(),
                onAllGranted = {
                    ctx.eventBus.emit(AppEvent.CompanionAction(ActionType.SMILE))
                },
            )
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "OnboardingPlugin"
    }
}

private val BackgroundColor = Color(0xFFE8A0BF)
private val SuccessColor = Color(0xFF6BBF6B)
private val WarningColor = Color(0xFFFF6B6B)

@Composable
fun PermissionScreen(
    hostContext: Context,
    onAllGranted: () -> Unit,
) {
    var showPermissionPage by remember { mutableStateOf(true) }

    val permissionsStatus = remember {
        mutableStateMapOf(
            "通知" to false,
            "悬浮窗" to false,
            "使用情况访问" to false,
            "无障碍服务" to false,
            "摄像头" to false,
            "忽略电池优化" to false,
        )
    }

    LaunchedEffect(Unit) {
        permissionsStatus["通知"] = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(hostContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

        permissionsStatus["悬浮窗"] = Settings.canDrawOverlays(hostContext)

        permissionsStatus["使用情况访问"] = if (Build.VERSION.SDK_INT >= 21) {
            try {
                val usm = hostContext.getSystemService(android.app.usage.UsageStatsManager::class.java)
                usm != null && usm.queryUsageStats(0, 0, 0).isNotEmpty()
            } catch (_: Exception) { false }
        } else true

        permissionsStatus["无障碍服务"] = false

        permissionsStatus["摄像头"] = ContextCompat.checkSelfPermission(
            hostContext, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        permissionsStatus["忽略电池优化"] = if (Build.VERSION.SDK_INT >= 23) {
            val pm = hostContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(hostContext.packageName)
        } else true
    }

    if (!showPermissionPage) return

    val allGranted = permissionsStatus.values.all { it }

    LaunchedEffect(allGranted) {
        if (allGranted) {
            onAllGranted()
            showPermissionPage = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "在开始之前，请允许我\n好好监督你～",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            permissionsStatus.forEach { (name, granted) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = name, color = Color.White, fontSize = 16.sp)
                    Text(
                        text = if (granted) "已授予" else "未授予",
                        color = if (granted) SuccessColor else WarningColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", hostContext.packageName, null)
                }
                hostContext.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = BackgroundColor,
            ),
        ) {
            Text("去授予权限", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            showPermissionPage = false
            onAllGranted()
        }) {
            Text(
                "先跳过，天依会难过的",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
        }
    }
}
