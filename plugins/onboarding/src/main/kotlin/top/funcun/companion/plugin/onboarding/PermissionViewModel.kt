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

/** 权限 UI 状态 */
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
 * 检查所有权限状态并写入 [target]。
 * 供 ViewModel 与插件 getNavData 共用。
 */
internal fun Context.checkPermissionsInto(target: MutableMap<String, Boolean>) {
    target[PermissionUiState.NOTIFICATION_NAME] = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else true

    target[PermissionUiState.OVERLAY_NAME] = Settings.canDrawOverlays(this)

    target[PermissionUiState.USAGE_STATS_NAME] = try {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }

    target[PermissionUiState.ACCESSIBILITY_NAME] = checkAccessibilityEnabled()

    target[PermissionUiState.CAMERA_NAME] = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    target[PermissionUiState.BATTERY_NAME] = if (Build.VERSION.SDK_INT >= 23) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(packageName)
    } else true
}

/** 无障碍服务是否已启用 */
internal fun Context.checkAccessibilityEnabled(): Boolean = try {
    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val serviceName = ComponentName(
        packageName,
        OnboardingPlugin.ACCESSIBILITY_SERVICE_CLASS,
    )
    am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { info ->
            val si = info.resolveInfo?.serviceInfo
            si != null &&
                si.packageName == serviceName.packageName &&
                si.name == serviceName.className
        }
} catch (_: Exception) {
    false
}

/**
 * 权限状态 ViewModel：StateFlow 驱动 UI。
 */
class PermissionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    /** 检查所有权限状态并更新到 StateFlow */
    fun checkPermissions(context: Context) {
        val s = mutableMapOf<String, Boolean>()
        context.checkPermissionsInto(s)
        _uiState.update { it.copy(status = s) }
    }

    /** 无障碍服务是否已启用 */
    fun isAccessibilityServiceEnabled(context: Context): Boolean =
        context.checkAccessibilityEnabled()

    /** 运行时权限结果回写 */
    fun onNotificationResult(granted: Boolean) {
        _uiState.update { it.copy(status = it.status + (PermissionUiState.NOTIFICATION_NAME to granted)) }
    }

    fun onCameraResult(granted: Boolean) {
        _uiState.update { it.copy(status = it.status + (PermissionUiState.CAMERA_NAME to granted)) }
    }
}