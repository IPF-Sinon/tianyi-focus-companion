package top.funcun.companion.plugin.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.event.AbortReason
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 番茄钟引擎插件。
 * 管理专注会话的状态机、计时器、前台服务。
 */
class FocusEnginePlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.focus")
    override val name = "番茄钟引擎"
    override val version = SemVer(1, 0, 0)
    override val description = "核心番茄钟计时与状态机"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private lateinit var focusStateMachine: FocusStateMachine
    private var focusJob: Job? = null

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        focusStateMachine = FocusStateMachine(
            context.getHostContext(),
            context.eventBus,
        )
        Log.i(TAG, "FocusEnginePlugin loaded")
    }

    override suspend fun onEnable() {
        ctx.registerUI(UISlot.FOCUS_FULLSCREEN) {
            FocusFullscreen(
                stateMachine = focusStateMachine,
                onStart = { targetMinutes ->
                    focusJob = CoroutineScope(Dispatchers.Default).launch {
                        focusStateMachine.start(targetMinutes)
                    }
                },
                onStop = {
                    focusJob?.cancel()
                    focusJob = CoroutineScope(Dispatchers.Default).launch {
                        focusStateMachine.stop()
                    }
                },
            )
        }
    }

    override suspend fun onDisable() {
        focusJob?.cancel()
        focusStateMachine.forceStop()
    }

    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "FocusEnginePlugin"
    }
}

/**
 * 专注状态机。
 */
class FocusStateMachine(
    private val context: android.content.Context,
    private val eventBus: top.funcun.companion.sdk.event.EventBus,
) {
    private val _state = MutableStateFlow(FocusState.IDLE)
    val state: StateFlow<FocusState> = _state

    private var remainingSeconds = 0
    private var targetMinutes = 25
    private var timerJob: Job? = null

    suspend fun start(targetMinutes: Int) {
        if (_state.value != FocusState.IDLE) return

        this.targetMinutes = targetMinutes
        remainingSeconds = targetMinutes * 60
        _state.value = FocusState.ACTIVE

        eventBus.emit(AppEvent.FocusStarted(targetMinutes))

        ForegroundService.start(context)

        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                val elapsed = targetMinutes * 60 - remainingSeconds
                if (elapsed % 60 == 0) {
                    eventBus.emit(AppEvent.FocusTick(
                        elapsedMinutes = elapsed / 60,
                        remainingMinutes = remainingSeconds / 60,
                    ))
                }
            }
            _state.value = FocusState.COMPLETED
            ForegroundService.stop(context)
            eventBus.emit(AppEvent.FocusCompleted(targetMinutes))
        }
    }

    suspend fun stop() {
        timerJob?.cancel()
        if (_state.value == FocusState.ACTIVE) {
            eventBus.emit(AppEvent.FocusAborted(AbortReason.MANUAL))
        }
        _state.value = FocusState.IDLE
        ForegroundService.stop(context)
    }

    fun forceStop() {
        timerJob?.cancel()
        _state.value = FocusState.IDLE
    }

    fun getElapsedSeconds(): Int = targetMinutes * 60 - remainingSeconds
    fun getRemainingSeconds(): Int = remainingSeconds
}

enum class FocusState {
    IDLE,
    ACTIVE,
    PAUSED,
    COMPLETED,
}

/**
 * 前台服务（保活 + 通知栏显示）。
 */
class ForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "专注状态",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("依见钟勤")
            .setContentText("天依正在看着你…")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "focus_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: android.content.Context) {
            val intent = Intent(context, ForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, ForegroundService::class.java))
        }
    }
}

/**
 * 全屏专注 UI。
 * 注册到 FOCUS_FULLSCREEN 插槽，由 UIShell 渲染。
 */
@Composable
fun FocusFullscreen(
    stateMachine: FocusStateMachine,
    onStart: (Int) -> Unit,
    onStop: () -> Unit,
) {
    val state by stateMachine.state.collectAsState()
    var targetMinutes by remember { mutableStateOf(25) }
    val showTimePicker = listOf(25, 45, 60, 120)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            FocusState.IDLE -> {
                Text(
                    text = "准备开始专注",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    showTimePicker.forEach { minutes ->
                        val label = if (minutes == 120) "无限" else "${minutes}分钟"
                        FilterChip(
                            selected = targetMinutes == minutes,
                            onClick = { targetMinutes = minutes },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8A0BF),
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onStart(targetMinutes) },
                    modifier = Modifier.fillMaxWidth(0.6f).height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8A0BF),
                    ),
                ) {
                    Text("开始", fontSize = 18.sp, color = Color.White)
                }
            }

            FocusState.ACTIVE -> {
                val remaining = stateMachine.getRemainingSeconds()
                val minutes = remaining / 60
                val seconds = remaining % 60

                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "天依正在看着你…",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 好感度条（由其他插件注册到 FOCUS_AFFECTION_BAR）
                // 但在此全屏模式下，由 FocusEnginePlugin 自己渲染
                AffectionBarPlaceholder()

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(0.4f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                ) {
                    Text("结束")
                }
            }

            FocusState.COMPLETED -> {
                Text(
                    text = "专注完成！",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6BBF6B),
                )
                Text(
                    text = "天依为你感到开心～",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(0.4f).height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8A0BF),
                    ),
                ) {
                    Text("返回", color = Color.White)
                }
            }

            FocusState.PAUSED -> {
                Text("已暂停", color = Color.White)
            }
        }
    }
}

/**
 * 好感度条占位（实际由 affection-system 插件渲染）。
 * 此处仅作占位，实际会被 FOCUS_AFFECTION_BAR 插槽的内容覆盖。
 */
@Composable
private fun AffectionBarPlaceholder() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(8.dp)) {
        // 留空，由 affection-system 插件填充
    }
}
