package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

object SpotifyAuthRepository {

    private const val TAG = "SpotifyAuthRepository"
    private const val CLIENT_ID = "1598958401ae4dc3a5fcbc54e984b544"
    const val REDIRECT_URI = "com.aistudio.music.livefeed://callback"
    private const val SCOPES = "user-read-currently-playing user-read-playback-state"

    private const val PREFS_NAME = "spotify_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_CODE_VERIFIER = "code_verifier"

    private val client = OkHttpClient()
    private var codeVerifier: String? = null

    private var _accessToken: String? = null
    private var _refreshToken: String? = null
    private var _expiresAt: Long = 0L

    val isAuthorized: Boolean get() = _refreshToken != null

    fun loadTokens(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        _refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        _expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        codeVerifier = prefs.getString(KEY_CODE_VERIFIER, null)
    }

    fun launchAuthFlow(context: Context) {
        val verifier = generateCodeVerifier().also { codeVerifier = it }
        val challenge = generateCodeChallenge(verifier)
        // Persiste il verifier in SharedPreferences: sopravvive alla morte del processo
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CODE_VERIFIER, verifier)
            .apply()
        val url = "https://accounts.spotify.com/authorize" +
            "?client_id=$CLIENT_ID" +
            "&response_type=code" +
            "&redirect_uri=${Uri.encode(REDIRECT_URI)}" +
            "&code_challenge_method=S256" +
            "&code_challenge=$challenge" +
            "&scope=${Uri.encode(SCOPES)}"
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    suspend fun handleCallback(context: Context, code: String): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val verifier = codeVerifier ?: prefs.getString(KEY_CODE_VERIFIER, null) ?: return@withContext false
        try {
            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("client_id", CLIENT_ID)
                .add("code_verifier", verifier)
                .build()
            val response = client.newCall(
                Request.Builder().url("https://accounts.spotify.com/api/token").post(body).build()
            ).execute()
            val bodyStr = response.body?.string() ?: return@withContext false
            val json = JSONObject(bodyStr)
            val accessToken = json.optString("access_token").takeIf { it.isNotEmpty() } ?: return@withContext false
            val refreshToken = json.optString("refresh_token").takeIf { it.isNotEmpty() } ?: return@withContext false
            val expiresIn = json.optLong("expires_in", 3600L)
            saveTokens(context, accessToken, refreshToken, System.currentTimeMillis() + expiresIn * 1000)
            // Verifier monouso: rimosso dopo lo scambio
            codeVerifier = null
            prefs.edit().remove(KEY_CODE_VERIFIER).apply()
            Log.d(TAG, "Token Spotify ottenuto con successo")
            true
        } catch (e: Exception) {
            Log.e(TAG, "handleCallback error: ${e.message}")
            false
        }
    }

    suspend fun getValidAccessToken(context: Context): String? {
        if (_accessToken != null && System.currentTimeMillis() < _expiresAt - 60_000) {
            return _accessToken
        }
        return refreshToken(context)
    }

    private suspend fun refreshToken(context: Context): String? = withContext(Dispatchers.IO) {
        val refresh = _refreshToken ?: return@withContext null
        try {
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refresh)
                .add("client_id", CLIENT_ID)
                .build()
            val response = client.newCall(
                Request.Builder().url("https://accounts.spotify.com/api/token").post(body).build()
            ).execute()
            val bodyStr = response.body?.string() ?: return@withContext null
            val json = JSONObject(bodyStr)
            val accessToken = json.optString("access_token").takeIf { it.isNotEmpty() } ?: return@withContext null
            val newRefresh = json.optString("refresh_token").takeIf { it.isNotEmpty() } ?: refresh
            val expiresIn = json.optLong("expires_in", 3600L)
            saveTokens(context, accessToken, newRefresh, System.currentTimeMillis() + expiresIn * 1000)
            accessToken
        } catch (e: Exception) {
            Log.e(TAG, "refreshToken error: ${e.message}")
            null
        }
    }

    fun clearTokens(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        _accessToken = null
        _refreshToken = null
        _expiresAt = 0L
    }

    private fun saveTokens(context: Context, accessToken: String, refreshToken: String, expiresAt: Long) {
        _accessToken = accessToken
        _refreshToken = refreshToken
        _expiresAt = expiresAt
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
