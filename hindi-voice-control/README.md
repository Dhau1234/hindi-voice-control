# Hindi Voice Control 🤖🗣️

Ek Android app jo **Hindi voice commands** se phone ke functions control karta hai — jaise:

- Apps open kare (`YouTube`, `Netflix`, `Spotify`, `Camera`, `Music`, `Chrome`, `WhatsApp`, `Instagram`, `Telegram`…)
- Browser / Internet open kare
- Camera / Selfie open kare
- Alarm set kare (`Alarm`, `घंटी`)
- Light control kare
- Music play kare
- WiFi / Call control kare

## ✨ Features

- **Hindi Speech Recognition** — `hi-IN` language model use hota hai
- **Live Partial Results** — speech khatam hone se pehle user ko feedback milta hai
- **Voice Feedback** — TTS se jawab Hindi mein bolta hai
- **App Launch** — voice se koi bhi app open ho jata hai
- **Alarm** — voice se alarm set hota hai (AlarmManager)
- **WhatsApp message + audio send** — voice se WhatsApp par message / audio / voice note bhej sakta hai
- **Foreground Service** — screen off hone ke baad bhi voice listening chalti rehti hai
- **Conversation Mode** — app baat karta hai: Sunta hai → Samjhta hai → Jawab deta hai → Dobara sunta hai
- **Maya AI Assistant** — AI se baat: `Hey Suji`, `Jai`, `Bai`, `Ask kya hua`
- **Multimodal Inputs** — Image/Document upload + OCR (ML Kit) + Passport parser + Gemini vision
- **ASR (Deepgram + Whisper)** — VAD + streaming
- **TTS (Sarvam AI)**
- **Action & Tool Integration** — Gemini FC + Guardrails
- **Memory (Vector DB)**
- **Barge-in Support** — TTS stop on new speech
- **Autocomplete** — Smart suggestions

## 🗣️ Voice Commands (Hindi + English)

| Command | Action |
|---|---|
| `Camera kholo` / `कैमरा खोलो` | Camera open |
| `Selfie lo` / `फोटो lo` | Camera open |
| `Browser kholo` / `ब्राउजर खोलो` | Browser open |
| `YouTube kholo` / `यूट्यूब खोलो` | YouTube open |
| `Music kholo` / `म्यूजिक खोलो` | Music / YouTube Music open |
| `Netflix kholo` / `नेटफ्लिक्स खोलो` | Netflix open |
| `Alarm 7 baje set karo` / `घंटी 7 बजे` | Alarm set |
| `Light on karo` / `लाइट चालू करो` | Light control |
| `Wifi on karo` / `वाईफाई चालू करो` | WiFi panel open |
| `Call karo` / `कॉल करो` | Dialer open |
| `WhatsApp message bhejo +919876543210 text` | WhatsApp message send |
| `WhatsApp par audio bhejo` / `... /storage/emulated/0/Music/song.mp3 audio bhejo` | WhatsApp par audio send |
| `WhatsApp voice note bhejo` / `व्हाट्सएप वॉइस नोट` | WhatsApp voice note send |
| `Hey Suji` / `Jai` / `Bai` / `Ask kya hua` | **AI Assistant (Maya) se baat** |

## 🛠️ Build & Install

### Prerequisites
- Android Studio (latest)
- Android SDK 34
- Android device / emulator with Android 7.0 (API 24)+

### Steps
1. Android Studio mein **Open** karo aur `hindi-voice-control` folder select karo
2. Gradle sync ho jaye
3. Device se `Run` button dabao

### APK directly build karna

```bash
cd hindi-voice-control
./gradlew.bat assembleRelease   # Windows
./gradlew assembleRelease       # Mac/Linux
```

APK: `app/build/outputs/apk/release/app-release.apk`

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## 📁 Project Structure

```
hindi-voice-control/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/hindivoicecontrol/
        │   ├── MainActivity.kt      # Speech recognizer + command handling
        │   ├── AlarmReceiver.kt     # Alarm broadcast receiver
        │   └── VoiceCommand.kt     # Hindi/English command parser
        └── res/
            ├── layout/activity_main.xml
            ├── drawable/ic_launcher.xml
            └── values/ (strings, colors, themes)
```

## 🔮 Future Improvements

- Continuous listening via Foreground Service
- Smart home integration (MQTT / Home Assistant / BLE bulbs)
- Hindi TTS voice feedback
- Custom wake word (`Hey Hindi`)

---

**Note:** Ye initial version app + speech recognition + alarm par focused hai. Production-grade continuous voice control ke liye Foreground Service aur smart device integrations add ki ja sakti hai.
