package com.yijianzhongqin.shell.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yijianzhongqin.sdk.slot.UISlot

/**
 * 全屏专注页。
 * 完全由插件渲染（FOCUS_FULLSCREEN 插槽），
 * 包括天依角色、计时器、控制按钮、好感度条。
 */
@Composable
fun FocusScreen(
    onFinish: () -> Unit,
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 插件提供的全屏专注界面
        getSlotContents(UISlot.FOCUS_FULLSCREEN).forEach { it() }
        // 后续可添加插件提供的 OVERLAY_WARNING 等在顶层
        getSlotContents(UISlot.OVERLAY_WARNING).forEach { it() }
    }
}
