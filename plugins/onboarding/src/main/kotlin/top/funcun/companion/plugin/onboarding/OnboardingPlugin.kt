package top.funcun.companion.plugin.onboarding

import android.accessibilityservice.AccessibilityServiceInfo
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * 首次启动时全屏引导用户授予权限，授权/跳过通过 SharedPreferences 持久化。
 * 同时在设置页注册「权限管理」入口，方便跳过用户重新授权。
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
        // 全屏权限引导（首次启动，跳过/完成后不再显示）
        ctx.registerUI(UISlot.HOME_TOP) {
            PermissionScreen(
                hostContext = ctx.getHostContext(),
                onAllGranted = {
                    ctx.eventBus.emit(AppEvent.CompanionAction(ActionType.SMILE))
                },
            )
        }
        // 设置页「权限管理」入口（始终显示，方便跳过用户重新授权）
        ctx.registerUI(UISlot.SETTINGS_SECTION) {
            PermissionSettingsSection(
                hostContext = ctx.getHostContext(),
            )
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "OnboardingPlugin"
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_SHOW_PAGE = "show_page"
        internal const val ACCESSIBILITY_SERVICE_CLASS =
            "top.funcun.companion.plugin.enforce.FocusAccessibilityService"
    }
}

// ── 权限状态检查 ──────────────────────────────────────────────────

/**
 * 检查所有权限状态，写入 [status]。
 * 支持运行时权限（通知/相机）检测、系统设置权限（悬浮窗/使用情况/无障碍/电池优化）检测。
 */
private fun Context.checkPermissions(status: MutableMap<String, Boolean>) {
    status["通知"] = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else true

    status["悬浮窗"] = Settings.canDrawOverlays(this)

    status["使用情况访问"] = if (Build.VERSION.SDK_INT >= 21) {
        try {
            val usm = getSystemService(android.app.usage.UsageStatsManager::class.java)
            if (usm == null) {
                false
            } else {
                val now = System.currentTimeMillis()
                // 权限已授予时 queryUsageStats 不会抛 SecurityException（结果可能为空，
                // 但代表着「有权限且暂无使用记录」，仍然算作已授权）
                @Suppress("UNUSED_EXPRESSION")
                usm.queryUsageStats(
                    android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                    now - 24 * 60 * 60 * 1000,
                    now
                )
                true
            }
        } catch (_: Exception) {
            false
        }
    } else true

    // 无障碍服务：通过 AccessibilityManager 检测 FocusAccessibilityService 是否已启用
    status["无障碍服务"] = if (Build.VERSION.SDK_INT >= 21) {
        try {
            val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val serviceName = ComponentName(
                packageName,
                OnboardingPlugin.ACCESSIBILITY_SERVICE_CLASS
            )
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { info ->
                    val si = info.resolveInfo?.serviceInfo ?: return@any false
                    si.packageName == serviceName.packageName && si.name == serviceName.className
                }
        } catch (_: Exception) {
            false
        }
    } else false

    status["摄像头"] = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    status["忽略电池优化"] = if (Build.VERSION.SDK_INT >= 23) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(packageName)
    } else true
}

// ── 权限状态 Composable 状态持有器 ───────────────────────────────

/**
 * 权限状态 + 运行时权限 Launcher 的持有器。
 * launcher 在组合阶段创建，点击回调中使用。
 */
private class PermissionState {
    val status = mutableStateMapOf(
        "通知" to false,
        "悬浮窗" to false,
        "使用情况访问" to false,
        "无障碍服务" to false,
        "摄像头" to false,
        "忽略电池优化" to false,
    )
    var notificationLauncher: ManagedActivityResultLauncher<String, Boolean>? = null
    var cameraLauncher: ManagedActivityResultLauncher<String, Boolean>? = null
}

/**
 * 创建并持有权限状态，注册 onResume 自动刷新。
 */
@Composable
private fun rememberPermissionState(hostContext: Context): PermissionState {
    val state = remember { PermissionState() }

    state.notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        state.status["通知"] = granted
    }

    state.cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        state.status["摄像头"] = granted
    }

    // 初始检查
    LaunchedEffect(Unit) {
        hostContext.checkPermissions(state.status)
    }

    // onResume 刷新：从系统设置页返回时自动更新
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hostContext.checkPermissions(state.status)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}

// ── 权限项图标 ──────────────────────────────────────────────────

