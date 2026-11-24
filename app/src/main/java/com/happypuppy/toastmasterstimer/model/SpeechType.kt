package com.happypuppy.toastmasterstimer.model

/**
 * The kinds of speech the timer can run, and the single source of truth for their preset ranges.
 *
 * [id] is persisted-grade: routes carry it and Wave 4 maps the stored title strings to it, so an id
 * must never change or be reused. [label] is the text saved with a speech today (English until
 * Wave 5 moves it to resources). Presets have a fixed range; [CUSTOM] takes its range from the
 * caller, so its preset range is null.
 */
enum class SpeechType(
    val id: String,
    val label: String,
    val presetMinMinutes: Int?,
    val presetMaxMinutes: Int?
) {
    TABLE_TOPICS("table_topics", "TABLE TOPICS", 1, 2),
    EVALUATION("evaluation", "EVALUATION", 2, 3),
    ICE_BREAKER("ice_breaker", "ICE BREAKER", 4, 6),
    SPEECH("speech", "SPEECH", 5, 7),
    CUSTOM("custom", "Custom Speech", null, null);

    /**
     * The title shown on the timer screen: "TABLE TOPICS (1 TO 2 MIN)" for presets and
     * "Custom Speech (3 - 8 min)" for a custom range. [customLabel] only renames a custom speech.
     */
    fun timerTitle(minMinutes: Int, maxMinutes: Int, customLabel: String? = null): String =
        if (this == CUSTOM) {
            val name = customLabel?.takeIf { it.isNotBlank() } ?: label
            "$name ($minMinutes - $maxMinutes min)"
        } else {
            "$label ($minMinutes TO $maxMinutes MIN)"
        }

    companion object {
        /** The fixed-range types in the order the Main screen lists them. */
        val presets: List<SpeechType> = entries.filter { it != CUSTOM }

        fun fromId(id: String): SpeechType? = entries.firstOrNull { it.id == id }
    }
}
