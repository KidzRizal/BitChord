package com.music.bitchord.data

/**
 * Short, human-facing descriptions of the stream being decoded, for the player
 * and the mini player. Built only from the measured [NerdStats.Snapshot]: a
 * figure the decoder hasn't reported is left out rather than guessed.
 */
object AudioQualityLabels {

    /** "Dolby Atmos", "Hi-Res Lossless", "Lossless" or "Lossy". */
    fun type(stats: NerdStats.Snapshot): String = when {
        stats.isDolbyAtmos -> "Dolby Atmos"
        stats.isHiRes -> "Hi-Res Lossless"
        stats.isLossless -> "Lossless"
        else -> "Lossy"
    }

    /**
     * The figures behind [type]: "FLAC · 24-bit · 96 kHz" for a lossless
     * stream, "Opus · 160 kbps · 48 kHz" for a lossy one. Bitrate is left off
     * a lossless stream, where it only restates depth x rate x channels.
     */
    fun detail(stats: NerdStats.Snapshot): String? = buildList {
        NerdStats.codecLabel(stats.mimeType)?.let(::add)
        if (stats.isLossless) {
            stats.bitDepth?.takeIf { it > 0 }?.let { add("$it-bit") }
        } else {
            stats.bitrateKbps?.takeIf { it > 0 }?.let { add("$it kbps") }
        }
        stats.sampleRateHz?.takeIf { it > 0 }?.let { add(NerdStats.sampleRateLabel(it)) }
    }.joinToString(" · ").takeIf { it.isNotEmpty() }

    /**
     * One compact line for the mini player: "Hi-Res Lossless · 24-bit/96 kHz",
     * "Lossless · 16-bit/44.1 kHz", "Lossy · Opus · 48 kHz".
     */
    fun compact(stats: NerdStats.Snapshot): String =
        (listOf(type(stats)) + listOfNotNull(figures(stats))).joinToString(" · ")

    /**
     * Just the figures of [compact], without the type: "24-bit/96 kHz" for a
     * lossless stream, "Opus · 48 kHz" for a lossy one. Null when nothing is
     * known yet.
     */
    fun figures(stats: NerdStats.Snapshot): String? {
        val rate = stats.sampleRateHz?.takeIf { it > 0 }?.let(NerdStats::sampleRateLabel)
        val depth = stats.bitDepth?.takeIf { it > 0 }
        val parts = when {
            stats.isLossless -> listOfNotNull(
                when {
                    depth != null && rate != null -> "$depth-bit/$rate"
                    depth != null -> "$depth-bit"
                    else -> rate
                },
            )
            else -> listOfNotNull(NerdStats.codecLabel(stats.mimeType), rate)
        }
        return parts.joinToString(" · ").takeIf { it.isNotEmpty() }
    }
}
