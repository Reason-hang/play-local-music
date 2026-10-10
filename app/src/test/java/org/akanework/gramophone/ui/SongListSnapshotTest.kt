package org.akanework.gramophone.ui

import androidx.media3.common.MediaItem
import org.akanework.gramophone.ui.adapters.SongListSnapshot
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongListSnapshotTest {
    private val a = media("a", "mp3")
    private val b = media("b", "mp4")
    private val c = media("c", "mp3")
    private fun media(id: String, extension: String) = MediaItem.Builder()
        .setMediaId(id).setUri("file:///music/$id.$extension").build()

    @Test fun boundSongWinsOverStalePositionAfterPinMove() {
        val displayed = listOf(b, a, c)
        // The old path chose displayed[0] (b) even though the bound row was a.
        assertNotEquals(a.mediaId, displayed[0].mediaId)
        val index = SongListSnapshot.clickedIndex(displayed, a, 0)!!
        assertEquals(a.mediaId, displayed[index].mediaId)
        assertEquals(c.mediaId, displayed[index + 1].mediaId)
        assertEquals(listOf("b", "a", "c"), displayed.map { it.mediaId })
    }

    @Test fun everyPinUnpinAndSortOrderSelectsTheClickedIdentity() {
        for (queue in listOf(listOf(a,b,c), listOf(b,a,c), listOf(c,b,a), listOf(b,c,a))) {
            for (clicked in queue) for (stalePosition in -1..queue.size) {
                assertEquals(clicked.mediaId,
                    queue[SongListSnapshot.clickedIndex(queue, clicked, stalePosition)!!].mediaId)
            }
        }
    }

    @Test fun filtersAndRefreshNeverSubstituteAnotherSong() {
        for (filtered in listOf(listOf(a,c), listOf(b), listOf(c,a), emptyList())) {
            for (clicked in listOf(a,b,c)) {
                val index = SongListSnapshot.clickedIndex(filtered, clicked, 0)
                if (clicked in filtered) assertEquals(clicked.mediaId, filtered[index!!].mediaId)
                else assertNull(index)
            }
        }
    }

    @Test fun duplicatePlaylistOccurrenceKeepsItsPosition() {
        assertEquals(2, SongListSnapshot.clickedIndex(listOf(a,b,a,c), a, 2))
    }

    @Test fun pinBoundaryRequiresBothSectionsAndNeverAddsQueueEntries() {
        val songs = listOf(a,b,c)
        assertEquals(2, SongListSnapshot.pinnedBoundary(songs) { it != c })
        assertNull(SongListSnapshot.pinnedBoundary(songs) { true })
        assertNull(SongListSnapshot.pinnedBoundary(songs) { false })
        assertNull(SongListSnapshot.pinnedBoundary(emptyList()) { true })
        assertEquals(3, songs.size)
    }
}
