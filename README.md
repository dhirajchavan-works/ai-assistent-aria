# 🤖 Aria – On-Device Voice AI Assistant
### 100% Private | No Cloud | No Data Leaks

---

## 🔒 Privacy Architecture

Every single operation happens **on your Android device**. No data ever leaves your phone:

| Component | Technology | Cloud? |
|-----------|-----------|--------|
| Speech → Text | Android `SpeechRecognizer` (on-device model) | ❌ Never |
| Command Processing | Rule-based NLP engine | ❌ Never |
| Text → Speech | Android `TextToSpeech` | ❌ Never |
| App launching | Android PackageManager | ❌ Never |
| Alarms/Timers | AlarmClock Intent API | ❌ Never |
| Volume/Flashlight | AudioManager / CameraManager | ❌ Never |

---

## 📱 Features

### Voice Commands (say or type):
| Category | Example Commands |
|----------|-----------------|
| ⏰ Alarm | "Set alarm for 7 AM" / "Set alarm for 6:30 PM meeting" |
| ⏱️ Timer | "Set timer for 10 minutes" / "Start 1 hour 30 minute timer" |
| 📞 Calls | "Call Mom" / "Call John" |
| 💬 Messages | "Send message" / "Text Sarah" |
| 🔊 Volume | "Volume up/down" / "Mute" / "Unmute" |
| 🔦 Torch | "Flashlight on/off" / "Torch on" |
| 📱 Apps | "Open WhatsApp" / "Launch Chrome" / "Open Camera" |
| ⚙️ Settings | "Open Settings" / "Open WiFi" / "Open Bluetooth" |
| 🧮 Math | "Calculate 25 times 4" / "What is 100 divided by 5" |
| 📐 Convert | "Convert 100 celsius to fahrenheit" / "50 km in miles" |
| 📅 Time/Date | "What time is it?" / "What day is today?" |
| 🔋 Battery | "Battery level" |
| 😄 Fun | "Tell me a joke" / "Give me a quote" / "Random fact" |

---

## 🛠️ How to Build

### Requirements:
- **Android Studio** Hedgehog or newer (free at developer.android.com)
- **JDK 17** (included with Android Studio)
- **Android SDK** API 26+ (Android 8.0 Oreo)
- **Gradle** 8.2 (auto-downloaded)

### Steps:

```bash
# 1. Open Android Studio
# 2. File → Open → Select the VoiceAI folder
# 3. Wait for Gradle sync to complete (~2 min first time)
# 4. Connect your Android phone with USB debugging ON
# 5. Click Run ▶ or press Shift+F10
```

### Build APK without a device:
```
Build → Build Bundle(s)/APK(s) → Build APK(s)
```
APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📂 Project Structure

```
VoiceAI/
├── app/src/main/
│   ├── AndroidManifest.xml          # Permissions & components
│   ├── java/com/voiceai/assistant/
│   │   ├── ui/
│   │   │   ├── MainActivity.kt      # Main screen, mic handling
│   │   │   ├── MainViewModel.kt     # Chat state management
│   │   │   ├── HistoryActivity.kt   # Command history screen
│   │   │   ├── SettingsActivity.kt  # Settings screen
│   │   │   ├── WaveformView.kt      # Animated audio waveform
│   │   │   └── adapter/
│   │   │       └── ChatAdapter.kt   # Chat bubble RecyclerView
│   │   ├── engine/
│   │   │   ├── CommandProcessor.kt  # 🧠 Main command brain
│   │   │   └── LocalBrain.kt        # NLP fallback classifier
│   │   ├── service/
│   │   │   ├── VoiceListenerService.kt  # Background service
│   │   │   └── BootReceiver.kt      # Auto-start on boot
│   │   └── utils/
│   │       └── WakeWordDetector.kt  # "Hey Aria" detection
│   └── res/
│       ├── layout/                  # UI layouts (XML)
│       ├── drawable/                # Icons & shapes
│       ├── values/                  # Colors, strings, themes
│       └── mipmap-*/               # App icons
```

---

## 🎨 UI Design

- **Dark theme** — deep space navy (#0F0F1A background)
- **Accent color** — purple (#BB86FC) inspired by Material You
- **Chat interface** — WhatsApp-style bubbles, user on right, AI on left
- **Waveform animation** — real-time audio amplitude bars while listening
- **Privacy badge** — always visible "🔒 On-Device" indicator

---

## ⚙️ Technical Details

### Speech Recognition
Uses Android's **on-device speech recognizer** with `EXTRA_PREFER_OFFLINE = true`. This forces the system to use the downloaded language model instead of Google's cloud servers.

> **Note:** On-device speech requires the Google on-device speech model to be downloaded on your phone. Go to: Settings → General management → Language → Text-to-speech → Preferred engine → Settings → Download languages

### Text to Speech
Uses Android's built-in `TextToSpeech` engine. Completely offline. No external TTS service.

### Command Processing
The `CommandProcessor` uses a priority-ordered `when` statement that:
1. Matches intents with multi-keyword patterns
2. Extracts entities (time, contact names, app names) via Regex
3. Executes Android system intents
4. Falls back to `LocalBrain` for unrecognized input

### LocalBrain NLP
A lightweight keyword-scoring classifier that maps unrecognized input to the most likely intent category without any ML model, no internet, no dependency.

---

## 🔐 Permissions Explained

| Permission | Why Needed |
|-----------|-----------|
| `RECORD_AUDIO` | Voice input |
| `CALL_PHONE` | "Call [name]" command |
| `SEND_SMS` | "Send message" command |
| `SET_ALARM` | Alarm commands |
| `CAMERA` | Flashlight control |
| `FOREGROUND_SERVICE` | Background listening |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after reboot |

---

## 🚀 Adding Your Own Commands

Open `CommandProcessor.kt` and add a new branch in the `when` block:

```kotlin
normalized.contains("play music") ->
    CommandResult("🎵 Opening music!") {
        val intent = context.packageManager
            .getLaunchIntentForPackage("com.spotify.music")
        intent?.let { context.startActivity(it) }
    }
```

---

## 📋 Requirements for End Users

- Android 8.0 (API 26) or higher
- Google Text-to-Speech engine (pre-installed on most phones)
- For on-device speech: Google Speech Services downloaded language pack

---

Built with ❤️ in Kotlin | No cloud. No tracking. No compromise.
