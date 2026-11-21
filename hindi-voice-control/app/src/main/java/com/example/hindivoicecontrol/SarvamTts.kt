package com.example.hindivoicecontrol

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SarvamTts(private val context: Context) {

    private val client = OkHttpClient()
    private var mediaPlayer: MediaPlayer? = null

    fun speak(text: String, apiKey: String, onComplete: () -> Unit) {
        // stop previous playback (barge-in support)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        val body = JSONObject().apply {
            put("text", text)
            put("model", "bulbul")
            put("speaker", "pavni")
            put("language_code", "hi-IN")
        }
        val request = Request.Builder()
            .url("https://api.sarvam.ai/tts")
            .post("application/json".toMediaType())
            .header("api-key", apiKey)
            .header("Content-Type", "application/json")
            .body(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                onComplete()
                return@use
            }
            val json = JSONObject(response.body?.string() ?: "{}")
            val audioBase64 = json.optString("audio")
            if (audioBase64.isEmpty()) {
                onComplete()
                return@use
            }
            val audioBytes = Base64.getDecoder().decode(audioBase64)
            val file = File(context.cacheDir, "sarvam_tts_${System.currentTimeMillis()}.mp3")
            file.writeBytes(audioBytes)

            val player = MediaPlayer.create(context, android.net.Uri.fromFile(file))
            mediaPlayer = player
            player?.setOnCompletionListener { onComplete() }
            player?.start()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
    }
}
