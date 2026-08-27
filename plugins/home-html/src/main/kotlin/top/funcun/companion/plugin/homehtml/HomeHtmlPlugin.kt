package top.funcun.companion.plugin.homehtml

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.ThemeHostService
import top.funcun.companion.sdk.ThemeHostServiceToken
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer
import java.io.File

/**
 * 主题插件（WebView 主界面）。
 *
 * 用 WebView 渲染一个「主题包」接管应用全部界面：底栏、专注页、统计页、
 * 插件页、设置页都由主题的 HTML/CSS/JS 决定。
 *
 * 主题包目录（优先级从高到低）：
 * 1. `外部文件目录/themes/current/`（用户自定义主题包，含 index.html）
 * 2. `assets/theme/index.html`（内置默认主题）
 *
 * 主题通过注入的 `TianyiHost` JS 对象访问宿主能力（插件列表、导航项、
 * 配置读写、卸载、插件动作、统计数据等）。
 */
class HomeHtmlPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.homehtml")
    override val name = "主题（HTML 主界面）"
    override val version = SemVer(1, 0, 0)
    override val description = "用 HTML 主题包接管全部界面，支持第三方主题"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    override val builtin = true
    override val iconEmoji = "🌐"

    /**
     * 主题插件的配置页：自定义 HTML。
     * 主题从 Bridge 拿到 schema 后发现 customHtml 非空，会加载该 HTML 渲染配置界面。
     * 该页面提供「导入主题包」「恢复默认主题」功能。
     */
    override val configSchema = top.funcun.companion.sdk.ConfigSchema(
        customHtml = "config.html",
    )

    private lateinit var ctx: PluginContext
    private var themeHost: ThemeHostService? = null

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        themeHost = context.getService(ThemeHostServiceToken)
        Log.i(TAG, "HomeHtmlPlugin loaded (themeHost=${themeHost != null})")
    }

    override suspend fun onEnable() {
        // 注册为全屏主界面：宿主把 HOME_TOP 内容全屏渲染
        ctx.registerUI(UISlot.HOME_TOP) {
            ThemeWebView(
                hostContext = ctx.getHostContext(),
                themeHost = themeHost,
            )
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "HomeHtmlPlugin"
    }
}

/**
 * 主题 WebView：渲染主题包并注入 TianyiHost JS 接口。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThemeWebView(
    hostContext: Context,
    themeHost: ThemeHostService?,
    modifier: Modifier = Modifier,
) {
    var webViewRef: WebView? = null
    // 记录上次加载时用户主题目录的指纹，只有变化才 reload（避免每次回前台丢失 Tab）
    var lastFingerprint by remember { mutableStateOf(themeFingerprint(hostContext)) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val current = themeFingerprint(hostContext)
                if (current != lastFingerprint) {
                    lastFingerprint = current
                    webViewRef?.reload()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.let { ThemeBackBridge.detach(it) }
            webViewRef?.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                ThemeBackBridge.attach(this)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // 安全加固：统一通过拦截器供给资源，禁止直接 file 访问
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW

                addJavascriptInterface(
                    TianyiHostBridge(themeHost),
                    "TianyiHost",
                )

                // 承接 alert/confirm，否则主题里的确认框无效
                webChromeClient = ThemeWebChromeClient(ctx)

                // 统一用虚拟域名加载：用户主题优先，缺失则回退 assets
                webViewClient = ThemeWebViewClient(ctx)
                loadUrl(HomeHtmlConstants.USER_THEME_BASE_URL)
            }
        },
    )
}

/** 用户主题目录指纹：文件数 + 最新修改时间 */
private fun themeFingerprint(context: Context): String {
    val dir = File(context.getExternalFilesDir(null), HomeHtmlConstants.USER_THEME_DIR)
    if (!dir.exists()) return "builtin"
    val files = dir.walkTopDown().filter { it.isFile }.toList()
    val latest = files.maxOfOrNull { it.lastModified() } ?: 0L
    return "user:${files.size}:$latest"
}

/**
 * 主题资源拦截器。
 *
 * 统一以 `https://theme.local/` 作为基地址：
 * - 用户主题目录存在该文件 → 读外部文件
 * - 否则回退读 APK assets 的内置主题
 *
 * 这样内置主题与用户主题的同源策略、相对路径行为完全一致。
 */
