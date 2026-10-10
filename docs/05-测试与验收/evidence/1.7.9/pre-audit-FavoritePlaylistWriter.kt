package org.akanework.gramophone.logic.library

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.akane.libphonograph.manipulator.ItemManipulator
import uk.akane.libphonograph.manipulator.PlaylistSerializer
import uk.akane.libphonograph.manipulator.PlaylistSerializer.Entry

/** One process-wide writer shared by the activity and media notification controls. */
object FavoritePlaylistWriter {
    private val mutex = Mutex()
    private val createdUris = mutableMapOf<String, Uri>()

    fun updatedEntries(existing: List<Entry>, selected: List<Entry>, favorite: Boolean): List<Entry> {
        val result = existing.filterNot { entry -> selected.any { it.fuzzyEquals(entry) } }
        if (!favorite) return result
        return result + selected.fold(emptyList<Entry>()) { entries, entry ->
            if (entries.any { it.fuzzyEquals(entry) }) entries else entries + entry
        }
    }

    suspend fun update(context: Context, requestedUri: Uri?, songs: List<Entry>, favorite: Boolean) =
        mutex.withLock {
            val known = requestedUri ?: createdUris[context.packageName]
            val uri = known ?: ItemManipulator.createPlaylist(context,
                ItemManipulator.getDefaultPlaylistFile(ItemManipulator.FAVORITES))
            createdUris[context.packageName] = uri
            val readback = if (known != null) ItemManipulator.readbackPlaylist(context, uri)
                else PlaylistSerializer.Playlist.create()
            ItemManipulator.setPlaylistContent(context, uri,
                readback.copy(entries = updatedEntries(readback.entries, songs, favorite)), known == null)
        }
}
