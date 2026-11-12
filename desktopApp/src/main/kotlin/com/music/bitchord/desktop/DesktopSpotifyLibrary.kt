package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.spotify.SpotifyImporter
import com.music.bitchord.data.spotify.SpotifyLibrary
import com.music.bitchord.data.spotify.SpotifyPlaylist
import com.music.bitchord.data.spotify.SpotifyTrack
import com.music.bitchord.ui.icons.BitChordIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * The listener's Spotify library — playlists and Liked Songs — read with the sp_dc session the
 * Canvas setup already stores. Spotify streams nothing here: each track is matched to its
 * YouTube Music counterpart ([SpotifyImporter.matchTrack]) when it is played, as on the phone.
 */
private object SpotifyMatchCache {
    /** Spotify track id -> matched song, or null once a search has come back empty. */
    val matches = mutableStateMapOf<String, Song?>()

    suspend fun resolve(track: SpotifyTrack): Song? {
        if (matches.containsKey(track.id)) return matches[track.id]
        val song = runCatching { SpotifyImporter.matchTrack(track) }.getOrNull()
        withContext(Dispatchers.Main) { matches[track.id] = song }
        return song
    }
}

@Composable
internal fun DesktopSpotifyLibraryPage(
    signedIn: Boolean,
    contentPadding: PaddingValues,
    onConnect: () -> Unit,
    onPlay: (songs: List<Song>, startIndex: Int, title: String) -> Unit,
    onEnqueue: (List<Song>) -> Unit,
) {
    var opened by remember { mutableStateOf<SpotifyPlaylist?>(null) }
    Box(Modifier.fillMaxSize().padding(contentPadding)) {
        when {
            !signedIn -> Column(
                Modifier.fillMaxWidth().padding(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(BitChordIcons.Library, null, tint = DesktopAccent, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(14.dp))
                Text("Connect Spotify", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Paste your sp_dc cookie to see your playlists and Liked Songs. Tracks play from YouTube Music.",
                    color = DesktopSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                SpotifyPill("Connect", BitChordIcons.Library, onConnect)
            }
            opened == null -> SpotifyPlaylistGrid(onOpen = { opened = it }, onConnect = onConnect)
            else -> SpotifyPlaylistTracks(
                playlist = opened!!,
                onBack = { opened = null },
                onPlay = onPlay,
                onEnqueue = onEnqueue,
            )
        }
    }
}

@Composable
private fun SpotifyPlaylistGrid(onOpen: (SpotifyPlaylist) -> Unit, onConnect: () -> Unit) {
    var playlists by remember { mutableStateOf<List<SpotifyPlaylist>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) {
        error = null
        playlists = null
        runCatching { SpotifyLibrary.playlists() }
            .onSuccess { playlists = it }
            .onFailure {
                error = it.message ?: "Could not load your Spotify library"
                DesktopTrackLog.log("spotify: library failed: ${it.message}")
            }
    }
    LazyColumn(
        contentPadding = PaddingValues(start = DesktopPageGutter, end = DesktopPageGutter, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spotify", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { reload++ }) { Icon(Icons.Rounded.Refresh, "Reload", tint = DesktopSecondary) }
            }
            Spacer(Modifier.height(8.dp))
        }
        val list = playlists
        when {
            error != null -> item {
                Column {
                    Text(error!!, color = DesktopSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "If this keeps happening, your sp_dc cookie may have expired.",
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    SpotifyPill("Update sp_dc", BitChordIcons.Library, onConnect)
                }
            }
            list == null -> item { SpotifyLoading() }
            list.isEmpty() -> item { Text("No playlists in your library.", color = DesktopSecondary) }
            else -> items(list, key = { it.id }) { playlist ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onOpen(playlist) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopArtwork(playlist.imageUrl, Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)), px = 160)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        playlist.owner?.let {
                            Text(it, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyPlaylistTracks(
    playlist: SpotifyPlaylist,
    onBack: () -> Unit,
    onPlay: (songs: List<Song>, startIndex: Int, title: String) -> Unit,
    onEnqueue: (List<Song>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var tracks by remember(playlist.id) { mutableStateOf<List<SpotifyTrack>>(emptyList()) }
    var loading by remember(playlist.id) { mutableStateOf(true) }
    var error by remember(playlist.id) { mutableStateOf<String?>(null) }
    var busy by remember(playlist.id) { mutableStateOf<String?>(null) }
    val matches = SpotifyMatchCache.matches

    LaunchedEffect(playlist.id) {
        runCatching { SpotifyLibrary.tracks(playlist.id) { soFar -> tracks = soFar } }
            .onSuccess { tracks = it }
            .onFailure {
                error = it.message ?: "Could not load this playlist"
                DesktopTrackLog.log("spotify: playlist ${playlist.id} failed: ${it.message}")
            }
        loading = false
    }

    /** Matches [from] to YouTube Music, a few at a time, reporting progress in [busy]. */
    suspend fun resolveAll(from: List<SpotifyTrack>): List<Song> = coroutineScope {
        val gate = Semaphore(4)
        var done = 0
        from.map { track ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    SpotifyMatchCache.resolve(track).also {
                        withContext(Dispatchers.Main) {
                            done++
                            busy = "Matching on YouTube Music… $done/${from.size}"
                        }
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

    /** Plays [index] at once, then fills the rest of the playlist in behind it. */
    fun playFrom(index: Int, shuffle: Boolean = false) {
        if (busy != null || tracks.isEmpty()) return
        scope.launch {
            val order = if (shuffle) tracks.shuffled() else tracks
            val start = if (shuffle) 0 else index
            busy = "Finding on YouTube Music…"
            val first = SpotifyMatchCache.resolve(order[start])
            if (first == null) {
                busy = null
                error = "No YouTube Music match for “${order[start].title}”"
                return@launch
            }
            error = null
            onPlay(listOf(first), 0, playlist.name)
            val rest = order.drop(start + 1)
            if (rest.isNotEmpty()) onEnqueue(resolveAll(rest))
            busy = null
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = DesktopPageGutter, end = DesktopPageGutter, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DesktopSecondary) }
                Text("Spotify", color = DesktopSecondary)
            }
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                DesktopArtwork(playlist.imageUrl, Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)), px = 480)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(playlist.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2)
                    val count = if (loading) "${tracks.size} songs…" else "${tracks.size} songs"
                    Text(listOfNotNull(playlist.owner, count).joinToString(" · "), color = DesktopSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SpotifyPill("Play", BitChordIcons.Play) { playFrom(0) }
                        SpotifyPill("Shuffle", BitChordIcons.Shuffle) { playFrom(0, shuffle = true) }
                    }
                }
            }
            busy?.let { Text(it, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall) }
            error?.let { Text(it, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))
        }
        if (tracks.isEmpty() && loading) item { SpotifyLoading() }
        itemsIndexed(tracks, key = { i, t -> "${t.id}:$i" }) { index, track ->
            val unmatched = matches.containsKey(track.id) && matches[track.id] == null
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { playFrom(index) }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", color = DesktopSecondary, modifier = Modifier.width(36.dp), style = MaterialTheme.typography.bodySmall)
                DesktopArtwork(track.imageUrl, Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)), px = 120)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (unmatched) DesktopSecondary else Color.Unspecified,
                    )
                    Text(
                        listOfNotNull(track.artist.ifBlank { null }, track.album).joinToString(" · ") +
                            if (unmatched) "  · not on YouTube Music" else "",
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (track.durationMs > 0) {
                    val s = track.durationMs / 1000
                    Text("%d:%02d".format(s / 60, s % 60), color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SpotifyLoading() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DesktopAccent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SpotifyPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(DesktopAccent).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.Black, fontWeight = FontWeight.SemiBold)
    }
}
