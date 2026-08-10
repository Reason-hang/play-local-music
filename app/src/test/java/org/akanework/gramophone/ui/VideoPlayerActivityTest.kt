package org.akanework.gramophone.ui

import android.content.pm.ActivityInfo
import androidx.media3.ui.PlayerView
import org.akanework.gramophone.R
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VideoPlayerActivityTest {
    @Test
    fun videoControlsUseTheRequestedSpeedPresets() {
        assertArrayEquals(
            arrayOf("0.75x", "1x", "1.3x", "1.5x", "1.7x", "2x"),
            RuntimeEnvironment.getApplication()
                .resources
                .getStringArray(R.array.exo_controls_playback_speeds)
        )
    }

    @Test
    fun fullScreenVideoPageHasBackControlAndLandscapeContract() {
        val activity = Robolectric.buildActivity(VideoPlayerActivity::class.java).create().get()

        assertNotNull(activity.findViewById<PlayerView>(R.id.video_player))
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            activity.packageManager.getActivityInfo(activity.componentName, 0).screenOrientation
        )
        activity.findViewById<android.view.View>(R.id.video_back).performClick()
        assertTrue(activity.isFinishing)
    }
}
