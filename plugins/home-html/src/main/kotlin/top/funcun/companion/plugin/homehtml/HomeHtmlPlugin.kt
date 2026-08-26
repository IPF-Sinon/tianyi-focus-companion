package top.funcun.companion.plugin.homehtml

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.funcun.companion.plugin.focus.FocusService
import top.funcun.companion.plugin.focus.FocusServiceToken
import top.funcun.companion.plugin.focus.FocusState
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * HTML 主界面插件。
 *
 * 通过 WebView 渲染 HTML 文件作为主界面。HTML 中的特定标识（data-tianyi-*）
 * 会由 JS 桥接层绑定到真实功能（开始专注/停止专注等）。
 *
 * HTML 来源（优先级从高到低）：
 * 1. [HOME_HTML_FILENAME]（应用外部文件目录，用户可自定义）
 * 2. assets 内置默认 home.html
 */
class HomeHtmlPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.homehtml")
    override val name = "HTML 主界面"
    override val version = SemVer(1, 0, 0)
    override val description = "WebView 渲染 HTML 主界面，支持自定义"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext
    private var focusService: FocusService? = null

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        Log.i(TAG, "HomeHtmlPlugin loaded")
    }

    override suspend fun onEnable() {
        focusService = ctx.getService(FocusServiceToken)
        if (focusService == null) {
            Log.w(TAG, "FocusService not found (focus-engine not loaded?)")
        }
        ctx.registerUI(UISlot.HOME_TOP) {
            HomeHtmlView(
                hostContext = ctx.getHostContext(),
                focusService = focusService,
            )
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "HomeHtmlPlugin"

        /** 用户可自定义的主界面 HTML 文件名（放在应用外部文件目录） */
        private const val HOME_HTML_FILENAME = "home.html"
    }
}

/**
 * 读取主界面 HTML：
 * 优先外部文件目录的用户自定义文件，否则回退到 assets 内置。
 */
private fun loadHomeHtml(context: Context): String {
    // 1. 用户自定义文件（应用外部文件目录，adb/文件管理器可访问）
    val userFile = java.io.File(context.getExternalFilesDir(null), "home.html")
    if (userFile.exists()) {
        return userFile.readText()
    }

    // 2. assets 内置默认
    return try {
        context.assets.open("home.html").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        Log.e("HomeHtmlPlugin", "Failed to load assets home.html", e)
        DEFAULT_HOME_HTML
    }
}

private const val DEFAULT_HOME_HTML = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>依见钟勤</title>
<style>
  :root { --primary: #4A90E2; --bg: #EEF3FA; --card: #FFFFFF; --text: #0B1A33; --sub: #6B7A93; }
  * { margin: 0; padding: 0; box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
  body { font-family: -apple-system, sans-serif; background: var(--bg); color: var(--text); min-height: 100vh; padding: 20px 16px; }
  .card { background: var(--card); border-radius: 24px; padding: 20px; margin-bottom: 16px; box-shadow: 0 8px 24px rgba(74,144,226,.08); }
  h1 { font-size: 22px; font-weight: 700; margin-bottom: 4px; }
  .sub { color: var(--sub); font-size: 13px; }
  button.primary { width: 100%; padding: 16px; border: none; border-radius: 24px;
    background: linear-gradient(135deg, #66CCFF, #4A90E2); color: #fff; font-size: 17px; font-weight: 600;
    margin-top: 12px; cursor: pointer; }
  .timer { text-align: center; font-size: 48px; font-weight: 700; font-variant-numeric: tabular-nums; padding: 8px 0; }
  .row { display: flex; gap: 10px; }
  .row button { flex: 1; padding: 14px; border: none; border-radius: 16px; font-size: 15px; font-weight: 500; cursor: pointer; }
  .chip { display: inline-block; padding: 4px 12px; border-radius: 30px; background: #F0F7FF; color: var(--primary); font-size: 12px; font-weight: 500; }
</style>
</head>
<body>
  <div class="card">
    <h1>依见钟勤</h1>
    <p class="sub">遇见天依之后，对学习一见钟情</p>
  </div>

  <div class="card">
    <div class="row" style="margin-bottom: 12px;">
      <span class="chip">专注</span>
    </div>
    <div class="timer" id="timer">00:00</div>
    <button class="primary" data-tianyi-action="focus-start" data-tianyi-minutes="25">开始专注 25 分钟</button>
    <button class="primary" data-tianyi-action="focus-stop" style="background: #E05555; display:none;" id="stopBtn">结束专注</button>
  </div>

  <script>
    // 由 Android 注入：window.TianyiBridge
    function bindActions() {
      document.querySelectorAll('[data-tianyi-action]').forEach(function (el) {
        el.addEventListener('click', function () {
          var action = el.getAttribute('data-tianyi-action');
          if (!window.TianyiBridge) return;
          if (action === 'focus-start') {
            var minutes = parseInt(el.getAttribute('data-tianyi-minutes') || '25', 10);
            window.TianyiBridge.startFocus(minutes);
          } else if (action === 'focus-stop') {
            window.TianyiBridge.stopFocus();
          }
        });
      });
    }

    document.addEventListener('DOMContentLoaded', bindActions);
  </script>
</body>
</html>
"""

/**
 * WebView 渲染 HTML 主界面，并注入 JS bridge 对接真实功能。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomeHtmlView(
    hostContext: Context,
    focusService: FocusService?,
    modifier: Modifier = Modifier,
) {
    var webView: WebView? = null
    val bridgeScope = remember { CoroutineScope(Dispatchers.Main) }

    DisposableEffect(Unit) {
        val subscription = focusService?.state?.let { flow ->
            bridgeScope.launch {
                flow.collectLatest { state ->
                    webView?.post {
                        webView?.evaluateJavascript(
                            "window.onTianyiFocusState && window.onTianyiFocusState('${state.name}')",
                            null,
                        )
                    }
                }
            }
        }
        onDispose {
            subscription?.cancel()
            webView?.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webView = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun startFocus(minutes: Int) {
                            bridgeScope.launch {
                                focusService?.start(minutes)
                            }
                        }

                        @JavascriptInterface
                        fun stopFocus() {
                            bridgeScope.launch {
                                focusService?.stop()
                            }
                        }
                    },
                    "TianyiBridge",
                )

                loadDataWithBaseURL(
                    "file:///android_asset/",
                    loadHomeHtml(ctx),
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
    )
}