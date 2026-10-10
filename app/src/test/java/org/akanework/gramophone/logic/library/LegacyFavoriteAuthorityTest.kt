package org.akanework.gramophone.logic.library

import android.app.Application
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import uk.akane.libphonograph.manipulator.PlaylistSerializer
import uk.akane.libphonograph.manipulator.PlaylistSerializer.Entry
import uk.akane.libphonograph.reader.Reader
import java.io.File

/** Exercises the actual Android 10 Reader authority choice, without mocking the Reader/codec. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class LegacyFavoriteAuthorityTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var provider: PlaylistProvider
    private lateinit var file: File
    private val uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, 7)
    private val a = Entry.ofAbstract(File("/storage/emulated/0/Music/a.mp3"))
    private val b = Entry.ofAbstract(File("/storage/emulated/0/Music/b.mp3"))
    private val c = Entry.ofAbstract(File("/storage/emulated/0/Music/c.mp3"))

    @Before fun registerPlaylistProvider() {
        file = File.createTempFile("legacy-favorite-", ".m3u", context.cacheDir)
        provider = PlaylistProvider(file, listOf("/storage/emulated/0/Music/a.mp3",
            "/storage/emulated/0/Music/b.mp3"))
        ShadowContentResolver.registerProviderInternal("media", provider)
    }

    @After fun removeFixture() {
        file.delete()
    }

    @Test fun newerAbstractMembersAreRetainedWhenAddingFavorite() {
        file.writeBytes(PlaylistSerializer.encode(context, file,
            PlaylistSerializer.Playlist.create().copy(entries = listOf(a))))
        assertTrue(file.setLastModified(1_600_000_000_000L))
        provider.modifiedSeconds = file.lastModified() / 1000 + 60

        val old = Reader.readPlaylist(context, uri)
        assertEntries(listOf(a, b), old.entries)
        assertEquals(1, provider.memberQueries)
        val updated = old.copy(entries = FavoritePlaylistWriter.updatedEntries(old.entries, listOf(c), true))
        val roundTrip = PlaylistSerializer.read(file, PlaylistSerializer.encode(context, file, updated))
        assertEntries(listOf(a, b, c), roundTrip.entries)
    }

    @Test fun pureAbstractPlaylistMembersAreRetainedWhenAddingFavorite() {
        assertTrue(file.delete())
        provider.modifiedSeconds = 1_600_000_060L

        val old = Reader.readPlaylist(context, uri)
        assertEntries(listOf(a, b), old.entries)
        assertEquals(1, provider.memberQueries)
        val updated = FavoritePlaylistWriter.updatedEntries(old.entries, listOf(c), true)
        assertEntries(listOf(a, b, c), updated)
        assertFalse("Reading an abstract playlist must not create a physical file", file.exists())
    }

    @Test fun newerPhysicalFileRemainsAuthoritativeOnAndroid10() {
        file.writeBytes(PlaylistSerializer.encode(context, file,
            PlaylistSerializer.Playlist.create().copy(entries = listOf(a, c))))
        assertTrue(file.setLastModified(1_600_000_000_000L))
        provider.modifiedSeconds = file.lastModified() / 1000 - 60

        assertEntries(listOf(a, c), Reader.readPlaylist(context, uri).entries)
        assertEquals(0, provider.memberQueries)
    }

    private fun assertEntries(expected: List<Entry>, actual: List<Entry>) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (wanted, found) -> assertTrue(wanted.fuzzyEquals(found)) }
    }

    private class PlaylistProvider(private val file: File, private val members: List<String>) : ContentProvider() {
        var modifiedSeconds = 0L
        var memberQueries = 0

        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            val columns = projection ?: arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_MODIFIED)
            return MatrixCursor(columns).apply {
                if (uri.pathSegments.lastOrNull() == "members") {
                    memberQueries++
                    members.forEach { path -> addRow(columns.map { column ->
                        when (column) {
                            MediaStore.MediaColumns.DATA -> path
                            else -> throw AssertionError("Unexpected member column: $column")
                        }
                    }.toTypedArray()) }
                } else {
                    addRow(columns.map { column -> when (column) {
                        MediaStore.MediaColumns.DATA -> file.absolutePath
                        MediaStore.MediaColumns.DATE_MODIFIED -> modifiedSeconds
                        else -> throw AssertionError("Unexpected playlist column: $column")
                    } }.toTypedArray())
                }
            }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = throw AssertionError("Read-only fixture")
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
            throw AssertionError("Read-only fixture")
        override fun update(uri: Uri, values: ContentValues?, selection: String?,
            selectionArgs: Array<out String>?): Int = throw AssertionError("Read-only fixture")
    }
}
