package com.music.bitchord.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.AudioQualityLabels
import com.music.bitchord.data.NerdStats

/**
 * The mini player's second line: the artist, then what is actually being
 * decoded — "Lossless · 16-bit/44.1 kHz", "Lossy · Opus · 48 kHz". The
 * quality only shows once the decoder has reported it; [NerdStats.current]
 * is nulled across a track change, so the last track's figures never linger.
 *
 * The artist gives way first: it fades at its end while the quality, being
 * short, keeps its full width.
 */
@Composable
internal fun MiniPlayerArtistAndQuality(
    artist: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val stats by NerdStats.current.collectAsStateWithLifecycle()
    val quality = stats?.let(AudioQualityLabels::compact)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FadingLine(
            text = artist,
            style = style,
            color = color,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (quality != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = quality,
                style = style.copy(
                    fontSize = style.fontSize * 0.82f,
                    fontWeight = FontWeight.Medium,
                ),
                color = color.copy(alpha = color.alpha * 0.85f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
