package com.voiceai.assistant.engine

/**
 * LocalBrain: A simple on-device intent classifier using keyword scoring.
 * No ML model needed — pure rule-based NLP. Works 100% offline.
 */
class LocalBrain {

    data class Intent(val name: String, val keywords: List<String>, val weight: Int = 1)

    private val intents = listOf(
        Intent("math", listOf("add", "subtract", "multiply", "divide", "square", "percent", "equals"), 3),
        Intent("time", listOf("time", "clock", "hour", "minute", "when"), 2),
        Intent("reminder", listOf("remind", "don't forget", "remember", "note"), 3),
        Intent("search", listOf("search", "look up", "find", "google", "bing"), 2),
        Intent("joke", listOf("laugh", "funny", "humor", "comic", "pun"), 2),
        Intent("weather", listOf("weather", "temperature", "rain", "snow", "sunny", "cloudy"), 2),
        Intent("music", listOf("music", "play", "song", "spotify", "youtube music"), 2),
        Intent("navigate", listOf("navigate", "directions", "maps", "route", "go to"), 2),
        Intent("define", listOf("define", "meaning", "what does", "definition of"), 2),
        Intent("translate", listOf("translate", "in spanish", "in french", "in hindi", "in arabic"), 2),
        Intent("news", listOf("news", "headlines", "current events", "today's news"), 2),
        Intent("greet", listOf("who are you", "your name", "what are you", "are you ai", "are you real"), 2)
    )

    private val responses = mapOf(
        "math" to "For math, try: 'calculate 25 times 4' or 'what is 100 divided by 5'",
        "time" to "Say 'what time is it' or 'set alarm for 8 AM'",
        "reminder" to "I can set alarms! Say 'set alarm for 6 PM' — full reminder features coming soon.",
        "search" to "I work fully offline, so I can't search the internet. I can open your browser though — just say 'open Chrome'.",
        "weather" to "I'm offline-only, so live weather isn't available. Try saying 'open Weather app'.",
        "music" to "Say 'open Spotify', 'open YouTube Music', or 'open Music app' and I'll launch it for you!",
        "navigate" to "Say 'open Maps' or 'open Google Maps' and I'll launch navigation for you.",
        "define" to "I don't have a dictionary built in yet, but say 'open Chrome' and I'll open your browser for definitions.",
        "translate" to "Say 'open Google Translate' and I'll launch the translate app for you.",
        "news" to "I can't fetch live news as I'm fully offline. Say 'open Chrome' or 'open News app'.",
        "joke" to "Say 'tell me a joke' and I'll crack one for you! 😄",
        "greet" to "I'm Aria, your fully on-device AI assistant! Everything I do happens right here on your phone — zero cloud, zero data leaks. 🔒"
    )

    fun respond(input: String): CommandResult {
        val lower = input.lowercase()

        // Score each intent
        val scores = intents.map { intent ->
            val score = intent.keywords.sumOf { kw ->
                if (lower.contains(kw)) intent.weight else 0
            }
            intent.name to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        return if (scores.isNotEmpty()) {
            val topIntent = scores.first().first
            val response = responses[topIntent] ?: "I heard you say: \"$input\" — try saying 'help' to see what I can do."
            CommandResult(response)
        } else {
            // Completely unknown — give a helpful fallback
            CommandResult(getFallback(input))
        }
    }

    private fun getFallback(input: String): String {
        val starters = listOf(
            "Hmm, I didn't quite catch that command. Try saying 'help' to see what I can do! 🤔",
            "I heard \"$input\" but I'm not sure what to do with it. Say 'help' for a list of commands.",
            "I'm still learning! Say 'help' to discover my capabilities.",
            "Not sure about that one. But I can set alarms, make calls, open apps, do math, and much more! Try 'help'."
        )
        return starters.random()
    }
}
