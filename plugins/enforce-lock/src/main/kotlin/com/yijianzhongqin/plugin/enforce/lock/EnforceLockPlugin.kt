package com.yijianzhongqin.plugin.enforce.lock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.yijianzhongqin.sdk.Plugin
import com.yijianzhongqin.sdk.PluginContext
import com.yijianzhongqin.sdk.util.PluginId
import com.yijianzhongqin.sdk.util.SemVer

/**
 * 深度锁机插件。
 * 使用 DevicePolicyManager 实现锁机模式。
 */
class EnforceLockPlugin : Plugin {

    override val id = PluginId("com.yijianzhongqin.plugin.enforce.lock")
    override val name = "深度锁机"
    override val version = SemVer(1, 0, 0)
    override val description = "DevicePolicyManager 锁机模式"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private lateinit var lockManager: LockManager

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        lockManager = LockManager(context.getHostContext())
        Log.i(TAG, "EnforceLockPlugin loaded")
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "EnforceLockPlugin"
    }
}

class LockManager(context: Context) {
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminReceiver = ComponentName(context, LockAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(adminReceiver.packageName)

    fun lockScreen() {
        if (isDeviceOwner()) {
            dpm.lockNow()
        }
    }

    fun startLockTask(packages: List<String>) {
        if (isDeviceOwner()) {
            dpm.setLockTaskPackages(adminReceiver, packages.toTypedArray())
        }
    }
}

class LockAdminReceiver : android.app.admin.DeviceAdminReceiver()
