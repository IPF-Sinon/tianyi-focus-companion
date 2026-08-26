package top.funcun.companion.plugin.onboarding

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 权限项定义：名称、级别、图标 */
data class PermissionEntry(
    val name: String,
    val level: PermissionLevel,
    val icon: String,
)

/** 权限 UI 状态，参考 Operit 的 UiState 模式 */
data class PermissionUiState(
    val entries: List<PermissionEntry> = defaultEntries(),
    val status: Map<String, Boolean> = entries.associate { it.name to false },
) {
    val allGranted: Boolean get() = status.values.all { it }
    val accessibilityGranted: Boolean get() = status[ACCESSIBILITY_NAME] == true

    companion object {
        const val NOTIFICATION_NAME = "通知"
        const val OVERLAY_NAME = "悬浮窗"
        const val USAGE_STATS_NAME = "使用情况访问"
        const val ACCESSIBILITY_NAME = "无障碍服务"
        const val CAMERA_NAME = "摄像头"
        const val BATTERY_NAME = "忽略电池优化"

        fun defaultEntries() = listOf(
            PermissionEntry(NOTIFICATION_NAME, PermissionLevel.BASIC, "🔔"),
            PermissionEntry(OVERLAY_NAME, PermissionLevel.SETTINGS, "🪟"),
            PermissionEntry(USAGE_STATS_NAME, PermissionLevel.ADVANCED, "📊"),
            PermissionEntry(ACCESSIBILITY_NAME, PermissionLevel.ACCESSIBILITY, "♿"),
            PermissionEntry(CAMERA_NAME, PermissionLevel.BASIC, "📷"),
            PermissionEntry(BATTERY_NAME, PermissionLevel.SETTINGS, "🔋"),
        )
    }
}

/**
 * 权限状态 ViewModel，参考 Operit 的 PermissionGuideViewModel 模式：
 * - StateFlow 驱动 UI
 * - checkPermissions 集中检查所有权限
 * - 运行时权限结果通过 onXxxResult 回写
 */
class PermissionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    /** 检查所有权限状态并更新到 StateFlow */
    fun checkPermissions(context: Context) {
        val s = mutableMapOf<String, Boolean>()

        s[PermissionUiState.NOTIFICATION_NAME] = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

        s[PermissionUiState.OVERLAY_NAME] = Settings.canDrawOverlays(context)

        s[PermissionUiState.USAGE_STATS_NAME] = if (Build.VERSION.SDK_INT >= 21) {
            try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= 29) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                }
                mode == AppOpsManager.MODE_ALLOWED
            } catch (_: Exception) {
                false
            }
        } else true

        // 无障碍服务：检测 FocusAccessibilityService 是否已启用
        s[PermissionUiState.ACCESSIBILITY_NAME] = isAccessibilityServiceEnabled(context)

        s[PermissionUiState.CAMERA_NAME] = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        s[PermissionUiState.BATTERY_NAME] = if (Build.VERSION.SDK_INT >= 23) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true

        _uiState.update { it.copy(status = s) }
    }

    /** 无障碍服务是否已启用 */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 21) return false
        return try {
            val am =
                context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val serviceName = ComponentName(
                context.packageName,
                OnboardingPlugin.ACCESSIBILITY_SERVICE_CLASS
            )
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { info ->
                    val si = info.resolveInfo?.serviceInfo ?: return@any false
                    si.packageName == serviceName.packageName &&
                        si.name == serviceName.className
                }
        } catch (_: Exception) {
            false
        }
    }

    /** 运行时权限结果回写 */
    fun onNotificationResult(granted: Boolean) {
        _uiState.update { it.copy(status = it.status + (PermissionUiState.NOTIFICATION_NAME to granted)) }
    }

    fun onCameraResult(granted: Boolean) {
        _uiState.update { it.copy(status = it.status + (PermissionUiState.CAMERA_NAME to granted)) }
    }
}
