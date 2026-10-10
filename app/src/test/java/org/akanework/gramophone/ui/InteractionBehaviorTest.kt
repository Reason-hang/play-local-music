package org.akanework.gramophone.ui

import android.net.Uri
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.library.FavoritePlaylistWriter
import org.akanework.gramophone.ui.components.VideoGesturePlayerView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import uk.akane.libphonograph.manipulator.PlaylistSerializer.Entry
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class InteractionBehaviorTest {
    private class TestPlayer(ready: Boolean, state: Int = Player.STATE_READY) : SimpleBasePlayer(Looper.getMainLooper()) {
        var commands = 0
        private var snapshot = State.Builder()
            .setAvailableCommands(Player.Commands.Builder().add(Player.COMMAND_PLAY_PAUSE).build())
            .setPlaylist(listOf(MediaItemData.Builder("video").setMediaItem(MediaItem.fromUri("file:///video.mp4")).build()))
            .setCurrentMediaItemIndex(0).setPlaybackState(state)
            .setPlayWhenReady(ready, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST).build()
        override fun getState() = snapshot
        override fun handleSetPlayWhenReady(playWhenReady: Boolean): com.google.common.util.concurrent.ListenableFuture<*> {
            commands++
            snapshot = snapshot.buildUpon().setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST).build()
            return Futures.immediateVoidFuture()
        }
    }
    private fun tap(view: View, time: Long, x: Float = 500f, y: Float = 300f) {
        for ((action, offset) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
            val event = MotionEvent.obtain(time,time+offset,action,x,y,0)
            view.dispatchTouchEvent(event); event.recycle()
        }
    }
    private fun video(player: Player): VideoGesturePlayerView {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Gramophone)
        return VideoGesturePlayerView(context, null).apply {
            this.player = player
            measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
            layout(0,0,1000,600)
        }
    }
    @Test fun centralDoubleTapPausesOnceAndNextDoubleTapResumesOnce() {
        val player = TestPlayer(true)
        val view = video(player)
        val time = android.os.SystemClock.uptimeMillis()
        tap(view,time); tap(view,time+120)
        assertFalse(player.playWhenReady); assertEquals(1, player.commands)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        val next = android.os.SystemClock.uptimeMillis()
        tap(view,next); tap(view,next+120)
        assertTrue(player.playWhenReady); assertEquals(2, player.commands)
    }
    @Test fun bufferingDoubleTapPausesPlaybackIntent() {
        val player = TestPlayer(true, Player.STATE_BUFFERING)
        assertFalse(player.isPlaying)
        val view = video(player); val time = android.os.SystemClock.uptimeMillis()
        tap(view,time); tap(view,time+120)
        assertFalse(player.playWhenReady); assertEquals(1, player.commands)
    }
    @Test fun singleTapDoesNotTogglePlayback() {
        val player = TestPlayer(true); val view = video(player)
        view.hideController()
        tap(view,android.os.SystemClock.uptimeMillis())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        assertTrue(player.playWhenReady); assertEquals(0, player.commands)
    }
    @Test fun progressBarInsideCentralRegionRetainsItsOwnTouchHandling() {
        val view = video(TestPlayer(true))
        val bar = androidx.media3.ui.DefaultTimeBar(view.context)
        view.addView(bar,android.widget.FrameLayout.LayoutParams(200,80).apply { leftMargin=400;topMargin=250 })
        view.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY))
        view.layout(0,0,1000,600)
        val event = MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,500f,290f,0)
        assertFalse(view.onInterceptTouchEvent(event))
        event.recycle()
    }
    @Test fun translatedCentralButtonKeepsItsOriginalSingleClickAction() {
        val view = video(TestPlayer(true))
        var clicks = 0
        val button = android.widget.Button(view.context).apply { translationX=100f;setOnClickListener { clicks++ } }
        view.addView(button,android.widget.FrameLayout.LayoutParams(100,80).apply { leftMargin=350;topMargin=250 })
        view.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY))
        view.layout(0,0,1000,600)
        tap(view,android.os.SystemClock.uptimeMillis(),500f,290f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        assertEquals(1,clicks)
    }
    @Test fun favoriteAddIsIdempotentAndRemovePreservesOtherEntries() {
        val a = Entry(listOf(Uri.parse("file:///a.mp3")))
        val b = Entry(listOf(Uri.parse("file:///b.mp4")))
        val first = FavoritePlaylistWriter.updatedEntries(emptyList(), listOf(a,a,b), true)
        assertEquals(listOf(a,b),first)
        assertEquals(first,FavoritePlaylistWriter.updatedEntries(first,listOf(a,b),true))
        assertEquals(listOf(b),FavoritePlaylistWriter.updatedEntries(first,listOf(a),false))
    }
    @Test fun savedStateRetainsActiveAndQueuedFavoriteTargets() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()
        val active = android.os.Bundle().apply { putString("probe","active") }
        MainActivity::class.java.getDeclaredField("activeFavoriteRequest").apply { isAccessible=true }.set(activity,active)
        val queue = MainActivity::class.java.getDeclaredField("favoriteRequests").apply { isAccessible=true }.get(activity) as ArrayDeque<android.os.Bundle>
        queue.addLast(android.os.Bundle().apply { putString("probe","queued") })
        val saved = android.os.Bundle()
        controller.saveInstanceState(saved)
        val requests = androidx.core.os.BundleCompat.getParcelableArrayList(saved,"queuedFavoriteRequests",android.os.Bundle::class.java)!!
        assertEquals(listOf("active","queued"),requests.map { it.getString("probe") })
        controller.destroy()
    }
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun homepageBarIsCollapsedWithoutQueueAndDoesNotDependOnScrolling() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()
        activity.playerBottomSheet.visible = true
        controller.visible()
        shadowOf(Looper.getMainLooper()).idle()
        val sheet = activity.playerBottomSheet
        assertTrue(sheet.actuallyVisible)
        val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
        assertEquals(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
        assertTrue(behavior.peekHeight > 0)
        assertEquals(activity.getString(R.string.empty_player_title),activity.findViewById<android.widget.TextView>(R.id.preview_song_name).text.toString())
        assertFalse(activity.findViewById<View>(R.id.preview_control).isEnabled)
        assertFalse(activity.findViewById<View>(R.id.preview_next).isEnabled)
        assertEquals(0.38f,activity.findViewById<View>(R.id.preview_control).alpha,0.01f)
        sheet.open()
        assertEquals(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
        val root = activity.window.decorView
        val bitmap = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        val directory = java.io.File("/private/tmp/gramophone-179/captures").apply { mkdirs() }
        java.io.File(directory, org.akanework.gramophone.BuildConfig.FLAVOR + "-homepage.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        controller.destroy()
    }
}
