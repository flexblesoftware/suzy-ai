package com.suzy.ai.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.suzy.ai.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateState(
    val isChecking: Boolean = false,
    val updateAvailable: Boolean = false,
    val latestVersion: String = "",
    val downloadUrl: String = "",
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val isReadyToInstall: Boolean = false,
    val downloadedFile: File? = null,
    val statusMessage: String = ""
)

class UpdateManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient()
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val repoApiUrl = "https://api.github.com/repos/flexblesoftware/suzy-ai/releases/latest"

    fun checkForUpdate(silent: Boolean = true) {
        scope.launch(Dispatchers.IO) {
            if (!silent) {
                _state.value = _state.value.copy(isChecking = true, statusMessage = "Checking for updates...")
            }
            try {
                val req = Request.Builder()
                    .url(repoApiUrl)
                    .header("Accept", "application/vnd.github+json")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        if (!silent) {
                            _state.value = _state.value.copy(isChecking = false, statusMessage = "Check failed (${resp.code})")
                        }
                        return@launch
                    }
                    val body = resp.body?.string() ?: return@launch
                    val json = JSONObject(body)
                    val tag = json.optString("tag_name", "").removePrefix("v")
                    val assets = json.optJSONArray("assets")
                    var apkUrl = ""
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i) ?: continue
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    val currentVer = BuildConfig.VERSION_NAME
                    if (tag.isNotBlank() && apkUrl.isNotBlank() && isNewerVersion(tag, currentVer)) {
                        _state.value = UpdateState(
                            updateAvailable = true,
                            latestVersion = tag,
                            downloadUrl = apkUrl,
                            statusMessage = "New update v$tag available"
                        )
                    } else if (!silent) {
                        _state.value = _state.value.copy(
                            isChecking = false,
                            updateAvailable = false,
                            statusMessage = "Suzy v$currentVer is up to date"
                        )
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _state.value = _state.value.copy(isChecking = false, statusMessage = "Network error")
                }
            }
        }
    }

    fun startDownload() {
        val url = _state.value.downloadUrl
        val ver = _state.value.latestVersion
        if (url.isBlank()) return

        val fileName = "Suzy-v$ver.apk"
        val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val outFile = File(destDir, fileName)
        if (outFile.exists()) outFile.delete()

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Suzy AI v$ver")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = dm.enqueue(request)
        _state.value = _state.value.copy(
            isDownloading = true,
            progress = 0f,
            statusMessage = "Downloading v$ver..."
        )

        scope.launch(Dispatchers.IO) {
            var active = true
            while (active) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                dm.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                        val pct = if (total > 0) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                        when (status) {
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                _state.value = _state.value.copy(
                                    progress = pct,
                                    statusMessage = "Downloading ${(pct * 100).toInt()}%"
                                )
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                active = false
                                _state.value = _state.value.copy(
                                    isDownloading = false,
                                    progress = 1f,
                                    isReadyToInstall = true,
                                    downloadedFile = outFile,
                                    statusMessage = "Download complete! Ready to install."
                                )
                                installApk(outFile)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                active = false
                                _state.value = _state.value.copy(
                                    isDownloading = false,
                                    statusMessage = "Download failed. Try again."
                                )
                            }
                        }
                    }
                }
                delay(250)
            }
        }
    }

    fun installApk(file: File? = _state.value.downloadedFile) {
        if (file == null || !file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(updateAvailable = false)
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val lParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(lParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
