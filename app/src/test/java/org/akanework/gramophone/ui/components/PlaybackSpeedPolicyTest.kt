package org.akanework.gramophone.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSpeedPolicyTest {
    @Test
    fun keepsEverySupportedProductSpeedIncludingTwoX() {
        PlaybackSpeedPolicy.presets.forEachIndexed { index, speed ->
            assertEquals(index, PlaybackSpeedPolicy.presetIndex(speed))
            assertEquals(speed, PlaybackSpeedPolicy.nearestPreset(speed), 0f)
        }
        assertNull(PlaybackSpeedPolicy.presetIndex(1.25f))
    }

    @Test
    fun normalizesLegacySpeedToNearestProductPreset() {
        assertEquals(1.3f, PlaybackSpeedPolicy.nearestPreset(1.25f), 0f)
        assertEquals(1.7f, PlaybackSpeedPolicy.nearestPreset(1.8f), 0f)
    }

    @Test
    fun unlockedSpeedKeepsPitchButLockedSpeedTracksTempo() {
        val unlocked = PlaybackSpeedPolicy.parameters(2f, 1.25f, locked = false)
        assertEquals(2f, unlocked.speed, 0f)
        assertEquals(1.25f, unlocked.pitch, 0f)

        val locked = PlaybackSpeedPolicy.parameters(2f, 1.25f, locked = true)
        assertEquals(2f, locked.speed, 0f)
        assertEquals(2f, locked.pitch, 0f)
    }

    @Test
    fun clampsOnlyPitchAndNeverEmitsUnsupportedSpeed() {
        val result = PlaybackSpeedPolicy.parameters(1.25f, 9f, locked = false)
        assertEquals(1.3f, result.speed, 0f)
        assertEquals(4f, result.pitch, 0f)
    }
}
