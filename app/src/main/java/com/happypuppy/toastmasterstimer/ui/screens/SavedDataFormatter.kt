package com.happypuppy.toastmasterstimer.ui.screens

import android.content.res.Resources
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.formatTime
import java.util.Date
import java.util.Locale

/** Localized templates and phase labels used when formatting saved speeches for sharing. */
data class SpeakerShareLabels(
    val header: String,
    val footerSingle: String,
    val footerAll: String,
    val nameFormat: String,
    val speechTypeFormat: String,
    val targetRangeFormat: String,
    val durationFormat: String,
    val phaseFormat: String,
    val recordedOnFormat: String,
    val phaseLabels: Map<TimerPhase, String> = emptyMap()
) {
    companion object {
        fun fromResources(resources: Resources): SpeakerShareLabels = SpeakerShareLabels(
            header = resources.getString(R.string.share_header),
            footerSingle = resources.getString(R.string.share_footer_single),
            footerAll = resources.getString(R.string.share_footer_all),
            nameFormat = resources.getString(R.string.share_field_name),
            speechTypeFormat = resources.getString(R.string.share_field_speech_type),
            targetRangeFormat = resources.getString(R.string.share_field_target_range),
            durationFormat = resources.getString(R.string.share_field_duration),
            phaseFormat = resources.getString(R.string.share_field_phase),
            recordedOnFormat = resources.getString(R.string.share_field_recorded_on),
            phaseLabels = TimerPhase.entries.associateWith { phase ->
                resources.getString(phaseCue(phase).labelRes)
            }
        )
    }
}

/**
 * Formats a target range `[minSeconds]..[maxSeconds]` as `"1–2"` when both bounds are positive
 * whole minutes, `"MM:SS–MM:SS"` when either bound has sub-minute precision, or `null` when either
 * bound is missing or invalid.
 */
fun formatTargetRangeMinutes(minSeconds: Int?, maxSeconds: Int?): String? {
    if (minSeconds == null || maxSeconds == null) return null
    if (minSeconds <= 0 || maxSeconds < minSeconds) return null
    return if (minSeconds % 60 == 0 && maxSeconds % 60 == 0) {
        "${minSeconds / 60}\u2013${maxSeconds / 60}"
    } else {
        "${formatTime(minSeconds)}\u2013${formatTime(maxSeconds)}"
    }
}

/** Parses a persisted [TimerPhase] name tolerantly, returning `null` for unknown or blank input. */
fun parseTimerPhase(raw: String?): TimerPhase? {
    val normalized = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return TimerPhase.entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
}

/** Formats one [Speaker] summary block, omitting target range and phase for legacy v2 rows. */
fun formatSpeakerSummary(
    speaker: Speaker,
    formattedDate: String,
    labels: SpeakerShareLabels,
    phaseLabel: String? = null
): String {
    val targetRange = formatTargetRangeMinutes(speaker.minSeconds, speaker.maxSeconds)
    val resolvedPhaseLabel = phaseLabel?.takeIf { it.isNotBlank() }
        ?: parseTimerPhase(speaker.phaseReached)?.let { labels.phaseLabels[it] }
    val locale = Locale.getDefault()

    return buildList {
        add(String.format(locale, labels.nameFormat, speaker.name))
        add(String.format(locale, labels.speechTypeFormat, speaker.speechType))
        if (targetRange != null) {
            add(String.format(locale, labels.targetRangeFormat, targetRange))
        }
        add(String.format(locale, labels.durationFormat, speaker.duration))
        if (resolvedPhaseLabel != null) {
            add(String.format(locale, labels.phaseFormat, resolvedPhaseLabel))
        }
        add(String.format(locale, labels.recordedOnFormat, formattedDate))
    }.joinToString("\n")
}

fun formatSingleSpeakerShareText(
    speaker: Speaker,
    formattedDate: String,
    labels: SpeakerShareLabels,
    phaseLabel: String? = null
): String = buildString {
    appendLine(labels.header)
    appendLine()
    appendLine(formatSpeakerSummary(speaker, formattedDate, labels, phaseLabel))
    appendLine()
    append(labels.footerSingle)
}

fun formatAllSpeakersShareText(
    speakers: List<Speaker>,
    formatDate: (Date) -> String,
    labels: SpeakerShareLabels
): String {
    if (speakers.isEmpty()) return ""
    return speakers.joinToString(
        prefix = "${labels.header}\n\n",
        separator = "\n--------------------------\n",
        postfix = "\n\n${labels.footerAll}"
    ) { speaker ->
        formatSpeakerSummary(speaker, formatDate(speaker.createdAt), labels)
    }
}
