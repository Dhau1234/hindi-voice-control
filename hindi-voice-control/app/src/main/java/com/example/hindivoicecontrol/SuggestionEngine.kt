package com.example.hindivoicecontrol

class SuggestionEngine {

    fun getSuggestions(partial: String, history: List<String>): List<String> {
        val p = partial.lowercase().trim()
        val pool = history.distinct() + listOf(
            "camera", "browser", "youtube", "music", "alarm", "light", "wifi",
            "call", "whatsapp", "internet", "selfie", "gallery", "phone"
        )
        val matched = pool.filter { it.startsWith(p) || it.contains(p) }.distinct().take(5)
        return matched.ifEmpty { listOf("Camera kholo", "Browser kholo", "Alarm 7 baje set karo") }
    }
}
