package org.akanework.gramophone.ui

import android.content.pm.ActivityInfo
import androidx.media3.ui.PlayerView
import org.akanework.gramophone.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VideoPlayerActivityTest {
    @Test
    fun hiddenMedia3SpeedAdapterKeepsItsRequiredSevenEntryContract() {
        val media3SpeedLabels = RuntimeEnvironment.getApplication()
            .resources
            .getStringArray(androidx.media3.ui.R.array.exo_controls_playback_speeds)

        assertEquals(7, media3SpeedLabels.size)
        assertEquals("2x", media3SpeedLabels.last())
    }

    @Test
    fun fullScreenVideoPageHasBackControlAndLandscapeContract() {
        val activity = Robolectric.buildActivity(VideoPlayerActivity::class.java).create().get()

        assertNotNull(activity.findViewById<PlayerView>(R.id.video_player))
        assertNotNull(activity.findViewById<android.view.View>(R.id.video_speed))
        assertNull(activity.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings))
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            activity.packageManager.getActivityInfo(activity.componentName, 0).screenOrientation
        )
        activity.findViewById<android.view.View>(R.id.video_back).performClick()
        assertTrue(activity.isFinishing)
    }
}
