package com.voiceai.assistant.engine

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)

data class CommandResult(
    val displayText: String,
    val spokenText: String? = null,
    val action: (() -> Unit)? = null
)

class CommandProcessor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val brain = LocalBrain()

    fun process(input: String): CommandResult {
        val normalized = input.lowercase(Locale.getDefault()).trim()

        return when {
            // ─── TIME & DATE ───────────────────────────────────────
            matchesAny(normalized, "time", "what time", "current time", "tell me the time") ->
                getTime()

            matchesAny(normalized, "date", "today's date", "what day", "what is today") ->
                getDate()

            matchesAny(normalized, "day", "what day is it") ->
                getDay()

            // ─── ALARMS ───────────────────────────────────────────
            normalized.contains("set alarm") || normalized.contains("wake me") ||
            normalized.contains("alarm at") ->
                setAlarm(normalized)

            normalized.contains("cancel alarm") || normalized.contains("stop alarm") ->
                cancelAlarm()

            // ─── TIMER ────────────────────────────────────────────
            normalized.contains("set timer") || normalized.contains("timer for") ||
            normalized.contains("start timer") ->
                setTimer(normalized)

            // ─── CALLS ────────────────────────────────────────────
            normalized.contains("call ") && !normalized.contains("recall") ->
                makeCall(normalized)

            normalized.contains("redial") || normalized.contains("call back") ->
                redial()

            // ─── MESSAGING ────────────────────────────────────────
            normalized.contains("send") && normalized.contains("message") ||
            normalized.contains("send sms") || normalized.contains("text ") ->
                sendMessage(normalized)

            // ─── VOLUME ───────────────────────────────────────────
            matchesAny(normalized, "volume up", "louder", "increase volume", "turn it up") ->
                adjustVolume(true)

            matchesAny(normalized, "volume down", "quieter", "lower volume", "turn it down") ->
                adjustVolume(false)

            matchesAny(normalized, "mute", "silent mode", "do not disturb", "turn off sound") ->
                setMute(true)

            matchesAny(normalized, "unmute", "sound on", "ring mode", "turn on sound") ->
                setMute(false)

            // ─── FLASHLIGHT ───────────────────────────────────────
            matchesAny(normalized, "flashlight on", "torch on", "turn on flashlight", "turn on torch") ->
                toggleFlashlight(true)

            matchesAny(normalized, "flashlight off", "torch off", "turn off flashlight", "turn off torch") ->
                toggleFlashlight(false)

            // ─── WIFI ─────────────────────────────────────────────
            matchesAny(normalized, "turn on wifi", "enable wifi", "wifi on") ->
                toggleWifi(true)

            matchesAny(normalized, "turn off wifi", "disable wifi", "wifi off") ->
                toggleWifi(false)

            // ─── BLUETOOTH ────────────────────────────────────────
            matchesAny(normalized, "open bluetooth", "bluetooth settings", "turn on bluetooth") ->
                openBluetooth()

            // ─── APPS ─────────────────────────────────────────────
            normalized.contains("open ") || normalized.contains("launch ") ->
                openApp(normalized)

            // ─── SETTINGS ─────────────────────────────────────────
            matchesAny(normalized, "open settings", "go to settings", "settings") ->
                openSettings()

            matchesAny(normalized, "open wifi settings", "wifi settings") ->
                openWifiSettings()

            matchesAny(normalized, "open bluetooth settings") ->
                openBluetooth()

            matchesAny(normalized, "open location", "gps settings", "location settings") ->
                openLocation()

            matchesAny(normalized, "airplane mode", "flight mode") ->
                openAirplaneMode()

            // ─── CALCULATOR ───────────────────────────────────────
            normalized.contains("calculate") || normalized.contains("what is") && containsMath(normalized) ||
            containsMathExpression(normalized) ->
                calculate(normalized)

            // ─── UNIT CONVERSIONS ────────────────────────────────
            normalized.contains("convert") || normalized.contains("in km") ||
            normalized.contains("in miles") || normalized.contains("celsius") ||
            normalized.contains("fahrenheit") ->
                convert(normalized)

            // ─── JOKES ────────────────────────────────────────────
            matchesAny(normalized, "joke", "tell me a joke", "make me laugh", "funny") ->
                tellJoke()

            // ─── QUOTES ───────────────────────────────────────────
            matchesAny(normalized, "quote", "motivate me", "motivational quote", "inspire me") ->
                getQuote()

            // ─── BATTERY ──────────────────────────────────────────
            matchesAny(normalized, "battery", "battery level", "how much battery", "charge level") ->
                getBattery()

            // ─── GREETINGS ────────────────────────────────────────
            matchesAny(normalized, "hello", "hi", "hey aria", "hey", "good morning", "good evening", "good night", "good afternoon") ->
                greet(normalized)

            // ─── HELP ─────────────────────────────────────────────
            matchesAny(normalized, "help", "what can you do", "commands", "capabilities", "what do you know") ->
                showHelp()

            // ─── WEATHER (offline tip) ────────────────────────────
            matchesAny(normalized, "weather", "temperature outside", "will it rain") ->
                weatherInfo()

            // ─── FACTS ────────────────────────────────────────────
            normalized.contains("fact") || normalized.contains("did you know") ->
                getFact()

            // ─── CLEAR / RESTART ──────────────────────────────────
            matchesAny(normalized, "clear", "start over", "reset", "new session") ->
                CommandResult("Chat cleared! Ready for your next command.", action = null)

            // ─── FALLBACK (local NLP brain) ────────────────────────
            else -> brain.respond(input)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // IMPLEMENTATIONS
    // ──────────────────────────────────────────────────────────────────────────

    private fun getTime(): CommandResult {
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        return CommandResult("🕐 It's $time", "The time is $time")
    }

    private fun getDate(): CommandResult {
        val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
        return CommandResult("📅 Today is $date", "Today is $date")
    }

    private fun getDay(): CommandResult {
        val day = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        return CommandResult("📆 Today is $day", "Today is $day")
    }

    private fun setAlarm(input: String): CommandResult {
        val timePattern = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""", RegexOption.IGNORE_CASE)
        val match = timePattern.find(input)
        return if (match != null) {
            var hour = match.groupValues[1].toInt()
            val minute = if (match.groupValues[2].isNotEmpty()) match.groupValues[2].toInt() else 0
            val amPm = match.groupValues[3].lowercase()
            if (amPm == "pm" && hour != 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            val label = extractAlarmLabel(input)
            CommandResult(
                "⏰ Setting alarm for ${formatTime(hour, minute)}${if (label.isNotEmpty()) " — $label" else ""}",
                "Alarm set for ${formatTime(hour, minute)}"
            ) {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label.ifEmpty { "Aria Alarm" })
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } else {
            CommandResult("⏰ Opening alarm clock — please set your time.", action = {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
            })
        }
    }

    private fun cancelAlarm(): CommandResult {
        return CommandResult("Opening alarms — tap to cancel.", action = {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
        })
    }

    private fun setTimer(input: String): CommandResult {
        val pattern = Regex("""(\d+)\s*(hour|hr|minute|min|second|sec)""", RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(input).toList()
        var totalSeconds = 0
        for (m in matches) {
            val num = m.groupValues[1].toInt()
            totalSeconds += when {
                m.groupValues[2].startsWith("h", true) -> num * 3600
                m.groupValues[2].startsWith("m", true) -> num * 60
                else -> num
            }
        }
        return if (totalSeconds > 0) {
            val label = if (totalSeconds >= 3600) "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m"
                        else if (totalSeconds >= 60) "${totalSeconds / 60} minutes"
                        else "$totalSeconds seconds"
            CommandResult("⏱️ Timer set for $label!") {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, totalSeconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "Aria Timer")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } else {
            CommandResult("Opening timer.", action = {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
            })
        }
    }

    private fun makeCall(input: String): CommandResult {
        val name = input.removePrefix("call ").removePrefix("phone ").trim().replaceFirstChar { it.uppercase() }
        return CommandResult("📞 Calling $name…") {
            val uri = Uri.encode(name)
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$uri")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(intent) } catch (e: Exception) {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
            }
        }
    }

    private fun redial(): CommandResult {
        return CommandResult("📞 Opening dialer to redial.") {
            val intent = Intent(Intent.ACTION_DIAL).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
        }
    }

    private fun sendMessage(input: String): CommandResult {
        return CommandResult("💬 Opening messaging app.") {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(intent) } catch (e: Exception) {
                val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
            }
        }
    }

    private fun adjustVolume(increase: Boolean): CommandResult {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val percent = (current * 100) / max
        return CommandResult(
            if (increase) "🔊 Volume up — $percent%" else "🔉 Volume down — $percent%",
            if (increase) "Volume increased to $percent percent" else "Volume decreased to $percent percent"
        )
    }

    private fun setMute(mute: Boolean): CommandResult {
        return if (mute) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            CommandResult("🔕 Phone set to vibrate.", "Phone is now on vibrate")
        } else {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            CommandResult("🔔 Ringer turned on.", "Ring mode enabled")
        }
    }

    private fun toggleFlashlight(on: Boolean): CommandResult {
        return CommandResult(
            if (on) "🔦 Turning on flashlight…" else "🔦 Turning off flashlight…"
        ) {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull()
                if (cameraId != null) cameraManager.setTorchMode(cameraId, on)
            } catch (e: Exception) {
                Log.e("VoiceAI", "Flashlight error: ${e.message}")
            }
        }
    }

    private fun toggleWifi(enable: Boolean): CommandResult {
        return CommandResult(
            if (enable) "📶 Opening Wi-Fi settings to enable." else "📶 Opening Wi-Fi settings to disable."
        ) {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    private fun openBluetooth(): CommandResult {
        return CommandResult("🔵 Opening Bluetooth settings.") {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    private fun openApp(input: String): CommandResult {
        val appName = input.removePrefix("open ").removePrefix("launch ").trim()
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        val found = packages.firstOrNull { pkg ->
            pm.getApplicationLabel(pkg).toString().lowercase().contains(appName.lowercase())
        }
        return if (found != null) {
            val appLabel = pm.getApplicationLabel(found)
            CommandResult("📱 Opening $appLabel…") {
                val intent = pm.getLaunchIntentForPackage(found.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent != null) context.startActivity(intent)
            }
        } else {
            CommandResult("❓ I couldn't find \"$appName\". Check the spelling or try from your app drawer.")
        }
    }

    private fun openSettings(): CommandResult {
        return CommandResult("⚙️ Opening Settings.") {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    private fun openWifiSettings(): CommandResult {
        return CommandResult("📶 Opening Wi-Fi settings.") {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    private fun openLocation(): CommandResult {
        return CommandResult("📍 Opening Location settings.") {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    private fun openAirplaneMode(): CommandResult {
        return CommandResult("✈️ Opening Airplane Mode settings.") {
            context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    private fun calculate(input: String): CommandResult {
        val expr = input
            .replace("calculate", "")
            .replace("what is", "")
            .replace("how much is", "")
            .replace("plus", "+").replace("minus", "-")
            .replace("times", "*").replace("multiplied by", "*")
            .replace("divided by", "/").replace("over", "/")
            .replace("x", "*").trim()
        return try {
            val result = evalMath(expr)
            CommandResult("🧮 $expr = $result", "The answer is $result")
        } catch (e: Exception) {
            CommandResult("🧮 I couldn't compute that. Try: 'calculate 25 times 4'")
        }
    }

    private fun convert(input: String): CommandResult {
        // Temperature
        val celsiusToF = Regex("""(\d+\.?\d*)\s*(?:celsius|°c|c)\s+(?:to\s+)?(?:fahrenheit|°f|f)""", RegexOption.IGNORE_CASE)
        val fToCelsius = Regex("""(\d+\.?\d*)\s*(?:fahrenheit|°f|f)\s+(?:to\s+)?(?:celsius|°c|c)""", RegexOption.IGNORE_CASE)
        val kmToMiles = Regex("""(\d+\.?\d*)\s*(?:km|kilometers?)\s+(?:to\s+)?(?:miles?)""", RegexOption.IGNORE_CASE)
        val milesToKm = Regex("""(\d+\.?\d*)\s*(?:miles?)\s+(?:to\s+)?(?:km|kilometers?)""", RegexOption.IGNORE_CASE)
        val kgToLbs = Regex("""(\d+\.?\d*)\s*(?:kg|kilograms?)\s+(?:to\s+)?(?:lbs?|pounds?)""", RegexOption.IGNORE_CASE)
        val lbsToKg = Regex("""(\d+\.?\d*)\s*(?:lbs?|pounds?)\s+(?:to\s+)?(?:kg|kilograms?)""", RegexOption.IGNORE_CASE)

        return when {
            celsiusToF.containsMatchIn(input) -> {
                val v = celsiusToF.find(input)!!.groupValues[1].toDouble()
                val r = String.format("%.1f", v * 9 / 5 + 32)
                CommandResult("🌡️ $v°C = $r°F")
            }
            fToCelsius.containsMatchIn(input) -> {
                val v = fToCelsius.find(input)!!.groupValues[1].toDouble()
                val r = String.format("%.1f", (v - 32) * 5 / 9)
                CommandResult("🌡️ $v°F = $r°C")
            }
            kmToMiles.containsMatchIn(input) -> {
                val v = kmToMiles.find(input)!!.groupValues[1].toDouble()
                val r = String.format("%.2f", v * 0.621371)
                CommandResult("📏 $v km = $r miles")
            }
            milesToKm.containsMatchIn(input) -> {
                val v = milesToKm.find(input)!!.groupValues[1].toDouble()
                val r = String.format("%.2f", v * 1.60934)
                CommandResult("📏 $v miles = $r km")
            }
            kgToLbs.containsMatchIn(input) -> {
                val v = kgToLbs.find(input)!!.groupValues[1].toDouble()
                val r = String.format("%.2f", v * 2.20462)
                CommandResult("⚖️ $v kg = $r lbs")
            }
            lbsToKg.containsMatchIn(input) -> {
                val v = lbsToKg.find(input)!!.groupValues[1].toDouble()
                val r = String.format("%.2f", v / 2.20462)
                CommandResult("⚖️ $v lbs = $r kg")
            }
            else -> CommandResult("I can convert: Celsius↔Fahrenheit, km↔miles, kg↔lbs. Try: 'convert 100 celsius to fahrenheit'")
        }
    }

    private fun getBattery(): CommandResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = bm.isCharging
        val status = if (charging) "charging ⚡" else if (level < 20) "low ⚠️" else "good"
        return CommandResult("🔋 Battery is at $level% — $status", "Battery level is $level percent, $status")
    }

    private fun greet(input: String): CommandResult {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            input.contains("good morning") || hour in 5..11 -> "Good morning! ☀️"
            input.contains("good afternoon") || hour in 12..16 -> "Good afternoon! 🌤️"
            input.contains("good evening") || hour in 17..20 -> "Good evening! 🌆"
            input.contains("good night") -> "Good night! 🌙 Sleep well."
            else -> "Hello! 👋"
        }
        return CommandResult("$greeting How can I help you today?", greeting)
    }

    private fun tellJoke(): CommandResult {
        val jokes = listOf(
            "Why don't scientists trust atoms? Because they make up everything! 😄",
            "I told my wife she was drawing her eyebrows too high. She looked surprised! 😂",
            "Why did the scarecrow win an award? Because he was outstanding in his field! 🌾",
            "I'm reading a book about anti-gravity. It's impossible to put down! 📚",
            "Why do programmers prefer dark mode? Because light attracts bugs! 🐛",
            "What do you call a fake noodle? An impasta! 🍝",
            "How does a penguin build its house? Igloos it together! 🐧",
            "Why can't you give Elsa a balloon? Because she'll let it go! 🎈"
        )
        val joke = jokes.random()
        return CommandResult(joke)
    }

    private fun getQuote(): CommandResult {
        val quotes = listOf(
            "\"The only way to do great work is to love what you do.\" — Steve Jobs",
            "\"In the middle of every difficulty lies opportunity.\" — Albert Einstein",
            "\"It always seems impossible until it's done.\" — Nelson Mandela",
            "\"The future belongs to those who believe in the beauty of their dreams.\" — Eleanor Roosevelt",
            "\"Success is not final, failure is not fatal: it is the courage to continue that counts.\" — Winston Churchill",
            "\"Believe you can and you're halfway there.\" — Theodore Roosevelt",
            "\"The best time to plant a tree was 20 years ago. The second best time is now.\" — Chinese Proverb",
            "\"Your time is limited, don't waste it living someone else's life.\" — Steve Jobs"
        )
        return CommandResult("💬 ${quotes.random()}")
    }

    private fun getFact(): CommandResult {
        val facts = listOf(
            "🧠 The human brain can store approximately 2.5 petabytes of information!",
            "🌍 One million Earths could fit inside the Sun.",
            "🐬 Dolphins sleep with one eye open.",
            "🍯 Honey never expires — archaeologists found 3,000-year-old honey in Egyptian tombs that was still edible.",
            "🦷 Teeth are the only part of the human body that cannot repair themselves.",
            "🌊 The Pacific Ocean is wider than the Moon.",
            "⚡ Lightning strikes the Earth about 8 million times per day.",
            "🦋 Butterflies taste with their feet."
        )
        return CommandResult(facts.random())
    }

    private fun weatherInfo(): CommandResult {
        return CommandResult(
            "🌤️ I'm fully offline, so I can't fetch live weather.\n\nFor weather, open your Weather app or ask Google Assistant. I keep your data 100% private!",
            "I work offline so I can't check live weather. Try your weather app."
        )
    }

    private fun showHelp(): CommandResult {
        return CommandResult(
            """🤖 What I can do (all on-device!):

⏰ Alarms & Timers
   "Set alarm for 7 AM"
   "Set timer for 5 minutes"

📞 Calls & Messages
   "Call Mom" / "Send message"

🔊 Audio & Device
   "Volume up/down"
   "Mute" / "Unmute"
   "Flashlight on/off"

📱 Apps & Settings
   "Open WhatsApp"
   "Open Settings / WiFi / Bluetooth"

🧮 Math & Conversions
   "Calculate 25 times 4"
   "Convert 100 celsius to fahrenheit"

📅 Time & Date
   "What time is it?"  "What day is today?"

🔋 Device Info
   "Battery level"

😄 Fun
   "Tell me a joke" / "Give me a quote"

All data stays on your phone. No cloud. No leaks.""",
            "Here's what I can do. Say time, alarm, call, open app, calculate, convert, battery, joke, or quote!"
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    private fun matchesAny(input: String, vararg keywords: String): Boolean {
        return keywords.any { input.contains(it) }
    }

    private fun containsMath(input: String): Boolean {
        return Regex("""\d+\s*[\+\-\*\/x]\s*\d+""").containsMatchIn(input)
    }

    private fun containsMathExpression(input: String): Boolean {
        return Regex("""\d+\s*(plus|minus|times|divided|multiplied|\+|\-|\*|\/)\s*\d+""", RegexOption.IGNORE_CASE).containsMatchIn(input)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        return "$h:${minute.toString().padStart(2, '0')} $amPm"
    }

    private fun extractAlarmLabel(input: String): String {
        return when {
            input.contains("for ") -> {
                val after = input.substringAfter("for ").trim()
                if (after.matches(Regex(".*\\d.*"))) "" else after
            }
            else -> ""
        }
    }

    // Simple math evaluator (no external libs)
    private fun evalMath(expr: String): String {
        val cleaned = expr.replace(" ", "")
        // Handle basic operators with left-to-right evaluation
        return try {
            val result = MathEvaluator.evaluate(cleaned)
            if (result == result.toLong().toDouble()) result.toLong().toString()
            else String.format("%.4f", result).trimEnd('0').trimEnd('.')
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot evaluate: $expr")
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Simple recursive descent math evaluator (no eval(), no external lib)
// ──────────────────────────────────────────────────────────────────────────────
object MathEvaluator {
    private var pos = 0
    private var expr = ""

    @Synchronized
    fun evaluate(expression: String): Double {
        pos = 0
        expr = expression.replace(" ", "")
        val result = parseExpression()
        if (pos < expr.length) throw RuntimeException("Unexpected character at position $pos")
        return result
    }

    private fun parseExpression(): Double {
        var result = parseTerm()
        while (pos < expr.length && (expr[pos] == '+' || expr[pos] == '-')) {
            val op = expr[pos++]
            val term = parseTerm()
            result = if (op == '+') result + term else result - term
        }
        return result
    }

    private fun parseTerm(): Double {
        var result = parseFactor()
        while (pos < expr.length && (expr[pos] == '*' || expr[pos] == '/' || expr[pos] == 'x')) {
            val op = expr[pos++]
            val factor = parseFactor()
            result = if (op == '*' || op == 'x') result * factor
                     else if (factor == 0.0) throw ArithmeticException("Division by zero")
                     else result / factor
        }
        return result
    }

    private fun parseFactor(): Double {
        if (pos < expr.length && expr[pos] == '(') {
            pos++ // consume '('
            val result = parseExpression()
            if (pos < expr.length && expr[pos] == ')') pos++
            return result
        }
        if (pos < expr.length && expr[pos] == '-') {
            pos++
            return -parseFactor()
        }
        val start = pos
        while (pos < expr.length && (expr[pos].isDigit() || expr[pos] == '.')) pos++
        if (start == pos) throw RuntimeException("Expected number at position $pos")
        return expr.substring(start, pos).toDouble()
    }
}
