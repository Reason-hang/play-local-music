package org.akanework.gramophone.ui.adapters

import androidx.media3.common.MediaItem

/** Resolve a bound row against one displayed queue, including moves and duplicate playlist entries. */
internal object SongListSnapshot {
    fun clickedIndex(songs: List<MediaItem>, clicked: MediaItem, position: Int): Int? {
        if (songs.getOrNull(position)?.mediaId == clicked.mediaId) return position
        return songs.indexOfFirst { it.mediaId == clicked.mediaId }.takeIf { it >= 0 }
    }

    fun pinnedBoundary(songs: List<MediaItem>, isPinned: (MediaItem) -> Boolean): Int? {
        val firstUnpinned = songs.indexOfFirst { !isPinned(it) }
        return firstUnpinned.takeIf { it > 0 && songs.take(it).any(isPinned) }
    }
}
