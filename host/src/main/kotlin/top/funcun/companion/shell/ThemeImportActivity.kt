package top.funcun.companion.shell

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import top.funcun.companion.theme.ThemeIO
import top.funcun.companion.theme.ThemeManager

/**
 * 主题导入界面。
 *
 * 用户选择一个 `.fpt`（FolkPatch 主题包）或 `.zip` 主题包，
 * 由 [ThemeIO] 解密/解压到 `外部文件目录/themes/current/`，
 * 解析 theme.json 并应用，完成后返回。
 */
class ThemeImportActivity : ComponentActivity() {

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                toast("已取消导入")
                finish()
                return@registerForActivityResult
            }
            val config = ThemeIO.import(this, uri)
            if (config != null) {
                ThemeManager.apply(this, config)
                toast("主题「${config.metaName}」已应用")
            } else {
                toast("导入失败：不是有效的主题包（.fpt / .zip）")
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 打开系统文件选择器（fpt/zip/任意）
        pickFile.launch(arrayOf("*/*"))
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}