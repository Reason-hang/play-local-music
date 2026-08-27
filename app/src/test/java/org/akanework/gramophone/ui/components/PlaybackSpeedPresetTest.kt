/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.ui.components

import org.akanework.gramophone.R
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PlaybackSpeedPresetTest {
    @Test
    fun speedPresetsMatchTheProductRequirement() {
        assertArrayEquals(
            arrayOf("0.75x", "1x", "1.3x", "1.5x", "1.7x", "2x"),
            RuntimeEnvironment.getApplication()
                .resources
                .getStringArray(R.array.playback_speed_presets)
        )
    }

    @Test
    fun productLabelsAndPolicyHaveTheSameOneToOneContract() {
        val labels = RuntimeEnvironment.getApplication()
            .resources
            .getStringArray(R.array.playback_speed_presets)

        assertEquals(PlaybackSpeedPolicy.presets.size, labels.size)
        assertArrayEquals(
            floatArrayOf(0.75f, 1f, 1.3f, 1.5f, 1.7f, 2f),
            PlaybackSpeedPolicy.presets,
            0f
        )
    }
}
