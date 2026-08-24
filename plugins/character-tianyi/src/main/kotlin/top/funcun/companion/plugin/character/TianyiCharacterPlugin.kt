package top.funcun.companion.plugin.character

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.model.Emotion
import top.funcun.companion.sdk.model.RenderMode
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 洛天依角色核心插件。
 * 管理 Live2D (2D) 和 VRM (3D) 双模渲染，自动切换。
 */
class TianyiCharacterPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.character.tianyi")
    override val name = "洛天依角色"
    override val version = SemVer(1, 0, 0)
    override val description = "洛天依 Live2D/VRM 双模渲染"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private lateinit var renderManager: RenderManager

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        renderManager = RenderManager(context.getHostContext(), context.eventBus)
        Log.i(TAG, "TianyiCharacterPlugin loaded")
    }

    override suspend fun onEnable() {
        // 注册全屏天依渲染区（3D VRM）
        ctx.registerUI(UISlot.FOCUS_FULLSCREEN) {
            TianyiCharacterView(
                renderManager = renderManager,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 订阅渲染模式切换事件
        ctx.eventBus.subscribe<AppEvent.RenderModeChanged> { event ->
            renderManager.setMode(event.mode)
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "TianyiCharacterPlugin"
    }
}

/**
 * 渲染管理器。
 * 控制 2D/3D 切换逻辑。
 */
class RenderManager(
    private val context: android.content.Context,
    private val eventBus: top.funcun.companion.sdk.event.EventBus,
) {
    private val _currentMode = MutableStateFlow(RenderMode.FULLSCREEN_3D)
    val currentMode: StateFlow<RenderMode> = _currentMode

    private val _currentEmotion = MutableStateFlow(Emotion.IDLE)
    val currentEmotion: StateFlow<Emotion> = _currentEmotion

    private val _mouthOpen = MutableStateFlow(0f)
    val mouthOpen: StateFlow<Float> = _mouthOpen

    fun setMode(mode: RenderMode) {
        _currentMode.value = mode
        eventBus.emit(AppEvent.RenderModeChanged(mode))
    }

    fun setEmotion(emotion: Emotion) {
        _currentEmotion.value = emotion
    }

    fun setMouthOpen(value: Float) {
        _mouthOpen.value = value.coerceIn(0f, 1f)
    }

    /**
     * 根据电池电量和是否在前台自动切换
     */
    fun autoSwitch(isInForeground: Boolean, batteryLevel: Int) {
        val newMode = when {
            isInForeground && batteryLevel > 15 -> RenderMode.FULLSCREEN_3D
            else -> RenderMode.OVERLAY_2D
        }
        if (newMode != _currentMode.value) {
            setMode(newMode)
        }
    }
}

/**
 * 天依角色 Compose 组件。
 * 当前使用 WebView 加载 three.js VRM 渲染（3D 模式）。
 * 后续可扩展为 Cubism SDK 原生渲染（2D 模式）。
 */
@Composable
fun TianyiCharacterView(
    renderManager: RenderManager,
    modifier: Modifier = Modifier,
) {
    val mode by renderManager.currentMode.collectAsState()
    val emotion by renderManager.currentEmotion.collectAsState()
    val mouthOpen by renderManager.mouthOpen.collectAsState()

    // 3D VRM 渲染（通过 WebView + three.js + @pixiv/three-vrm）
    // 2D 模式将使用 Cubism SDK for Native（需要单独集成）
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.domStorageEnabled = true

                addJavascriptInterface(
                    VrmBridge(renderManager),
                    "TianyiBridge",
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 通知 JS 设置初始情绪
                        view?.evaluateJavascript(
                            "setEmotion('${emotion.name}')",
                            null,
                        )
                    }
                }

                // 加载 VRM 查看器 HTML
                // 实际项目中需要将 viewer.html 放在 assets/vrm/ 目录
                loadUrl("file:///android_asset/vrm/viewer.html")
            }
        },
        update = { webView ->
            // 更新情绪
            webView.evaluateJavascript(
                "setEmotion('${emotion.name}')",
                null,
            )
            // 更新口型
            webView.evaluateJavascript(
                "setMouthOpen($mouthOpen)",
                null,
            )
        },
    )
}

/**
 * JavaScript Bridge。
 * 供 WebView 中的 three.js 调用。
 */
class VrmBridge(private val renderManager: RenderManager) {

    @JavascriptInterface
    fun onModelLoaded() {
        Log.i("VrmBridge", "VRM model loaded")
    }

    @JavascriptInterface
    fun onAnimationEnd() {
        Log.i("VrmBridge", "Animation ended")
    }

    @JavascriptInterface
    fun onTap() {
        // 用户点击了天依
        renderManager.setEmotion(Emotion.PLAYFUL)
    }
}
