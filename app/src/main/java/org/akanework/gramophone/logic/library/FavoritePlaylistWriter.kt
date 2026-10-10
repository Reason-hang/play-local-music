package org.akanework.gramophone.logic.library

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import org.akanework.gramophone.logic.gramophoneApplication
import org.nift4.mediastorecompat.MediaStoreCompat
import uk.akane.libphonograph.manipulator.ItemManipulator
import uk.akane.libphonograph.manipulator.PlaylistSerializer
import uk.akane.libphonograph.manipulator.PlaylistSerializer.Entry
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

/** Shared activity/service writer. File locking additionally serializes cooperating apps. */
object FavoritePlaylistWriter {
    private val mutex = Mutex()
    private val createdUris = mutableMapOf<String, Uri>()

    fun updatedEntries(existing: List<Entry>, selected: List<Entry>, favorite: Boolean): List<Entry> {
        if (!favorite) return existing.filterNot { entry -> selected.any { it.fuzzyEquals(entry) } }
        val result = existing.toMutableList()
        selected.forEach { item ->
            var found = false
            val iterator = result.listIterator()
            while (iterator.hasNext()) {
                if (item.fuzzyEquals(iterator.next())) {
                    if (found) iterator.remove() else found = true
                }
            }
            if (!found) result.add(item)
        }
        return result
    }

    suspend fun update(context: Context, requestedUri: Uri?, songs: List<Entry>, favorite: Boolean) =
        mutex.withLock {
            val known = requestedUri ?: createdUris[context.packageName]
            val uri = known ?: ItemManipulator.createPlaylist(context,
                ItemManipulator.getDefaultPlaylistFile(ItemManipulator.FAVORITES))
            try {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    // Legacy MediaStore members can be newer than, or exist without, the file.
                    // Keep the reader's authority selection and abstract-playlist conversion.
                    val old = ItemManipulator.readbackPlaylist(context, uri)
                    val updated = old.copy(entries = updatedEntries(old.entries, songs, favorite))
                    ItemManipulator.setPlaylistContent(context, uri, updated, known == null)
                    createdUris[context.packageName] = uri
                    return@withLock
                }
                val file = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA),
                    null, null, null).use { cursor ->
                    require(cursor != null && cursor.moveToFirst()) { "Favorite playlist unavailable" }
                    File(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)))
                }
                val pathMap = context.gramophoneApplication.reader.pathMapFlow.first()
                val descriptor = requireNotNull(MediaStoreCompat.openFileDescriptor(context, uri, "rw"))
                ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { stream ->
                    ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(stream.fd)).use { input ->
                        stream.channel.lock().use {
                            // Neither descriptor closes until the lock is released. Closing any descriptor
                            // for this inode can release POSIX locks. Parsing uses memory streams only.
                            stream.channel.position(0)
                            val bytes = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count <= 0) break
                                require(bytes.size() + count < 5 * 1024 * 1024) { "Favorite playlist too large" }
                                bytes.write(buffer, 0, count)
                            }
                            val original = bytes.toByteArray()
                            val old = if (original.isEmpty()) PlaylistSerializer.Playlist.create()
                                else PlaylistSerializer.read(file, original)
                            val resolved = old.entries.map { it.updateFromMediaItem(pathMap) }
                            val updated = old.copy(entries = updatedEntries(resolved, songs, favorite))
                            val encoded = PlaylistSerializer.encode(context, file, updated)
                            require(encoded.size < 5 * 1024 * 1024) { "Favorite playlist too large" }
                            try {
                                replaceBytes(stream, encoded)
                            } catch (error: Exception) {
                                try { replaceBytes(stream, original) }
                                catch (restore: Exception) { error.addSuppressed(restore) }
                                throw error
                            }
                    }
                    }
                }
                if (known == null) MediaStoreCompat.finishCreate(context, uri, file)
                else MediaStoreCompat.scanFile(context, uri, file)
                createdUris[context.packageName] = uri
            } catch (error: Exception) {
                createdUris.remove(context.packageName)
                if (known == null) {
                    // Only clean up the file created by this failed operation, never an old list.
                    try { context.contentResolver.delete(uri, null, null) }
                    catch (cleanup: Exception) { error.addSuppressed(cleanup) }
                }
                throw error
            }
        }

    private fun replaceBytes(stream: java.io.FileOutputStream, bytes: ByteArray) {
        stream.channel.position(0)
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) stream.channel.write(buffer)
        stream.channel.truncate(bytes.size.toLong())
        stream.channel.force(true)
    }
}
