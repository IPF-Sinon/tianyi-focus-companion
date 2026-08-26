package top.funcun.companion.plugin.focus

import kotlinx.coroutines.flow.StateFlow

/**
 * 专注服务，通过 SDK 服务机制注册，供其他插件（如 HTML 主界面）调用。
 *
 * 注意：start/stop 为阻塞式（内部自行切协程），与事件总线配合。
 */
interface FocusService {

    /** 当前专注状态 */
    val state: StateFlow<FocusState>

    /** 目标时长（分钟） */
    val targetMinutes: Int

    /** 剩余秒数 */
    val remainingSeconds: Int

    /** 开始专注（默认 25 分钟） */
    fun start(targetMinutes: Int = 25)

    /** 停止/结束当前专注 */
    fun stop()
}

/**
 * 通过该 Token 从 [PluginContext.getService] 获取 FocusService。
 */
val FocusServiceToken = top.funcun.companion.sdk.util.ServiceToken.create<FocusService>("focus_service")