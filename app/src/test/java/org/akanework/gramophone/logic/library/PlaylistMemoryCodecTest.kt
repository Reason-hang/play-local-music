package org.akanework.gramophone.logic.library

import android.net.Uri
import android.os.ParcelFileDescriptor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import uk.akane.libphonograph.manipulator.PlaylistSerializer
import uk.akane.libphonograph.manipulator.PlaylistSerializer.Entry
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PlaylistMemoryCodecTest {
    private val context = RuntimeEnvironment.getApplication()
    @Test fun allExistingPlaylistFormatsRoundTripInMemoryWithOriginalRelativeBase() {
        val entry = Entry(listOf(Uri.parse("file:///storage/emulated/0/Music/lesson.mp3")))
        for (extension in listOf("m3u", "xspf", "wpl", "pls")) {
            val target = File("/storage/emulated/0/Music/gramophone_favourite.$extension")
            val bytes = PlaylistSerializer.encode(context, target, PlaylistSerializer.Playlist.create().copy(entries=listOf(entry), title="收藏"))
            val parsed = PlaylistSerializer.read(target, bytes)
            assertEquals(extension,1,parsed.entries.size)
            assertTrue(extension,entry.fuzzyEquals(parsed.entries.single()))
        }
    }
    @Test fun repeatedFavoritePreservesFirstPositionAndDeduplicatesOnlySelectedEntry() {
        val a = Entry(listOf(Uri.parse("file:///a.mp3")))
        val b = Entry(listOf(Uri.parse("file:///b.mp3")))
        assertEquals(listOf(a,b),FavoritePlaylistWriter.updatedEntries(listOf(a,b),listOf(a),true))
        assertEquals(listOf(a,b),FavoritePlaylistWriter.updatedEntries(listOf(a,b,a),listOf(a),true))
        assertEquals(listOf(b,b),FavoritePlaylistWriter.updatedEntries(listOf(a,b,b),listOf(a),false))
    }
    @Test fun fileDescriptorCanReadAndWriteWhileHoldingTheSameExclusiveLock() {
        val file = File.createTempFile("favorite-lock-", ".m3u", context.cacheDir)
        file.writeText("before")
        val descriptor = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_WRITE)
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { stream ->
            ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(stream.fd)).use { input ->
            stream.channel.lock().use {
                stream.channel.position(0)
                val buffer = ByteArray(6)
                assertEquals(6,input.read(buffer))
                assertEquals("before",String(buffer))
                stream.channel.position(0)
                stream.channel.write(java.nio.ByteBuffer.wrap("after".toByteArray()))
                stream.channel.truncate(5)
                stream.channel.force(true)
            }
        }
        }
        assertEquals("after",file.readText())
        java.io.RandomAccessFile(file,"rw").use { second ->
            second.channel.tryLock().use { assertNotNull(it) }
        }
        assertTrue(file.delete())
    }
}
