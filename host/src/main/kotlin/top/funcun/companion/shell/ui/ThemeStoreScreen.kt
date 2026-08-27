package top.funcun.companion.shell.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import top.funcun.companion.theme.ThemeIO
import top.funcun.companion.theme.ThemeManager
import java.io.File

private data class StoreTheme(
    val id: Int,
    val name: String,
    val author: String,
    val description: String,
    val version: String,
    val downloadUrl: String,
    val previewUrl: String,
)

/**
 * 主题商店：复用 FolkPatch 主题 API（https://folk.mysqil.com/api/themes）。
 */
@Composable
fun ThemeStoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var themes by remember { mutableStateOf<List<StoreTheme>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var applyingId by remember { mutableStateOf<Int?>(null) }

    fun load() {
        loading = true
        error = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { fetchThemes() }
            result.fold(
                onSuccess = { themes = it },
                onFailure = { error = it.message },
            )
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "主题商店",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(40.dp))
                Text("加载失败：$error", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { load() }) { Text("重试") }
            }
            else -> LazyColumn {
                items(themes.orEmpty(), key = { it.id }) { t ->
                    ThemeStoreItem(
                        theme = t,
                        applying = applyingId == t.id,
                        onApply = {
                            applyingId = t.id
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    downloadAndApply(context, t.downloadUrl)
                                }
                                applyingId = null
                                if (ok) {
                                    // 主题已应用，提示
                                }
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ThemeStoreItem(theme: StoreTheme, applying: Boolean, onApply: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = theme.previewUrl,
                contentDescription = theme.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(theme.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(theme.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (theme.description.isNotEmpty()) {
                    Text(theme.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (applying) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Button(onClick = onApply, shape = RoundedCornerShape(16.dp)) {
                    Text("应用")
                }
            }
        }
    }
}

private fun fetchThemes(): Result<List<StoreTheme>> = runCatching {
    val client = OkHttpClient.Builder().build()
    val req = Request.Builder().url("https://folk.mysqil.com/api/themes").build()
    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("HTTP ${resp.code}")
        val text = resp.body?.string() ?: error("empty")
        val arr = JSONArray(text)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            StoreTheme(
                id = o.optInt("id"),
                name = o.optString("name"),
                author = o.optString("author"),
                description = o.optString("description"),
                version = o.optString("version"),
                downloadUrl = o.optString("download_url"),
                previewUrl = o.optString("preview_url"),
            )
        }
    }
}

private fun downloadAndApply(context: android.content.Context, url: String): Boolean {
    return try {
        val client = OkHttpClient.Builder().build()
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val bytes = resp.body?.bytes() ?: return false
            val f = File(context.cacheDir, "theme_import.tmp")
            f.writeBytes(bytes)
            val config = ThemeIO.import(context, Uri.fromFile(f))
            if (config != null) ThemeManager.apply(context, config)
            config != null
        }
    } catch (e: Exception) {
        false
    }
}