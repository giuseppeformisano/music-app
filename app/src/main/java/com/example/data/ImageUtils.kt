package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
     * Corregge automaticamente la rotazione EXIF originale della foto (es. foto scattate in verticale con smartphone).
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

            // Leggi orientamento EXIF originale della foto
            val exif = try {
                ExifInterface(ByteArrayInputStream(bytes))
            } catch (e: Exception) {
                null
            }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
            }

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

            // Applica l'orientamento originale della foto (evita che le foto verticali appaiano ruotate di 90 gradi)
            val orientedBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true).also {
                    if (it != decodedBitmap) decodedBitmap.recycle()
                }
            } else {
                decodedBitmap
            }

            val width = orientedBitmap.width
            val height = orientedBitmap.height
            val maxSide = maxOf(width, height)
            val scale = (maxDimension.toFloat() / maxSide).coerceAtMost(1f)

            val scaledBitmap = if (scale < 1f) {
                val targetW = (width * scale).toInt().coerceAtLeast(1)
                val targetH = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(orientedBitmap, targetW, targetH, true).also {
                    if (it != orientedBitmap) orientedBitmap.recycle()
                }
            } else {
                orientedBitmap
            }

            val safeId = userId.ifBlank { "current_user" }.replace("/", "_").replace(":", "_")
            val targetDir = File(context.filesDir, "profiles").apply { if (!exists()) mkdirs() }
            val targetFile = File(targetDir, "${prefix}_${safeId}_${System.currentTimeMillis()}.jpg")

            FileOutputStream(targetFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
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
            if (bytes.isEmpty()) return fileUriOrPath
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

            // Nome basato sull'hash del contenuto: quando l'immagine cambia cambia anche il
            // path file:// → Coil ricarica (niente vecchia immagine dalla cache). Se il
            // contenuto è identico il path resta stabile e la cache di Coil viene riusata.
            val contentHash = java.security.MessageDigest.getInstance("MD5")
                .digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
                .take(12)
            val targetFile = File(targetDir, "${prefix}_${safeId}_${contentHash}.jpg")

            if (!targetFile.exists()) {
                // Rimuove le versioni precedenti dello stesso avatar/cover per non accumulare file.
                targetDir.listFiles { f -> f.name.startsWith("${prefix}_${safeId}_") && f.name != targetFile.name }
                    ?.forEach { it.delete() }
                targetFile.writeBytes(bytes)
            }
            "file://${targetFile.absolutePath}"
        } catch (e: Exception) {
            Log.e(TAG, "Errore base64ToLocalFile: ${e.message}")
            base64OrUrl
        }
    }

    /**
     * Carica e decodifica l'avatar dell'utente in un oggetto Bitmap (da URL HTTP, file:/// o stringa Base64).
     * Utilizzato per mostrare la foto profilo come icona grande (setLargeIcon) nelle notifiche Android.
     */
    suspend fun loadAvatarBitmap(context: Context, avatarUrl: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (avatarUrl.isNullOrBlank()) return@withContext null
        try {
            if (avatarUrl.startsWith("data:image", ignoreCase = true) ||
                (avatarUrl.length > 200 && !avatarUrl.startsWith("http", ignoreCase = true) && !avatarUrl.startsWith("file:", ignoreCase = true))
            ) {
                val cleanBase64 = if (avatarUrl.contains(",")) avatarUrl.substringAfter(",") else avatarUrl
                val bytes = Base64.decode(cleanBase64.trim(), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else if (avatarUrl.startsWith("file:", ignoreCase = true) || avatarUrl.startsWith("/")) {
                val cleanPath = avatarUrl.removePrefix("file://").removePrefix("file:")
                BitmapFactory.decodeFile(cleanPath)
            } else {
                val loader = ImageLoader(context)
                val request = coil.request.ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                (result.drawable as? BitmapDrawable)?.bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore loadAvatarBitmap: ${e.message}")
            null
        }
    }
}
