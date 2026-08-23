package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer
import java.io.ByteArrayOutputStream

class Base64Fetcher(
    private val data: String,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val base64Clean = if (data.contains(",")) data.substringAfter(",") else data
        val bytes = Base64.decode(base64Clean.trim(), Base64.DEFAULT)
        val buffer = Buffer().write(bytes)
        return SourceResult(
            source = ImageSource(buffer, options.context),
            mimeType = "image/jpeg",
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

    fun processImageUri(context: Context, uriString: String, maxDimension: Int = 512, quality: Int = 80): String {
        if (!uriString.startsWith("content://") && !uriString.startsWith("file://")) {
            return uriString
        }
        return try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver

            // Leggi tutti i byte dello stream in un unico passaggio sicuro
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return uriString
            if (bytes.isEmpty()) return uriString

            // Decodifica prima le sole dimensioni
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return uriString

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

            val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: return uriString

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

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            scaledBitmap.recycle()

            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.e(TAG, "Errore conversione immagine in Base64: ${e.message}")
            uriString
        }
    }
}
