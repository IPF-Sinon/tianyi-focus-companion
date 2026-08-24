package top.funcun.companion.plugin.voice

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.net.URL

/**
 * 端到端语音对话插件。
 * 支持 MiniMax、CosyVoice、GPT-4o voice 等多种语音引擎。
 * 当前为模拟实现，实际需要接入对应 API。
 */
class VoiceConversationPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.voice")
    override val name = "天依语音"
    override val version = SemVer(1, 0, 0)
    override val description = "端到端语音对话与口型同步"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private lateinit var voiceEngine: VoiceEngine
    private var voiceJob: Job? = null

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        voiceEngine = VoiceEngine(context.getHostContext())
        Log.i(TAG, "VoiceConversationPlugin loaded")
    }

    override suspend fun onEnable() {
        // 订阅天依说话事件
        ctx.eventBus.subscribe<AppEvent.CompanionSpeech> { event ->
            voiceJob = CoroutineScope(Dispatchers.Default).launch {
                voiceEngine.speak(
                    text = event.text,
                    emotion = event.emotion,
                    audioUrl = event.audioUrl,
                )
            }
        }
    }

    override suspend fun onDisable() {
        voiceJob?.cancel()
        voiceEngine.stop()
    }

    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "VoiceConversationPlugin"
    }
}

/**
 * 语音引擎。
 * 管理 TTS 合成的生命周期，支持流式播放和口型同步。
 */
class VoiceEngine(private val context: android.content.Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    /**
     * 播放天依语音。
     * 实际项目会调用：
     * - MiniMax T2A API (文本转语音)
     * - 或 CosyVoice 2.0 自部署
     * - 或 GPT-4o voice API
     * 当前为模拟实现，播放一段静音音频。
     */
    suspend fun speak(
        text: String,
        emotion: top.funcun.companion.sdk.model.Emotion,
        audioUrl: String?,
    ) {
        Log.i(TAG, "Speaking: '$text' (emotion=$emotion)")

        if (audioUrl != null) {
            // 使用预合成的音频 URL
            playAudioUrl(audioUrl)
        } else {
            // 模拟语音合成延迟
            delay(text.length * 80L) // 约 80ms/字
            // 实际会调用 TTS API 获取音频流
            Log.i(TAG, "TTS synthesis complete for: $text")
        }
    }

    private fun playAudioUrl(url: String) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener { this@VoiceEngine.isPlaying = false }
                prepareAsync()
                this@VoiceEngine.isPlaying = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying

    companion object {
        private const val TAG = "VoiceEngine"
    }
}
