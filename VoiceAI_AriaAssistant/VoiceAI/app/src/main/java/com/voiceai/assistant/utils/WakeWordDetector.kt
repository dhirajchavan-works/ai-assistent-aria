package com.voiceai.assistant.utils

/**
 * WakeWordDetector: Checks spoken input for a wake phrase.
 * Used by the service to filter when to actually process a command.
 * 100% on-device.
 */
class WakeWordDetector(private val wakeWord: String = "hey aria") {

    private val variants = listOf(
        wakeWord,
        "aria",
        "hey aria",
        "ok aria",
        "okay aria",
        "hi aria"
    )

    fun containsWakeWord(input: String): Boolean {
        val lower = input.lowercase().trim()
        return variants.any { lower.startsWith(it) || lower.contains(it) }
    }

    fun stripWakeWord(input: String): String {
        var result = input.lowercase().trim()
        for (v in variants) {
            result = result.removePrefix(v).trim()
        }
        return result.replaceFirstChar { it.uppercase() }
    }
}
