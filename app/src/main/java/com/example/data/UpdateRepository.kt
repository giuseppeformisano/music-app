package com.example.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

@JsonClass(generateAdapter = true)
data class VersionInfo(
    @Json(name = "versionCode") val versionCode: Int,
    @Json(name = "versionName") val versionName: String
)

object UpdateRepository {

    private const val TAG = "UpdateRepository"
    private const val REPO = "giuseppeformisano/music-app"
    private const val BUILD_TYPE = "debug"
    private const val RELEASE_TAG = "latest-$BUILD_TYPE"

    // Inserisci qui il tuo Personal Access Token di GitHub (PAT con permesso Contents: Read-only)
    const val GITHUB_PAT = ""

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private val versionAdapter = moshi.adapter(VersionInfo::class.java)

    private fun buildRequest(url: String, acceptHeader: String = "application/vnd.github+json"): Request {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", acceptHeader)
        if (GITHUB_PAT.isNotBlank()) {
            builder.header("Authorization", "Bearer $GITHUB_PAT")
        }
        return builder.build()
    }

    suspend fun checkUpdate(currentVersionCode: Int): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val releaseUrl = "https://api.github.com/repos/$REPO/releases/tags/$RELEASE_TAG"
            val response = client.newCall(buildRequest(releaseUrl)).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val releaseJson = JSONObject(body)
            val assets = releaseJson.optJSONArray("assets") ?: return@withContext null

            var versionAssetId: Long? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == "version.json") {
                    versionAssetId = asset.optLong("id")
                    break
                }
            }

            val versionJsonString = if (versionAssetId != null) {
                val assetUrl = "https://api.github.com/repos/$REPO/releases/assets/$versionAssetId"
                val assetResp = client.newCall(buildRequest(assetUrl, "application/octet-stream")).execute()
                if (assetResp.isSuccessful) assetResp.body?.string() else null
            } else {
                val directUrl = "https://github.com/$REPO/releases/download/$RELEASE_TAG/version.json"
                val directResp = client.newCall(buildRequest(directUrl)).execute()
                if (directResp.isSuccessful) directResp.body?.string() else null
            }

            if (versionJsonString.isNullOrBlank()) return@withContext null
            val info = versionAdapter.fromJson(versionJsonString) ?: return@withContext null
            if (info.versionCode > currentVersionCode) info else null
        } catch (e: Exception) {
            Log.e(TAG, "Errore check aggiornamento: ${e.message}")
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val releaseUrl = "https://api.github.com/repos/$REPO/releases/tags/$RELEASE_TAG"
            val releaseResp = client.newCall(buildRequest(releaseUrl)).execute()
            if (!releaseResp.isSuccessful) return@withContext null
            val releaseBody = releaseResp.body?.string() ?: return@withContext null
            val releaseJson = JSONObject(releaseBody)
            val assets = releaseJson.optJSONArray("assets") ?: return@withContext null

            var apkAssetId: Long? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                if (name == "app-$BUILD_TYPE.apk" || name.endsWith(".apk")) {
                    apkAssetId = asset.optLong("id")
                    break
                }
            }

            val downloadRequest = if (apkAssetId != null) {
                val assetUrl = "https://api.github.com/repos/$REPO/releases/assets/$apkAssetId"
                buildRequest(assetUrl, "application/octet-stream")
            } else {
                val directUrl = "https://github.com/$REPO/releases/download/$RELEASE_TAG/app-$BUILD_TYPE.apk"
                buildRequest(directUrl)
            }

            val response = client.newCall(downloadRequest).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val totalBytes = body.contentLength()

            val dir = File(context.externalCacheDir, "updates").also { it.mkdirs() }
            val apkFile = File(dir, "app-update.apk")

            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress(((downloaded * 100) / totalBytes).toInt())
                        }
                    }
                }
            }
            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Errore download APK: ${e.message}")
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        context.startActivity(intent)
    }
}
