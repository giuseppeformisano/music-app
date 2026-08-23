package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class Base64Fetcher(
    private val data: String,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val base64Clean = if (data.contains(",")) data.substringAfter(",") else data
        val bytes = Base64.decode(base64Clean.trim(), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("Could not decode Base64 image into bitmap")
        val drawable = BitmapDrawable(options.context.resources, bitmap)
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            val trimmed = data.trim()
            if (trimmed.startsWith("data:image", ignoreCase = true) ||
                (trimmed.length > 200 && !trimmed.startsWith("http", ignoreCase = true) && !trimmed.startsWith("file:", ignoreCase = true) && !trimmed.startsWith("content:", ignoreCase = true))
            ) {
                return Base64Fetcher(trimmed, options)
            }
            return null
        }
    }
}

object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Salva un'immagine selezionata da URI di sistema direttamente nello storage interno privato dell'app.
     * Ridimensiona e comprime in JPEG per ottimizzare lo spazio e la velocità di caricamento.
     * Restituisce la URI file:/// del file salvato localmente, immediatamente visualizzabile da Coil.
     */
    fun saveImageLocally(
        context: Context,
        uri: Uri,
        prefix: String,
        userId: String,
        maxDimension: Int = 512,
        quality: Int = 80
    ): String? {
        return try {
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.isEmpty()) return null

            // Decodifica dimensioni
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var inSampleSize = 1
            if (origHeight > maxDimension || origWidth > maxDimension) {
                val halfHeight = origHeight / 2
                val halfWidth = origWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            val width = decodedBitmap.width
            val height = decodedBitmap.height
            val maxSide = maxOf(width, height)
            val scale = (maxDimension.toFloat() / maxSide).coerceAtMost(1f)

            val scaledBitmap = if (scale < 1f) {
                val targetW = (width * scale).toInt().coerceAtLeast(1)
                val targetH = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true).also {
                    if (it != decodedBitmap) decodedBitmap.recycle()
                }
            } else {
                decodedBitmap
            }

            val safeId = userId.ifBlank { "current_user" }.replace("/", "_").replace(":", "_")
            val targetDir = File(context.filesDir, "profiles").apply { if (!exists()) mkdirs() }
            val targetFile = File(targetDir, "${prefix}_${safeId}_${System.currentTimeMillis()}.jpg")

            FileOutputStream(targetFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            scaledBitmap.recycle()

            "file://${targetFile.absolutePath}"
        } catch (e: Exception) {
            Log.e(TAG, "Errore saveImageLocally: ${e.message}", e)
            null
        }
    }

    /**
     * Converte un file locale file:/// o /data/... in stringa Base64 per la persistenza su Firestore.
     */
    fun fileUriToBase64(fileUriOrPath: String?): String? {
        if (fileUriOrPath.isNullOrBlank()) return null
        if (!fileUriOrPath.startsWith("file:", ignoreCase = true) && !fileUriOrPath.startsWith("/")) {
            return fileUriOrPath
        }
        return try {
            val cleanPath = if (fileUriOrPath.startsWith("file://", ignoreCase = true)) {
                fileUriOrPath.removePrefix("file://")
            } else if (fileUriOrPath.startsWith("file:/", ignoreCase = true)) {
                fileUriOrPath.removePrefix("file:")
            } else {
                fileUriOrPath
            }
            val file = File(cleanPath)
            if (!file.exists()) return fileUriOrPath
            val bytes = file.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.e(TAG, "Errore fileUriToBase64: ${e.message}")
            fileUriOrPath
        }
    }

    /**
     * Se una stringa da Firestore è in formato Base64 (data:image/jpeg;base64,...),
     * la decodifica e la scrive in un file locale privato, restituendo la URI file:///.
     */
    fun base64ToLocalFile(context: Context, base64OrUrl: String?, prefix: String, userId: String): String? {
        if (base64OrUrl.isNullOrBlank()) return null
        if (base64OrUrl.startsWith("http://", ignoreCase = true) ||
            base64OrUrl.startsWith("https://", ignoreCase = true) ||
            base64OrUrl.startsWith("file://", ignoreCase = true) ||
            base64OrUrl.startsWith("file:/", ignoreCase = true)
        ) {
            return base64OrUrl
        }
        return try {
            val cleanBase64 = if (base64OrUrl.contains(",")) base64OrUrl.substringAfter(",") else base64OrUrl
            val bytes = Base64.decode(cleanBase64.trim(), Base64.DEFAULT)
            if (bytes.isEmpty()) return base64OrUrl

            val safeId = userId.ifBlank { "current_user" }.replace("/", "_").replace(":", "_")
            val targetDir = File(context.filesDir, "profiles").apply { if (!exists()) mkdirs() }
            val targetFile = File(targetDir, "${prefix}_${safeId}.jpg")

            targetFile.writeBytes(bytes)
            "file://${targetFile.absolutePath}"
        } catch (e: Exception) {
            Log.e(TAG, "Errore base64ToLocalFile: ${e.message}")
            base64OrUrl
        }
    }
}
