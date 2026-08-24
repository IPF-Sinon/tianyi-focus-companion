package top.funcun.companion.plugin.patrol

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.model.PatrolResult
import top.funcun.companion.sdk.model.PatrolSeverity
import top.funcun.companion.sdk.model.PatrolTrigger
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer
import kotlinx.coroutines.*
import java.io.File
import java.util.Random

/**
 * 智能巡查插件。
 * 通过动作检测、界面切换、定时三种方式触发巡查，
 * 调用 VLM API 识别画面内容并做出回应。
 */
class PatrolVlmPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.patrol.vlm")
    override val name = "智能巡查"
    override val version = SemVer(1, 0, 0)
    override val description = "动作/界面/定时触发巡查，VLM 识别并回应"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private lateinit var patrolEngine: PatrolEngine
    private var patrolJob: Job? = null

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        patrolEngine = PatrolEngine(context.getHostContext(), context.eventBus)
        Log.i(TAG, "PatrolVlmPlugin loaded")
    }

    override suspend fun onEnable() {
        // 订阅专注开始事件，启动巡查
        ctx.eventBus.subscribe<AppEvent.FocusStarted> { _ ->
            patrolJob = CoroutineScope(Dispatchers.Default).launch {
                patrolEngine.start()
            }
        }

        // 订阅专注结束事件，停止巡查
        ctx.eventBus.subscribe<AppEvent.FocusCompleted> { _ ->
            patrolJob?.cancel()
            patrolEngine.stop()
        }

        ctx.eventBus.subscribe<AppEvent.FocusAborted> { _ ->
            patrolJob?.cancel()
            patrolEngine.stop()
        }
    }

    override suspend fun onDisable() {
        patrolJob?.cancel()
        patrolEngine.stop()
    }

    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "PatrolVlmPlugin"
    }
}

/**
 * 巡查引擎。
 * 管理三种触发机制并调用 VLM。
 */
class PatrolEngine(
    private val context: android.content.Context,
    private val eventBus: top.funcun.companion.sdk.event.EventBus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isRunning = false
    private val random = Random()

    // 动作检测
    private var sensorManager: SensorManager? = null
    private var lastAccelTime = 0L
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f

    fun start() {
        if (isRunning) return
        isRunning = true

        Log.i(TAG, "Patrol engine started")

        // 1. 定时巡查（随机 5-40 分钟）
        scope.launch {
            while (isRunning) {
                val delayMinutes = random.nextInt(35) + 5 // 5..40
                delay(delayMinutes * 60 * 1000L)
                if (isRunning) {
                    triggerPatrol(PatrolTrigger.TIMER)
                }
            }
        }

        // 2. 动作检测（加速度计）
        sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.registerListener(
            motionListener,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL,
        )
    }

    fun stop() {
        isRunning = false
        sensorManager?.unregisterListener(motionListener)
        Log.i(TAG, "Patrol engine stopped")
    }

    private val motionListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!isRunning) return
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

            val now = System.currentTimeMillis()
            if (now - lastAccelTime < 1000) return // 每秒检查一次

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val delta = Math.abs(x - lastAccelX) + Math.abs(y - lastAccelY) + Math.abs(z - lastAccelZ)
            lastAccelX = x
            lastAccelY = y
            lastAccelZ = z
            lastAccelTime = now

            // 大幅度动作变化触发巡查
            if (delta > 3.0f) {
                scope.launch {
                    triggerPatrol(PatrolTrigger.MOTION)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /**
     * 触发一次巡查。
     * 实际项目中会：抓拍帧/截图 → 上传 VLM API → 解析结果 → 调整好感度并生成回应。
     * 目前为模拟实现。
     */
    private suspend fun triggerPatrol(trigger: PatrolTrigger) {
        eventBus.emit(AppEvent.PatrolTriggered(trigger))

        // 模拟 VLM 调用延迟
        delay(500)

        // 模拟巡查结果
        val studying = random.nextFloat() > 0.3f
        val severity = if (studying) PatrolSeverity.NONE else PatrolSeverity.MILD

        val result = PatrolResult(
            timestampEpochMs = System.currentTimeMillis(),
            trigger = trigger,
            imageUrl = null,
            aiDescription = if (studying) "用户在认真学习" else "用户似乎在分心",
            studying = studying,
            activity = if (studying) "学习" else "玩手机",
            severity = severity,
            affectionDelta = if (studying) 1 else -3,
            companionResponse = if (studying) "不错，继续加油～" else "喂，别偷懒哦～",
        )

        eventBus.emit(AppEvent.PatrolResulted(result))

        Log.i(TAG, "Patrol result: studying=$studying, severity=$severity")
    }

    companion object {
        private const val TAG = "PatrolEngine"
    }
}
