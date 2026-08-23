package com.yijianzhongqin.sdk.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 事件总线。
 * 插件间通过事件总线通信，不直接引用。
 * 发送者不知道接收者是谁，接收者只订阅自己关心的事件。
 */
class EventBus {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )

    /** 所有事件流，插件通过过滤特定类型来订阅 */
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /**
     * 发送事件到总线。
     * 所有订阅了此事件类型的插件都会收到通知。
     */
    fun emit(event: AppEvent) {
        scope.launch {
            _events.emit(event)
        }
    }

    /**
     * 订阅特定类型的事件。
     * @param filter 事件类型过滤器
     * @param handler 事件处理器
     */
    inline fun <reified T : AppEvent> subscribe(
        crossinline handler: (T) -> Unit,
    ) {
        scope.launch {
            _events.collect { event ->
                if (event is T) {
                    handler(event)
                }
            }
        }
    }
}
