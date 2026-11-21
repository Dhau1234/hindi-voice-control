package com.example.hindivoicecontrol

object VoiceCommand {

    private val appPackages: Map<String, String> = mapOf(
        "youtube" to "com.google.android.youtube",
        "यूट्यूब" to "com.google.android.youtube",
        "music" to "com.google.android.youtube.music",
        "म्यूजिक" to "com.google.android.youtube.music",
        "netflix" to "com.netflix.media",
        "नेटफ्लिक्स" to "com.netflix.media",
        "spotify" to "com.spotify.music",
        "स्पॉटिफ़ाई" to "com.spotify.music",
        "instagram" to "com.instagram.android",
        "इंस्टाग्राम" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "फेसबुक" to "com.facebook.katana",
        "whatsapp" to "com.whatsapp",
        "व्हाट्सएप" to "com.whatsapp",
        "telegram" to "org.telegram.messenger",
        "टेलीग्राम" to "org.telegram.messenger",
        "chrome" to "com.android.chrome",
        "कंचर" to "com.android.chrome"
    )

    fun hasOpenKeyword(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("open") || t.contains("kholo") || t.contains("khol") ||
            t.contains("खोल") || t.contains("खोलो") || t.contains("को")
    }

    fun isAlarmCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("alarm") || t.contains("alert") || t.contains("wake") ||
            t.contains("घंटी") || t.contains("जाग") || t.contains("नेज")
    }

    fun isCameraCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("camera") || t.contains("कैमरा") || t.contains("selfie") ||
            t.contains("फोटो")
    }

    fun isBrowserCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("browser") || t.contains("ब्राउजर") || t.contains("नेट") ||
            t.contains("internet") || t.contains("इंटरनेट") || t.contains("गूगल")
    }

    fun isLightCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("light") || t.contains("लाइट") || t.contains("lamps") ||
            t.contains("एंट") || t.contains("led")
    }

    fun isMusicCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("music") || t.contains("song") || t.contains("म्यूजिक") ||
            t.contains("गाना") || t.contains("play")
    }

    fun isWifiCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("wifi") || t.contains("वाईफाई") || t.contains("wifi")
    }

    fun isDocumentCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("passport") || t.contains("document") || t.contains("id card") ||
            t.contains("aadhaar") || t.contains("photo") || t.contains("image") || t.contains("scan")
    }

    fun isOfflineMapCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("offline") || t.contains("map") || t.contains("osm")
    }

    fun isCallCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("call") || t.contains("कॉल") || t.contains("phone")
    }

    fun findAppPackage(text: String): String? {
        val words = text.lowercase().split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        for (word in words) {
            appPackages[word]?.let { return it }
        }
        return null
    }

    fun isWhatsAppCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("whatsapp") || t.contains("भाईसाफ") || t.contains("व्हाट्सएप") ||
            t.contains("बायसबाई")
    }

    fun isWakeWordCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("hey suji") || t.contains("suji") || t.contains("hey maya") || t.contains("माया")
    }

    fun isAiConversationCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("jai") || t.contains("bai") || t.contains("ask") ||
            t.contains("chat") || t.contains("ai")
    }

    fun isAudioCommand(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("audio") || t.contains("sound") || t.contains("साउंड") ||
            t.contains("ऑडियो") || t.contains("वॉइस नोट") || t.contains("वॉइसनोट") || t.contains("voice")
    }

    fun extractWhatsAppPhone(text: String): String? {
        val digits = Regex("[0-9]{10,12}").findAll(text).map { it.value }.toList()
        return digits.firstOrNull()?.let { "+${it}" }
    }

    fun extractWhatsAppMessage(text: String): String? {
        val cleaned = text.lowercase()
            .replace(Regex("\\bwhatsapp\\b"), " ")
            .replace(Regex("\\bwa\\b"), " ")
            .replace(Regex("\\bमैसेज\\b"), " ")
            .replace(Regex("\\bनमसेज\\b"), " ")
            .replace(Regex("\\bभाईसाफ\\b"), " ")
            .replace(Regex("\\bव्हाट्सएप\\b"), " ")
            .replace(Regex("\\bनमस्कार\\b"), " ")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .trim()
        return cleaned.ifEmpty { null }
    }

    fun detectLanguage(text: String): String {
        val t = text.trim()
        return if (t.contains(Regex("[\\u0900-\\u097F]"))) "hi-IN" else "en-US"
    }
}
