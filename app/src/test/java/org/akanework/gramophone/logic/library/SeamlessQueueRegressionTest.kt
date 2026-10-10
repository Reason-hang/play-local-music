package org.akanework.gramophone.logic.library

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import org.akanework.gramophone.logic.GramophonePlaybackService
import org.akanework.gramophone.logic.QueueBoard
import org.akanework.gramophone.logic.utils.exoplayer.EndedWorkaroundPlayer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SeamlessQueueRegressionTest {
    private val a = MediaItem.Builder().setMediaId("a").setUri("file:///a.mp3").build()
    private val b = MediaItem.Builder().setMediaId("b").setUri("file:///b.mp3").build()
    private val c = MediaItem.Builder().setMediaId("c").setUri("file:///c.mp3").build()
    private fun scenario(old: List<MediaItem>, oldIndex: Int, updated: List<MediaItem>, selected: Int) {
        val context = RuntimeEnvironment.getApplication()
        val exo = ExoPlayer.Builder(context).build()
        val player = EndedWorkaroundPlayer(context,exo,{ null },QueueBoard(GramophonePlaybackService()))
        try {
            player.setMediaItems(old,oldIndex,1234,"test",false,true,null,false,null,null,null)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(updated[selected].mediaId,player.currentMediaItem!!.mediaId)
            player.setMediaItemsSeamlessly(updated,selected,C.TIME_UNSET,"test",false,true,null,null,null)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(updated.map { it.mediaId },(0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId })
            assertEquals(selected,player.currentMediaItemIndex)
            assertEquals(updated[selected].mediaId,player.currentMediaItem!!.mediaId)
            assertEquals(1234L,player.currentPosition)
            if (selected+1<updated.size) {
                player.seekToNextMediaItem()
                shadowOf(Looper.getMainLooper()).idle()
                assertEquals(updated[selected+1].mediaId,player.currentMediaItem!!.mediaId)
            }
        } finally { player.release() }
    }
    @Test fun favoriteMetadataRefreshKeepsCurrentSongAndPosition() {
        val updated = a.buildUpon().setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle("更新标题").setUserRating(androidx.media3.common.HeartRating(true)).build()).build()
        scenario(listOf(a,b,c),0,listOf(updated,b,c),0)
    }
    @Test fun duplicateMediaOccurrencesKeepSelectedPosition() = scenario(listOf(a,b,c),0,listOf(a,b,a,c),2)
    @Test fun pinnedCurrentSongMovesToFirstWithoutPlayingSecond() = scenario(listOf(b,a,c),1,listOf(a,b,c),0)
    @Test fun clickingCurrentFirstSongDoesNotDuplicateItOrAdvancePlayback() = scenario(listOf(a,b,c),0,listOf(a,b,c),0)
    @Test fun currentLastSongMovesToFirstAndKeepsExpectedNext() = scenario(listOf(b,c,a),2,listOf(a,b,c),0)
    @Test fun currentSongCanMoveFromFirstToMiddle() = scenario(listOf(a,b,c),0,listOf(b,a,c),1)
}
