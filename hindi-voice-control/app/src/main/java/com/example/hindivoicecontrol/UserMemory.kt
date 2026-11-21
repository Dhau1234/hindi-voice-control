package com.example.hindivoicecontrol

import android.content.Context

class UserMemory(context: Context) {

    private data class MemoryEntry(val text: String, val embedding: FloatArray, val timestamp: Long)

    private val prefs = context.getSharedPreferences("hindi_voice_memory", Context.MODE_PRIVATE)
    private val memoryList = mutableListOf<MemoryEntry>()

    fun remember(context: Context, text: String, embedding: FloatArray) {
        memoryList.add(MemoryEntry(text, embedding, System.currentTimeMillis()))
        persist()
    }

    fun search(context: Context, query: String, embedding: FloatArray, topK: Int = 3): List<String> {
        val scored = memoryList.map { m ->
            Triple(cosine(embedding, m.embedding), m.text, m.timestamp)
        }.filter { it.first > 0.0 }.sortedByDescending { it.first }.take(topK)
        return scored.map { it.second }
    }

    fun getAllMemories(context: Context): List<String> = memoryList.map { it.text }

    private fun cosine(a: FloatArray, b: FloatArray): Double {
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val magA = Math.sqrt(na.toDouble())
        val magB = Math.sqrt(nb.toDouble())
        return if (magA == 0.0 || magB == 0.0) 0.0 else dot / (magA * magB)
    }

    private fun persist() {
        val payload = memoryList.joinToString("|") { "${it.timestamp}:${it.text}" }
        prefs.edit().putString("memories", payload).apply()
        // vectors stored alongside in production via real vector DB
    }
}
