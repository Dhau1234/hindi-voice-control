package com.example.hindivoicecontrol

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptions
import com.google.mlkit.vision.text.TextBlocks

class OcrHelper(private val context: Context) {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognizeText(bitmap: Bitmap): List<TextBlocks> {
        val task: Task<TextBlocks> = recognizer.process(bitmap, TextRecognizerOptions.DEFAULT_OPTIONS)
        return Tasks.await(task)
    }
}
