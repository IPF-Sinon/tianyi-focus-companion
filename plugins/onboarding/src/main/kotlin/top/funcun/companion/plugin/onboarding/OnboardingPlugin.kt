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
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.event.ActionType
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.app.ActivityCompat


private val BackgroundColor = Color(0xFFE8A0BF)
private val SuccessColor = Color(0xFF6BBF6B)
private val WarningColor = Color(0xFFFF6B6B)


/**
 * 开屏权限引导插件。
 * 第一屏集中索要所有权限，授权完才进入主界面。
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
                    ctx.eventBus.emit(AppEvent.CompanionAction(ActionT@Composable
fun PermissionScreen(
    hostContext: Context,
    onAllGranted: () -> Unit,
) {
    var showPermissionPage by remember { mutableStateOf(true) }

    // 通知权限（Android 13+ 需要运行时弹窗）
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTrigger++ }

    // 相机权限（运行时弹窗）
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTrigger++ }

    // 用于触发重新检测的计数器
    var refreshTrigger by remember { mutableIntStateOf(0) }

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

    // 检测权限状态
    fun checkPermissions(ctx: Context) {
        permissionsStatus["通知"] = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

        permissionsStatus["悬浮窗"] = Settings.canDrawOverlays(ctx)

        permissionsStatus["使用情况访问"] = if (Build.VERSION.SDK_INT >= 21) {
            checkUsageStatsAccess(ctx)
        } else true

        permissionsStatus["无障碍服务"] = checkAccessibilityEnabled(ctx)

        permissionsStatus["摄像头"] = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        permissionsStatus["忽略电池优化"] = if (Build.VERSION.SDK_INT >= 23) {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(ctx.packageName)
        } else true
    }

    // 初始检测 + 刷新触发
    LaunchedEffect(refreshTrigger) {
        checkPermissions(hostContext)
    }

    // 页面返回时刷新（onResume）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions(hostContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!showPermissionPage) return

    val allGranted = permissionsStatus.values.all { it }

    LaunchedEffect(allGranted) {
        if (allGranted) {
            onAllGranted()
            showPermissionPage = false
        }
    }

    // 处理权限点击
    fun handlePermissionClick(name: String) {
        try {
            when (name) {
                "通知" -> {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return
                    }
                }
                "摄像头" -> {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                    return
                }
            }
            // 其他权限走系统设置
            val intent = getPermissionIntent(hostContext, name)
            if (intent != null) {
                hostContext.startActivity(intent)
            } else {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", hostContext.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                hostContext.startActivity(fallback)
            }
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", hostContext.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            hostContext.startActivity(fallback)
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
            val permissionIcons = mapOf(
                "无障碍服务" to "♿",
                "使用情况访问" to "📊",
                "摄像头" to "📷",
                "通知" to "🔔",
                "忽略电池优化" to "🔋",
                "悬浮窗" to "🪟",
            )
            permissionsStatus.forEach { (name, granted) ->
                Button(
                    onClick = { handlePermissionClick(name) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = if (granted) 0.3f else 0.15f),
                        contentColor = Color.White,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${permissionIcons[name] ?: ""} $name",
                            fontSize = 16.sp,
                        )
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

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", hostContext.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

/**
 * 根据权限名称返回对应的系统设置 Intent。
 */
private fun getPermissionIntent(context: Context, permissionName: String): Intent? {
    val intent: Intent? = when (permissionName) {
        "无障碍服务" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        "使用情况访问" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        "摄像头" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        "通知" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else null
        "忽略电池优化" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        "悬浮窗" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        else -> null
    }
    // Application context 启动 Activity 必须加 NEW_TASK flag
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}