private fun permissionIcon(name: String): String = when (name) {
    "通知" -> "🔔"
    "悬浮窗" -> "🪟"
    "使用情况访问" -> "📊"
    "无障碍服务" -> "♿"
    "摄像头" -> "📷"
    "忽略电池优化" -> "🔋"
    else -> "⚙️"
}

// ── 通用权限列表组件（FolkPatch 风格卡片） ───────────────────────

/**
 * 权限列表卡片，参考 FolkPatch UI：
 * - RoundedCornerShape(24.dp) Card，surfaceContainer 底色
 * - ListItem 行：图标 + 名称 + 状态标签
 * - 点击行执行 [onClick]
 */
@Composable
private fun PermissionList(
    permissionsStatus: Map<String, Boolean>,
    onClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column {
            permissionsStatus.entries.forEachIndexed { index, (name, granted) ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(name) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = permissionIcon(name),
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (granted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    ) {
                        Text(
                            text = if (granted) "已授予" else "未授予",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (granted) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── 权限点击处理（运行时弹窗优先） ───────────────────────────────

/**
 * 权限点击回调：
 * - 通知（Android 13+）/ 相机（Android 6+）→ 运行时权限弹窗
 * - 其余跳转系统设置页
 */
private fun permissionClickHandler(
    hostContext: Context,
    state: PermissionState,
): (String) -> Unit = { name ->
    when (name) {
        "通知" -> {
            if (Build.VERSION.SDK_INT >= 33) {
                state.notificationLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                    ?: openSettings(hostContext, name)
            } else {
                openSettings(hostContext, name)
            }
        }
        "摄像头" -> {
            if (Build.VERSION.SDK_INT >= 23) {
                state.cameraLauncher?.launch(Manifest.permission.CAMERA)
                    ?: openSettings(hostContext, name)
            } else {
                openSettings(hostContext, name)
            }
        }
        else -> openSettings(hostContext, name)
    }
}

// ── 全屏引导页（HOME_TOP） ──────────────────────────────────────

/**
 * 全屏权限引导页，注册到 UISlot.HOME_TOP。
 * 跳过/全部授权后通过 SharedPreferences 持久化，不再显示。
 */
@Composable
fun PermissionScreen(
    hostContext: Context,
    onAllGranted: () -> Unit,
) {
    val prefs = remember { hostContext.getSharedPreferences(OnboardingPlugin.PREFS_NAME, Context.MODE_PRIVATE) }
    var showPermissionPage by remember { mutableStateOf(prefs.getBoolean(OnboardingPlugin.KEY_SHOW_PAGE, true)) }
    val state = rememberPermissionState(hostContext)
    val onClick = permissionClickHandler(hostContext, state)

    // 全部授予后自动隐藏
    val allGranted = state.status.values.all { it }
    LaunchedEffect(allGranted) {
        if (allGranted) {
            prefs.edit().putBoolean(OnboardingPlugin.KEY_SHOW_PAGE, false).apply()
            onAllGranted()
            showPermissionPage = false
        }
    }

    if (!showPermissionPage) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "在开始之前，请允许我\n好好监督你～",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))

        PermissionList(
            permissionsStatus = state.status,
            onClick = onClick,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { fallbackToAppSettings(hostContext) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("去授予权限", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            prefs.edit().putBoolean(OnboardingPlugin.KEY_SHOW_PAGE, false).apply()
            showPermissionPage = false
            onAllGranted()
        }) {
            Text(
                "先跳过，天依会难过的",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 14.sp,
            )
        }
    }
}

// ── 设置页「权限管理」入口（SETTINGS_SECTION） ──────────────────

/**
 * 设置页的权限管理入口，注册到 UISlot.SETTINGS_SECTION。
 * 始终显示，方便跳过引导的用户重新授权。
 */
@Composable
fun PermissionSettingsSection(
    hostContext: Context,
) {
    val state = rememberPermissionState(hostContext)
    val onClick = permissionClickHandler(hostContext, state)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "权限管理",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        PermissionList(
            permissionsStatus = state.status,
            onClick = onClick,
        )
    }
}

// ── 辅助函数 ────────────────────────────────────────────────────

/**
 * 跳转到对应权限的系统设置页；失败时降级到应用详情页。
 */
private fun openSettings(context: Context, permissionName: String) {
    val intent = getPermissionIntent(context, permissionName)
    if (intent != null) {
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // fall through to app details
        }
    }
    fallbackToAppSettings(context)
}

private fun fallbackToAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // 无 Activity 可处理，忽略
    }
}

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
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}