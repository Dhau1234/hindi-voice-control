package com.example.hindivoicecontrol

import android.content.Context

class WhisperAsr(
    private val context: Context,
    private val modelPath: String
) : AsrEngine {

    private var internalOnResult: (String) -> Unit = {}
    private var running = false

    override fun startListening(onResult: (String) -> Unit) {
        internalOnResult = onResult
        if (running) return
        running = true
        // Whisper local model engine start (whisper.cpp based native engine)
        // model file path: modelPath
        // on speech end -> internalOnResult(transcript)
    }

    override fun stopListening() {
        running = false
        // release whisper engine
    }
}
