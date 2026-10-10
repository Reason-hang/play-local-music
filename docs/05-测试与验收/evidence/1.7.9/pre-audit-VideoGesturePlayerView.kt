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

/** Central taps are confirmed before dispatch, so a double tap cannot also click pause. */
class VideoGesturePlayerView(context: Context, attrs: AttributeSet?) : PlayerView(context, attrs) {
    private var centralGesture = false
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val button = buttonAt(this@VideoGesturePlayerView, e.x, e.y)
            return button?.performClick() ?: performClick()
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            player?.takeIf { it.mediaItemCount > 0 &&
                it.isCommandAvailable(Player.COMMAND_PLAY_PAUSE) }?.playOrPause()
            showController()
            return true
        }
    })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            centralGesture = ev.x in width / 3f..width * 2f / 3f &&
                ev.y in height / 4f..height * 3f / 4f
        }
        return centralGesture || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!centralGesture) return super.onTouchEvent(event)
        gestures.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL) centralGesture = false
        return true
    }

    private fun buttonAt(group: ViewGroup, x: Float, y: Float): View? {
        for (i in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(i)
            if (child.visibility != View.VISIBLE || !child.isEnabled ||
                x < child.left || x >= child.right || y < child.top || y >= child.bottom) continue
            if (child is ViewGroup) buttonAt(child, x-child.left, y-child.top)?.let { return it }
            if (child is Button || child is ImageButton) return child
        }
        return null
    }

    override fun onDetachedFromWindow() {
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        gestures.onTouchEvent(event)
        event.recycle()
        centralGesture = false
        super.onDetachedFromWindow()
    }
}