private class ThemeWebViewClient(private val context: Context) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url ?: return null
        if (url.host != "theme.local") return null

        val relative = url.path?.trimStart('/')?.ifEmpty { "index.html" } ?: "index.html"
        val mime = mimeOf(relative)

        // 1. 用户主题目录
        val userDir = File(context.getExternalFilesDir(null), HomeHtmlConstants.USER_THEME_DIR)
        val userFile = File(userDir, relative)
        if (userFile.exists() &&
            userFile.canonicalPath.startsWith(userDir.canonicalPath)
        ) {
            return WebResourceResponse(mime, "UTF-8", userFile.inputStream())
        }

        // 2. 回退内置 assets（theme/ 目录）
        return try {
            val stream = context.assets.open("theme/$relative")
            WebResourceResponse(mime, "UTF-8", stream)
        } catch (e: Exception) {
            null
        }
    }

    private fun mimeOf(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js" -> "application/javascript"
        "json" -> "application/json"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "svg" -> "image/svg+xml"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        else -> "application/octet-stream"
    }
}

/**
 * 承接主题内的 alert / confirm，用系统对话框呈现。
 */
private class ThemeWebChromeClient(private val context: Context) :
    android.webkit.WebChromeClient() {

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: android.webkit.JsResult?,
    ): Boolean {
        android.app.AlertDialog.Builder(context)
            .setMessage(message ?: "")
            .setPositiveButton("确定") { _, _ -> result?.confirm() }
            .setOnCancelListener { result?.cancel() }
            .show()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: android.webkit.JsResult?,
    ): Boolean {
        android.app.AlertDialog.Builder(context)
            .setMessage(message ?: "")
            .setPositiveButton("确定") { _, _ -> result?.confirm() }
            .setNegativeButton("取消") { _, _ -> result?.cancel() }
            .setOnCancelListener { result?.cancel() }
            .show()
        return true
    }
}

/**
 * 注入到主题的 JS 接口：window.TianyiHost。
 *
 * 所有方法返回 JSON 字符串，主题用 JSON.parse 解析。
 */
private class TianyiHostBridge(private val host: ThemeHostService?) {

    @JavascriptInterface
    fun getPlugins(): String = host?.getPluginsJson() ?: """{"plugins":[]}"""

    @JavascriptInterface
    fun getNavItems(): String = host?.getNavItemsJson() ?: """{"items":[]}"""

    @JavascriptInterface
    fun getConfigSchema(pluginId: String): String =
        host?.getConfigSchemaJson(pluginId) ?: "null"

    @JavascriptInterface
    fun readConfig(pluginId: String, key: String, defaultValue: String): String =
        host?.readConfig(pluginId, key, defaultValue) ?: defaultValue

    @JavascriptInterface
    fun writeConfig(pluginId: String, key: String, value: String) {
        host?.writeConfig(pluginId, key, value)
    }

    @JavascriptInterface
    fun uninstallPlugin(pluginId: String): Boolean =
        host?.uninstallPlugin(pluginId) ?: false

    @JavascriptInterface
    fun invokeAction(pluginId: String, actionId: String): String =
        host?.invokeAction(pluginId, actionId) ?: "null"

    @JavascriptInterface
    fun requestNavData(pluginId: String, navId: String): String =
        host?.requestNavData(pluginId, navId) ?: "null"

    @JavascriptInterface
    fun getAppInfo(): String = host?.getAppInfoJson() ?: """{"appName":"依见钟勤"}"""

    @JavascriptInterface
    fun getThemeInfo(): String = host?.getThemeInfoJson() ?: """{"installed":false,"source":"builtin"}"""

    @JavascriptInterface
    fun getCustomConfigHtml(pluginId: String): String =
        host?.getCustomConfigHtml(pluginId) ?: ""

    @JavascriptInterface
    fun importTheme(): Boolean = host?.importTheme() ?: false

    @JavascriptInterface
    fun resetTheme(): Boolean = host?.resetTheme() ?: false

    /** 主题日志（便于调试） */
    @JavascriptInterface
    fun log(message: String) {
        Log.i("ThemeJS", message)
    }
}