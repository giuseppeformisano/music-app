package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Servizio di fallback automatico per il recupero online della copertina ad alta risoluzione
 * tramite iTunes Search API qualora l'immagine non sia fornita dalla notifica o dalla piattaforma.
 */
object CoverResolver {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, String>()
    const val DEFAULT_FALLBACK_COVER = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80"

    suspend fun resolveCoverUrl(artist: String, title: String, currentCoverUrl: String? = null): String {
        val cleanUrl = currentCoverUrl?.trim().orEmpty()
        // Se c'è già una copertina HTTP valida (non vuota e non l'immagine generica di fallback), la usiamo direttamente
        if (cleanUrl.startsWith("http", ignoreCase = true) && !cleanUrl.contains("unsplash.com")) {
            return cleanUrl
        }

        val cleanArtist = artist.trim()
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return cleanUrl.ifBlank { DEFAULT_FALLBACK_COVER }

        val cacheKey = "${cleanArtist.lowercase()}-${cleanTitle.lowercase()}"
        cache[cacheKey]?.let { cachedUrl ->
            if (cachedUrl.isNotBlank()) return cachedUrl
        }

        return withContext(Dispatchers.IO) {
            try {
                val queryTerm = listOf(cleanArtist, cleanTitle).filter { it.isNotBlank() }.joinToString(" ")
                val encodedQuery = URLEncoder.encode(queryTerm, "UTF-8")
                val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=1"

                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                val bodyString = response.body?.string()

                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val json = JSONObject(bodyString)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val artworkUrl = firstResult.optString("artworkUrl100", "")
                        if (artworkUrl.isNotBlank()) {
                            val highResUrl = artworkUrl.replace("100x100bb", "600x600bb")
                            cache[cacheKey] = highResUrl
                            return@withContext highResUrl
                        }
                    }
                }
            } catch (_: Exception) {
                // In caso di errore di rete o timeout, ritorna la copertina corrente o quella di default
            }

            cleanUrl.ifBlank { DEFAULT_FALLBACK_COVER }
        }
    }
}
