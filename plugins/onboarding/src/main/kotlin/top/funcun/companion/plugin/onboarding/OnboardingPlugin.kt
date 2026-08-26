package top.funcun.companion.plugin.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
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
 *
 * 架构参考 Operit：
 * - PermissionViewModel + StateFlow 管理权限状态
 * - PermissionStatusItem 单行状态项
 * - AccessibilityWizardCard 分步无障碍引导
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
        internal const val PREFS_NAME = "onboarding_prefs"
        internal const val KEY_SHOW_PAGE = "show_page"
        internal const val ACCESSIBILITY_SERVICE_CLASS =
            "top.funcun.companion.plugin.enforce.FocusAccessibilityService"
    }
}

// ── ViewModel 组合入口 ──────────────────────────────────────────

/** 权限状态与运行时 Launcher 的组合持有 */
private class PermissionUiBindings(
    val viewModel: PermissionViewModel,
) {
    var notificationLauncher: ManagedActivityResultLauncher<String, Boolean>? = null
    var cameraLauncher: ManagedActivityResultLauncher<String, Boolean>? = null

    fun onPermissionClick(context: Context, name: String, openSettings: (Context, String) -> Unit) {
        when (name) {
            PermissionUiState.NOTIFICATION_NAME -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                        ?: openSettings(context, name)
                } else {
                    openSettings(context, name)
                }
            }
            PermissionUiState.CAMERA_NAME -> {
                if (Build.VERSION.SDK_INT >= 23) {
                    cameraLauncher?.launch(Manifest.permission.CAMERA)
                        ?: openSettings(context, name)
                } else {
                    openSettings(context, name)
                }
            }
            else -> openSettings(context, name)
        }
    }
}

/**
 * 创建并持有 PermissionViewModel + 运行时 Launcher，
 * 注册初始检查与 onResume 自动刷新。
 */
@Composable
private fun rememberPermissionBindings(hostContext: Context): PermissionUiBindings {
    // viewModel() 是 Composable 调用，必须在 remember 外先获取
    val vm: PermissionViewModel = viewModel()
    val bindings = remember(vm) { PermissionUiBindings(vm) }

    bindings.notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        vm.onNotificationResult(granted)
    }

    bindings.cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        vm.onCameraResult(granted)
    }

    // 初始检查
    LaunchedEffect(Unit) {
        vm.checkPermissions(hostContext)
    }

    // onResume 刷新：从系统设置页返回时自动更新
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.checkPermissions(hostContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return bindings
}

// ── 通用权限列表（FolkPatch 卡片 + Operit 状态项） ───────────────

/**
 * 权限列表卡片：
 * - FolkPatch 风格 RoundedCornerShape(24.dp) surfaceContainer 卡片
 * - 内部使用 Operit 风格 PermissionStatusItem 单行状态项
 * - 无障碍服务未授予时点击展开 AccessibilityWizardCard 分步向导
 */
@Composable
private fun PermissionList(
    uiState: PermissionUiState,
    onClick: (String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    var wizardExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column {
            uiState.entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
                val isAccessibility = entry.name == PermissionUiState.ACCESSIBILITY_NAME
                PermissionStatusItem(
                    icon = entry.icon,
                    title = entry.name,
                    level = entry.level,
                    isGranted = uiState.status[entry.name] == true,
                    onClick = {
                        if (isAccessibility && uiState.accessibilityGranted.not()) {
                            wizardExpanded = !wizardExpanded
                        } else {
                            onClick(entry.name)
                        }
                    },
                )
                // 无障碍服务分步向导（内嵌在该项下方）
                if (isAccessibility) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = wizardExpanded && !uiState.accessibilityGranted
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            AccessibilityWizardCard(
                                isServiceEnabled = uiState.accessibilityGranted,
                                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            )
                        }
                    }
                }
            }
        }
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
    val bindings = rememberPermissionBindings(hostContext)
    val uiState by bindings.viewModel.uiState.collectAsState()

    // 全部授予后自动隐藏
    LaunchedEffect(uiState.allGranted) {
        if (uiState.allGranted) {
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
            uiState = uiState,
            onClick = { name ->
                bindings.onPermissionClick(hostContext, name) { c, n ->
                    openPermissionSettings(c, n)
                }
            },
            onOpenAccessibilitySettings = {
                openPermissionSettings(hostContext, PermissionUiState.ACCESSIBILITY_NAME)
            },
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
    val bindings = rememberPermissionBindings(hostContext)
    val uiState by bindings.viewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "权限管理",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        PermissionList(
            uiState = uiState,
            onClick = { name ->
                bindings.onPermissionClick(hostContext, name) { c, n ->
                    openPermissionSettings(c, n)
                }
            },
            onOpenAccessibilitySettings = {
                openPermissionSettings(hostContext, PermissionUiState.ACCESSIBILITY_NAME)
            },
        )
    }
}

// ── 辅助函数 ────────────────────────────────────────────────────

/**
 * 跳转到对应权限的系统设置页；失败时降级到应用详情页。
 */
internal fun openPermissionSettings(context: Context, permissionName: String) {
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

internal fun fallbackToAppSettings(context: Context) {
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

internal fun getPermissionIntent(context: Context, permissionName: String): Intent? {
    val intent: Intent? = when (permissionName) {
        PermissionUiState.ACCESSIBILITY_NAME -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        PermissionUiState.USAGE_STATS_NAME -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        PermissionUiState.CAMERA_NAME -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        PermissionUiState.NOTIFICATION_NAME -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else null
        PermissionUiState.BATTERY_NAME -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        PermissionUiState.OVERLAY_NAME -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        else -> null
    }
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}