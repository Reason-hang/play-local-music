package org.akanework.gramophone.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import org.akanework.gramophone.logic.playOrPause
import org.akanework.gramophone.logic.diagnostics.DiagnosticStore

/** Picture taps are confirmed before dispatch; controls keep their original touch handling. */
class VideoGesturePlayerView(context: Context, attrs: AttributeSet?) : PlayerView(context, attrs) {
    private var playbackGesture = false
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val control = controlAt(this@VideoGesturePlayerView, e.x, e.y)
            return if (control == null) performClick()
                else if (control.id == androidx.media3.ui.R.id.exo_play_pause && control.isEnabled) control.performClick()
                else true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            player?.takeIf { it.mediaItemCount > 0 &&
                it.isCommandAvailable(Player.COMMAND_PLAY_PAUSE) }?.let {
                it.playOrPause()
                DiagnosticStore.recordEvent(context, "video", "double_tap_play_pause",
                    details = mapOf("requestedPlayWhenReady" to it.playWhenReady.toString()))
            } ?: DiagnosticStore.recordEvent(context, "video", "double_tap_unavailable", "WARN",
                details = mapOf("playerBound" to (player != null).toString(),
                    "playPauseCommand" to (player?.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)?.toString() ?: "null")))
            showController()
            return true
        }
    })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            playbackGesture = acceptsPlaybackGesture(ev.x, ev.y)
            if (!playbackGesture) cancelGesture()
        }
        return playbackGesture || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!playbackGesture) return super.onTouchEvent(event)
        gestures.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL) playbackGesture = false
        return true
    }

    private fun controlAt(group: ViewGroup, x: Float, y: Float): View? {
        for (i in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(i)
            val left = child.left + child.translationX - group.scrollX
            val top = child.top + child.translationY - group.scrollY
            if (child.visibility != View.VISIBLE || child.alpha == 0f ||
                x < left || x >= left + child.width || y < top || y >= top + child.height) continue
            if (child is ViewGroup) controlAt(child, x-left, y-top)?.let { return it }
            if (child is Button || child is ImageButton || child is androidx.media3.ui.TimeBar || child.isClickable) return child
        }
        return null
    }

    private fun acceptsPlaybackGesture(x: Float, y: Float): Boolean {
        val control = controlAt(this, x, y)
        return control == null ||
            (control.id == androidx.media3.ui.R.id.exo_play_pause && control.isEnabled)
    }

    private fun cancelGesture() {
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        gestures.onTouchEvent(event)
        event.recycle()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!hasWindowFocus) {
            cancelGesture()
            playbackGesture = false
        }
        super.onWindowFocusChanged(hasWindowFocus)
    }

    override fun onDetachedFromWindow() {
        cancelGesture()
        playbackGesture = false
        super.onDetachedFromWindow()
    }
}
