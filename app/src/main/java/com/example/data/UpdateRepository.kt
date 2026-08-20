package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private const val BASE_URL = "https://github.com/$REPO/releases/download/$RELEASE_TAG"
    const val APK_URL = "$BASE_URL/app-$BUILD_TYPE.apk"
    private const val VERSION_URL = "$BASE_URL/version.json"

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private val versionAdapter = moshi.adapter(VersionInfo::class.java)

    suspend fun checkUpdate(currentVersionCode: Int): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(VERSION_URL).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val info = versionAdapter.fromJson(body) ?: return@withContext null
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
            val request = Request.Builder().url(APK_URL).build()
            val response = client.newCall(request).execute()
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
