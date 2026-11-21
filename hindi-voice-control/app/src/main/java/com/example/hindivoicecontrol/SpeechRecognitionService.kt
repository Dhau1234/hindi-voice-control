package com.example.hindivoicecontrol

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.ServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import java.io.File
import java.util.Calendar
import java.util.Locale

class SpeechRecognitionService : Service() {

    private var asrEngine: AsrEngine? = null
    private var ttsPlayer: SarvamTts? = null
    private var isListening = false
    private var lastHeardText: String = ""
    private var pendingAction: String? = null
    private var memory: UserMemory? = null
    private var suggestions: List<String> = emptyList()
    private var listeningHistory: MutableList<String> = mutableListOf()
    private var conversationHistory: MutableList<Pair<String, String>> = mutableListOf()
    private var currentLanguage: String = "hi-IN"

    override fun onCreate() {
        super.onCreate()
        ttsPlayer = SarvamTts(this)
        memory = UserMemory(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startListening()
                startForegroundNotification()
            }
            ACTION_STOP -> {
                stopListening()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopListening()
        ttsPlayer?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun startListening() {
        stopListening()
        asrEngine = DeepgramAsr(this, DEEPGRAM_API_KEY)
        asrEngine?.startListening { transcript -> handleVoiceResult(transcript) }
        isListening = true
    }

    fun startWhisperListening() {
        stopListening()
        val whisperEngine = WhisperAsr(this, "/data/user/0/com.example.hindivoicecontrol/files/whisper_model")
        whisperEngine.startListening { transcript -> handleVoiceResult(transcript) }
        asrEngine = whisperEngine
        isListening = true
    }

    fun stopListening() {
        asrEngine?.stopListening()
        asrEngine = null
        isListening = false
    }

    private fun handleVoiceResult(transcript: String) {
        lastHeardText = transcript
        ttsPlayer?.stop()
        listeningHistory.add(transcript)
        suggestions = SuggestionEngine().getSuggestions(transcript, listeningHistory)
        if (pendingAction != null) {
            if (isAffirmative(transcript)) {
                executePendingAction()
            } else {
                pendingAction = null
                speak("Theek hai, aage badhenge")
            }
            return
        }
        currentLanguage = VoiceCommand.detectLanguage(transcript)
        memory?.remember(this, transcript, FloatArray(0))
        handleCommand(transcript)
    }

    fun handleCommand(rawText: String) {
        val text = rawText.lowercase().trim()
        when {
            VoiceCommand.isWakeWordCommand(text) -> respondToAi("Hey Suji, bata kya hua")
            VoiceCommand.isAiConversationCommand(text) -> respondToAi(text)
            VoiceCommand.isOfflineMapCommand(text) -> handleOfflineMap(text)
            VoiceCommand.isAlarmCommand(text) -> setAlarm(text)
            VoiceCommand.isDocumentCommand(text) -> openDocumentCapture()
            VoiceCommand.isCameraCommand(text) -> openCamera()
            VoiceCommand.isBrowserCommand(text) -> openBrowser()
            VoiceCommand.isLightCommand(text) -> toggleLight()
            VoiceCommand.isMusicCommand(text) -> openMusic()
            VoiceCommand.isWifiCommand(text) -> toggleWifi()
            VoiceCommand.isCallCommand(text) -> openCall()
            VoiceCommand.isWhatsAppCommand(text) -> {
                if (VoiceCommand.isAudioCommand(text)) {
                    sendWhatsAppAudioFromText(text)
                } else {
                    val phone = VoiceCommand.extractWhatsAppPhone(text) ?: ""
                    val message = VoiceCommand.extractWhatsAppMessage(text) ?: ""
                    sendWhatsAppMessage(phone, message)
                }
            }
            VoiceCommand.hasOpenKeyword(text) -> openAppForText(text)
            else -> speak("Kripya phir se bolo")
        }
    }

    fun speak(text: String) {
        ttsPlayer?.speak(text, SARVAM_API_KEY) { startListening() }
    }

    private fun respondToAi(prompt: String) {
        val history = conversationHistory.toList()
        val answer = AiAssistant.askAgent(this, prompt, AiAssistant.API_KEY, null, null, history, currentLanguage)
        if (answer.startsWith("CONFIRMATION_REQUIRED:")) {
            val actionData = answer.substringAfter("CONFIRMATION_REQUIRED:")
            val parts = actionData.split(":", limit = 2)
            pendingAction = parts[0] + "|" + (parts.getOrNull(1) ?: "{}")
            speak("Action confirm karna hai. Bhejna hai kya? Haan ya Nahi")
        } else {
            val modelAnswer = answer
            val updated = conversationHistory.toMutableList()
            updated.add(Pair("user", prompt))
            updated.add(Pair("model", modelAnswer))
            conversationHistory = updated.takeLast(20)
            if (isTravelContext(prompt)) {
                speak("Batao, trip ya visa ke baare mein planning karna hai? Passport ya booking details batao.")
            } else {
                speak(modelAnswer)
            }
        }
    }

    private fun isTravelContext(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("flight") || t.contains("hotel") || t.contains("visa") || t.contains("passport") ||
            t.contains("trip") || t.contains("yatra") || t.contains("airport") || t.contains("booking")
    }

    private fun isAffirmative(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("haan") || t.contains("hua") || t.contains("yes") || t.contains("theek") || t.contains("ok")
    }

    private fun executePendingAction() {
        val pending = pendingAction ?: return
        pendingAction = null
        val parts = pending.split("|")
        val name = parts[0]
        val argsJson = parts.getOrNull(1) ?: "{}"
        val result = AiAssistant.executeTool(this, name, org.json.JSONObject(argsJson))
        speak("Action ho gaya: $result")
    }

    private fun openAppForText(text: String) {
        val pkg = VoiceCommand.findAppPackage(text)
        if (pkg != null) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    this.startActivitySafe(intent)
                    speak("App khol raha hoon")
                } else {
                    speak("App nahi mili")
                }
            } catch (e: android.content.ActivityNotFoundException) {
                speak("App nahi mili")
            }
        } else {
            speak("Samajh nahi aaya kaun sa app kholna hai")
        }
    }

    private fun handleOfflineMap(text: String) {
        when {
            text.contains("download") -> speak("Offline map download kar raha hoon")
            text.contains("open") || text.contains("kholo") -> speak("Offline map open kar raha hoon")
            text.contains("delete") -> speak("Offline map delete kar raha hoon")
            else -> speak("Offline map ke baare mein batao")
        }
    }

    private fun openDocumentCapture() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        this.startActivitySafe(intent)
        speak("Document capture kar raha hoon")
    }

    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        this.startActivitySafe(intent)
        speak("Camera khol raha hoon")
    }

    private fun openBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        this.startActivitySafe(intent)
        speak("Browser khol raha hoon")
    }

    private fun openMusic() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/music")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        this.startActivitySafe(intent)
        speak("Music khol raha hoon")
    }

    private fun openCall() {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        this.startActivitySafe(intent)
        speak("Call khol raha hoon")
    }

    fun sendWhatsAppMessage(phoneNumber: String, message: String) {
        if (phoneNumber.isEmpty()) {
            speak("Koi number nahi mila")
            return
        }
        val uri = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp")
        try {
            this.startActivitySafe(intent)
            speak("Message bhe raha hoon")
        } catch (e: android.content.ActivityNotFoundException) {
            speak("WhatsApp nahi hai")
        }
    }

    fun sendWhatsAppAudio(audioUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            setPackage("com.whatsapp")
            setClipData(android.content.ClipData.newRawUri("audio", audioUri))
        }
        this.startActivitySafe(intent)
        speak("Audio WhatsApp par bhe raha hoon")
    }

    private fun sendWhatsAppAudioFromText(text: String) {
        val audioPath = Regex("/storage/[^\\s]+").find(text)?.value
            ?: Regex("content://[^\\s]+").find(text)?.value
            ?: Regex("/[^\\s]*music/[^\\s]+").find(text)?.value
        if (audioPath == null) {
            speak("Audio file ka path batao")
            return
        }
        val uri = if (audioPath.startsWith("content://")) Uri.parse(audioPath) else Uri.fromFile(File(audioPath))
        sendWhatsAppAudio(uri)
    }

    private fun toggleWifi() {
        val intent = Intent(android.provider.Settings.Panel.ACTION_WIFI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        this.startActivitySafe(intent)
        speak("Wifi control khol raha hoon")
    }

    private fun toggleLight() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("lightcontrol://toggle")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(packageManager) != null) {
            this.startActivitySafe(intent)
            speak("Light on kar raha hoon")
        } else {
            speak("Light control app nahi mila")
        }
    }

    private fun setAlarm(text: String) {
        val numbers = Regex("[0-9]+").findAll(text).map { it.value.toIntOrNull() ?: 0 }.toList()
        val hour = numbers.firstOrNull()?.coerceIn(0, 23) ?: 9
        val minute = if (numbers.size > 1) numbers[1].coerceIn(0, 59) else 0

        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(AlarmReceiver.ACTION).setClass(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(alarmTime.timeInMillis, pendingIntent)

        speak("Alarm set kar raha hoon")
    }

    private fun Context.startActivitySafe(intent: Intent) {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun startForegroundNotification() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Hindi Voice Control")
            .setContentText("Listening…")
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hindi Voice Control",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_START = "com.example.hindivoicecontrol.START_LISTENING"
        const val ACTION_STOP = "com.example.hindivoicecontrol.STOP_LISTENING"

        private const val DEEPGRAM_API_KEY = "bb7c1bb20975396ba6852141a4b430e5c31de129"
        private const val SARVAM_API_KEY = "sk_8k5n12dw_PL93zibcbwrMUvGagwX6QBmw"

        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "hindi_voice_control_service"
    }
}
