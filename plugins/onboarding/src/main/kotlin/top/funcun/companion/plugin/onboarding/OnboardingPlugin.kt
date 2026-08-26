package top.funcun.companion.plugin.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.event.ActionType
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 开屏权限引导插件。
 * 第一屏集中索要所有权限，授权完才进入主界面。
 * 授权状态通过 SharedPreferences 持久化，再次进入应用不再显示。
 */
class OnboardingPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.onboarding")
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
    // ── 持久化：已完成引导的记录（避免跳过/完成后再进入仍显示） ──
    val prefs = remember { hostContext.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE) }
    var showPermissionPage by remember { mutableStateOf(prefs.getBoolean("show_page", true)) }

    // ── 权限状态 ──
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

    // ── 权限检查函数（可复用，同时用于初始化和 onResume 刷新） ──
    fun checkPermissions() {
        permissionsStatus["通知"] = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(hostContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

        permissionsStatus["悬浮窗"] = Settings.canDrawOverlays(hostContext)

        permissionsStatus["使用情况访问"] = if (Build.VERSION.SDK_INT >= 21) {
            try {
                val usm = hostContext.getSystemService(android.app.usage.UsageStatsManager::class.java)
                if (usm == null) {
                    false
                } else {
                    val now = System.currentTimeMillis()
                    // 查询最近 24 小时：有权限时返回非 null（可能为空 List），无权限时抛异常或返回 null
                    usm.queryUsageStats(
                        android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                        now - 24 * 60 * 60 * 1000,
                        now
                    )
                    true // 没抛异常就说明有权限
                }
            } catch (_: Exception) {
                false
            }
        } else true

        // 无障碍服务：只能通过系统设置手动开启，无法直接检测（AccessibilityManager 需要 BIND_ACCESSIBILITY_SERVICE 权限）
        // 始终显示为未授予，用户自行跳转设置
        permissionsStatus["无障碍服务"] = false

        permissionsStatus["摄像头"] = ContextCompat.checkSelfPermission(
            hostContext, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        permissionsStatus["忽略电池优化"] = if (Build.VERSION.SDK_INT >= 23) {
            val pm = hostContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(hostContext.packageName)
        } else true
    }

    // ── 初始加载时检查权限 ──
    LaunchedEffect(Unit) {
        checkPermissions()
    }

    // ── 每次 Activity onResume 刷新权限（用户从系统设置页返回） ──
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── 运行时权限请求 Launcher：通知 ──
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionsStatus["通知"] = granted
    }

    // ── 运行时权限请求 Launcher：相机 ──
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionsStatus["摄像头"] = granted
    }

    // ── 全部授予后自动隐藏 ──
    val allGranted = permissionsStatus.values.all { it }
    LaunchedEffect(allGranted) {
        if (allGranted) {
            prefs.edit().putBoolean("show_page", false).apply()
            onAllGranted()
            showPermissionPage = false
        }
    }

    // ── 已跳过/已完成，不渲染 ──
    if (!showPermissionPage) return

    // ── UI 布局 ──
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
                Button(
                    onClick = {
                        when (name) {
                            "通知" -> {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    // Android 13+ 运行时弹窗请求
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    // 低版本跳通知设置（虽然已预授权，但保留作为引导）
                                    val intent = getPermissionIntent(hostContext, name)
                                    if (intent != null) {
                                        try {
                                            hostContext.startActivity(intent)
                                        } catch (_: Exception) {
                                            fallbackToAppSettings(hostContext)
                                        }
                                    }
                                }
                            }
                            "摄像头" -> {
                                if (Build.VERSION.SDK_INT >= 23) {
                                    // Android 6+ 运行时弹窗请求
                                    cameraLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    val intent = getPermissionIntent(hostContext, name)
                                    if (intent != null) {
                                        try {
                                            hostContext.startActivity(intent)
                                        } catch (_: Exception) {
                                            fallbackToAppSettings(hostContext)
                                        }
                                    }
                                }
                            }
                            else -> {
                                // 其他权限跳系统设置
                                val intent = getPermissionIntent(hostContext, name)
                                if (intent != null) {
                                    try {
                                        hostContext.startActivity(intent)
                                    } catch (_: Exception) {
                                        fallbackToAppSettings(hostContext)
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = name, fontSize = 16.sp)
                        Text(
                            text = if (granted) "已授予" else "未授予 →",
                            color = if (granted) SuccessColor else WarningColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 全部权限设置页快捷入口
        Button(
            onClick = { fallbackToAppSettings(hostContext) },
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

        // 跳过按钮
        TextButton(onClick = {
            prefs.edit().putBoolean("show_page", false).apply()
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

/**
 * 降级到应用详情页
 */
private fun fallbackToAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/**
 * 根据权限名称返回对应的系统设置 Intent。
 */
private fun getPermissionIntent(context: Context, permissionName: String): Intent? {
    val intent: Intent? = when (permissionName) {
        "无障碍服务" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        "使用情况访问" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        "摄像头" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        "通知" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else null
        "忽略电池优化" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        "悬浮窗" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        else -> null
    }
    // Application context 启动 Activity 必须加 NEW_TASK flag
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}