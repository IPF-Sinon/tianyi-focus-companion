package com.yijianzhongqin.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class RenderState(
    val currentMode: RenderMode = RenderMode.FULLSCREEN_3D,
    val isAutoSwitch: Boolean = true,
    val batteryLevel: Int = 100,
)

@Serializable
enum class RenderMode {
    FULLSCREEN_3D,
    OVERLAY_2D,
    MINIMAL
}
