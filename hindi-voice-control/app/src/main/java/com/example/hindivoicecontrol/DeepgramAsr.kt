package com.example.hindivoicecontrol

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.ByteArrayOutputStream
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class DeepgramAsr(
    private val context: Context,
    private val apiKey: String
) : AsrEngine {

    private var internalOnResult: (String) -> Unit = {}
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private var isRunning = false
    private var isSpeech = false
    private var silenceFrames = 0
    private val speechBuffer = ByteArrayOutputStream()
    private val frameSize = 320
    private val rmsThreshold = 450.0

    override fun startListening(onResult: (String) -> Unit) {
        internalOnResult = onResult
        start()
    }

    override fun stopListening() {
        stop()
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        isSpeech = false
        silenceFrames = 0
        speechBuffer.clear()

        val url = "wss://api.deepgram.com/v1/listen?model=nova&language=hi&sample_rate=8000"
        val request = okhttp3.Request.Builder().url(url).build()
        val listener = object : WebSocketListener {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                this@DeepgramAsr.webSocket = webSocket
                startMicrophoneCapture()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = org.json.JSONObject(text)
                    if (json.optString("type") == "UtteranceEnd") {
                        val transcript = json
                            .getJSONObject("results")
                            .getJSONObject("channels")
                            .getJSONArray("alternatives")
                            .getJSONObject(0)
                            .getString("transcript")
                        stopInternal()
                        internalOnResult(transcript)
                    }
                } catch (e: Exception) {
                    Log.e("DeepgramAsr", "Message parse error: ${e.message}")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { }

            override fun onClose(webSocket: WebSocket, code: Int, reason: String) {
                stopMicrophoneCapture()
                isRunning = false
            }
        }
        client.newWebSocket(request, listener)
    }

    fun stop() {
        isRunning = false
        captureThread?.interrupt()
        try { audioRecord?.stop() } catch (e: Exception) { }
        audioRecord = null
        try { webSocket?.close(1000, "stopped") } catch (e: Exception) { }
    }

    private fun startMicrophoneCapture() {
        try {
            val minBuf = AudioRecord.getMinBufferSize(
                16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 16000, AudioRecord.MICROPHONE
            )
            val record = AudioRecord.Builder(context, MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                .setBufferSizeInBytes(minBuf)
                .build()
            audioRecord = record
            captureThread = Thread { captureLoop(record) }.apply { name = "DeepgramMic"; start() }
        } catch (e: Exception) { }
    }

    private fun captureLoop(record: AudioRecord) {
        val buffer = ByteArray(32000)
        record.startRecording()
        while (isRunning) {
            val bytesRead = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
            if (bytesRead > 0) {
                var offset = 0
                while (offset < bytesRead) {
                    val frameLen = minOf(frameSize, bytesRead - offset)
                    if (frameLen > 0) {
                        val frame = ByteArray(frameLen)
                        System.arraycopy(buffer, offset, frame, 0, frameLen)
                        val rms = computeRms(frame)
                        if (rms > rmsThreshold) {
                            isSpeech = true
                            silenceFrames = 0
                            speechBuffer.write(frame, 0, frameLen)
                        } else {
                            if (isSpeech) {
                                silenceFrames++
                                if (silenceFrames >= 30) {
                                    endUtterance()
                                }
                            }
                        }
                    }
                    offset += frameLen
                }
            }
        }
        try { record.stop() } catch (e: Exception) { }
    }

    private fun computeRms(data: ByteArray): Double {
        var sum = 0.0
        var count = 0
        var i = 0
        while (i + 1 < data.size) {
            val s = (data[i].toInt() and 0xFF) or ((data[i + 1].toInt() and 0xFF) shl 8)
            val sample = if (s >= 0x8000) s - 0x10000 else s
            sum += sample * sample.toDouble()
            count++
            i += 2
        }
        return if (count == 0) 0.0 else Math.sqrt(sum / count)
    }

    private fun endUtterance() {
        isSpeech = false
        silenceFrames = 0
        speechBuffer.reset()
        try {
            webSocket?.send(okio.ByteString("{\"type\":\"Finalize\"}".toByteArray(Charsets.UTF_8)))
        } catch (e: Exception) { }
    }

    private fun stopInternal() {
        isRunning = false
        captureThread?.interrupt()
        try { audioRecord?.stop() } catch (e: Exception) { }
        audioRecord = null
        try { webSocket?.close(1000, "speech-end") } catch (e: Exception) { }
    }
}
