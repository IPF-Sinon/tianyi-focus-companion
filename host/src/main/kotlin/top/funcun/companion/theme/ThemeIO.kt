package top.funcun.companion.theme

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipInputStream

/**
 * 主题包导入。
 *
 * 支持两种格式：
 * 1. `.fpt`（FolkPatch 主题包）：AES/CBC/PKCS5 加密的 zip，前 16 字节为 IV
 * 2. `.zip`（明文）：直接是 zip
 *
 * 解出的资源落地到 [themeDir]（外部文件目录 themes/current/），
 * 其中 theme.json 解析为 [ThemeConfig]。
 */
object ThemeIO {

    private const val TAG = "ThemeIO"

    /** FolkPatch 主题包加密密钥源串 */
    private const val FOLKPATCH_KEY = "FolkPatchThemeSecretKey2025"

    /** 用户主题目录（相对外部文件目录） */
    const val THEME_DIR = "themes/current"

    fun themeDir(context: Context): File =
        File(context.getExternalFilesDir(null), THEME_DIR)

    /**
     * 从 Uri 导入主题包。
     * @return 成功解析的 ThemeConfig，失败返回 null
     */
    fun import(context: Context, uri: Uri): ThemeConfig? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null

            val dir = themeDir(context)
            dir.deleteRecursively()
            dir.mkdirs()

            // 先尝试当作明文 zip，失败再尝试 FolkPatch 加密格式
            val ok = extractPlainZip(bytes, dir) || extractFolkPatchFpt(bytes, dir)
            if (!ok) {
                Log.e(TAG, "Unrecognized theme package format")
                return null
            }

            loadConfig(dir)
        } catch (e: Exception) {
            Log.e(TAG, "import failed", e)
            null
        }
    }

    /** 读取已导入主题目录的 theme.json */
    fun loadConfig(dir: File): ThemeConfig? {
        val json = File(dir, ThemeConfig.CONFIG_FILE)
        if (!json.exists()) return null
        return parseConfig(json.readText())
    }

    /** 当前是否有已安装的用户主题 */
    fun hasUserTheme(context: Context): Boolean =
        File(themeDir(context), ThemeConfig.CONFIG_FILE).exists()

    /** 删除用户主题 */
    fun reset(context: Context): Boolean =
        try { themeDir(context).deleteRecursively() } catch (e: Exception) { false }

    // ── 解包 ────────────────────────────────────────────────

    private fun extractPlainZip(bytes: ByteArray, dir: File): Boolean {
        // zip 魔数 PK\x03\x04
        if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
            return false
        }
        return try {
            ZipInputStream(bytes.inputStream()).use { zis -> unzipInto(zis, dir) }
            File(dir, ThemeConfig.CONFIG_FILE).exists() ||
                flattenSingleTopDir(dir)
        } catch (e: Exception) {
            false
        }
    }

    private fun extractFolkPatchFpt(bytes: ByteArray, dir: File): Boolean {
        if (bytes.size <= 16) return false
        return try {
            val iv = bytes.copyOfRange(0, 16)
            val body = bytes.copyOfRange(16, bytes.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, folkPatchKey(), IvParameterSpec(iv))
            CipherInputStream(body.inputStream(), cipher).use { cis ->
                ZipInputStream(BufferedInputStream(cis)).use { zis -> unzipInto(zis, dir) }
            }
            File(dir, ThemeConfig.CONFIG_FILE).exists() || flattenSingleTopDir(dir)
        } catch (e: Exception) {
            Log.w(TAG, "not a FolkPatch fpt or wrong key", e)
            false
        }
    }

    private fun unzipInto(zis: ZipInputStream, dir: File) {
        var entry = zis.nextEntry
        while (entry != null) {
            val name = entry.name.trimStart('/')
            val dest = File(dir, name)
            if (dest.canonicalPath.startsWith(dir.canonicalPath)) {
                if (entry.isDirectory) {
                    dest.mkdirs()
                } else {
                    dest.parentFile?.mkdirs()
                    dest.outputStream().use { out -> zis.copyTo(out) }
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }

    /** 若 theme.json 在单层顶层目录内，则把该目录内容上提 */
    private fun flattenSingleTopDir(dir: File): Boolean {
        val children = dir.listFiles() ?: return false
        val dirs = children.filter { it.isDirectory }
        if (children.size == 1 && dirs.size == 1) {
            val inner = dirs[0]
            if (File(inner, ThemeConfig.CONFIG_FILE).exists()) {
                inner.listFiles()?.forEach { f ->
                    f.copyRecursively(File(dir, f.name), overwrite = true)
                }
                inner.deleteRecursively()
                return true
            }
        }
        return false
    }

    private fun folkPatchKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(FOLKPATCH_KEY.toByteArray())
        return SecretKeySpec(bytes, "AES")
    }

    // ── JSON 解析（兼容 FolkPatch 字段名与我们自己的字段名）────

    private fun parseConfig(jsonStr: String): ThemeConfig {
        val j = JSONObject(jsonStr)
        fun str(vararg keys: String, def: String): String {
            for (k in keys) if (j.has(k)) return j.optString(k, def)
            return def
        }
        fun bool(vararg keys: String, def: Boolean): Boolean {
            for (k in keys) if (j.has(k)) return j.optBoolean(k, def)
            return def
        }
        fun flt(vararg keys: String, def: Float): Float {
            for (k in keys) if (j.has(k)) return j.optDouble(k, def.toDouble()).toFloat()
            return def
        }
        fun int(vararg keys: String, def: Int): Int {
            for (k in keys) if (j.has(k)) return j.optInt(k, def)
            return def
        }

        return ThemeConfig(
            metaName = str("meta_name", "metaName", def = "导入的主题"),
            metaAuthor = str("meta_author", "metaAuthor", def = ""),
            metaVersion = str("meta_version", "metaVersion", def = "1.0"),
            metaDescription = str("meta_description", "metaDescription", def = ""),
            customColor = str("customColor", "custom_color", def = "#4A90E2"),
            useSystemDynamicColor = bool("useSystemDynamicColor", def = false),
            colorGenerationMode = str("colorGenerationMode", def = "classic"),
            colorStandard = str("colorStandard", def = "MD3_2021"),
            colorStyle = str("colorStyle", def = "TONAL_SPOT"),
            nightModeEnabled = bool("nightModeEnabled", def = false),
            nightModeFollowSys = bool("nightModeFollowSys", def = true),
            isBackgroundEnabled = bool("isBackgroundEnabled", def = false),
            backgroundOpacity = flt("backgroundOpacity", def = 1.0f),
            backgroundBlur = flt("backgroundBlur", def = 0f),
            backgroundDim = flt("backgroundDim", def = 0.2f),
            isDualBackgroundDimEnabled = bool("isDualBackgroundDimEnabled", def = false),
            backgroundDayDim = flt("backgroundDayDim", def = 0.1f),
            backgroundNightDim = flt("backgroundNightDim", def = 0.4f),
            isFontEnabled = bool("isFontEnabled", def = false),
            homeLayoutStyle = str("homeLayoutStyle", def = "dashboard"),
            cardCornerRadius = int("cardCornerRadius", def = 24),
        )
    }
}