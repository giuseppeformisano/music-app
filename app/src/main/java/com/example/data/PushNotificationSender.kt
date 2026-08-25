package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object PushNotificationSender {
    private const val TAG = "PushNotificationSender"

    // Sostituire con l'URL effettivo del backend deployato su Render (o IP locale per test)
    var renderBackendUrl: String = "https://cross-notify-hub.onrender.com"
    var apiKey: String = "cross-notify-secret-key-2026"
    var appName: String = "music-app"

    private val client by lazy { OkHttpClient() }

    fun sendPushNotification(
        targetToken: String,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ) {
        if (targetToken.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("appName", appName)
                    put("token", targetToken)
                    put("title", title)
                    put("body", body)
                    if (data != null) {
                        val dataJson = JSONObject()
                        data.forEach { (k, v) -> dataJson.put(k, v) }
                        put("data", dataJson)
                    }
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$renderBackendUrl/send-notification")
                    .addHeader("x-api-key", apiKey)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Notifica push inviata con successo via Render: ${response.body?.string()}")
                    } else {
                        Log.e(TAG, "Errore invio push via Render: HTTP ${response.code} - ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione durante l'invio della notifica push: ${e.message}")
            }
        }
    }
}
