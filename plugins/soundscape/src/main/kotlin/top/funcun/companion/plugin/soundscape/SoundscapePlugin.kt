package top.funcun.companion.plugin.soundscape

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

class SoundscapePlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.soundscape")
    override val name = "白噪音"
    override val version = SemVer(1, 0, 0)
    override val description = "专注环境音"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private var mediaPlayer: MediaPlayer? = null

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        Log.i(TAG, "SoundscapePlugin loaded")
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {
        stopSound()
    }
    override suspend fun onUnload() {
        stopSound()
    }

    private fun playSound(resId: Int) {
        stopSound()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setLooping(true)
            setVolume(0.3f, 0.3f)
            // 实际项目中会从 assets 加载音频文件
            // setDataSource(context.assets.openFd("soundscape/rain.mp3"))
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }

    private fun stopSound() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "SoundscapePlugin"
    }
}
