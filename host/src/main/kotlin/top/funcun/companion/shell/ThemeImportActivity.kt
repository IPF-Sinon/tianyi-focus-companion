package top.funcun.companion.shell

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import top.funcun.companion.plugin.homehtml.HomeHtmlConstants
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 主题导入界面。
 *
 * 用户选择一个 .zip 主题包，解压到
 * `外部文件目录/themes/current/`，完成后自动返回（WebView 恢复时重载主题）。
 *
 * 该 Activity 不依赖 Compose，纯原生实现，避免与主题 WebView 状态互相干扰。
 */
class ThemeImportActivity : ComponentActivity() {

    private val pickZip =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                toast("已取消导入")
                finish()
                return@registerForActivityResult
            }
            val done = importTheme(uri)
            toast(if (done) "主题导入成功，正在应用..." else "主题导入失败：请确认是有效的 zip 主题包")
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 打开系统文件选择器（zip）
        pickZip.launch(arrayOf("application/zip", "application/octet-stream"))
    }

    private fun importTheme(uri: Uri): Boolean {
        return try {
            val targetDir = File(getExternalFilesDir(null), HomeHtmlConstants.USER_THEME_DIR)
            targetDir.deleteRecursively()
            targetDir.mkdirs()

            val input = contentResolver.openInputStream(uri) ?: return false

            // 解压 zip 到目标目录
            var indexFound = false
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // 去掉可能存在的顶层文件夹（主题包通常包含 index.html 在根目录）
                    val name = entry.name.trimStart('/')
                    val relative = name.substringAfterFirst('/').ifBlank { name }
                    val dest = File(targetDir, relative)

                    if (entry.isDirectory) {
                        dest.mkdirs()
                    } else {
                        dest.parentFile?.mkdirs()
                        dest.outputStream().use { out -> zip.copyTo(out) }
                        if (relative == "index.html") indexFound = true
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (!indexFound) {
                // 顶层文件夹内的 index.html 也算（如 theme/index.html）
                indexFound = File(targetDir, "index.html").exists() ||
                    File(targetDir, "theme/index.html").exists()
            }

            indexFound
        } catch (e: Exception) {
            android.util.Log.e("ThemeImport", "import failed", e)
            false
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}