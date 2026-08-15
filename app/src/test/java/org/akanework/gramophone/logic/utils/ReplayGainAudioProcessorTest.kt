/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.logic.utils

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplayGainAudioProcessorTest {
    @Test
    fun externalLoudnessUsesFloatPcmCompressionPath() {
        val processor = ReplayGainAudioProcessor()

        processor.setExternalLoudnessEnabled(true)

        assertEquals(
            C.ENCODING_PCM_FLOAT,
            processor.configure(
                AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
            ).encoding
        )
    }

    @Test
    fun externalLoudnessCanBeDisabledBeforePlaybackStarts() {
        val processor = ReplayGainAudioProcessor()

        processor.setExternalLoudnessEnabled(true)
        processor.setExternalLoudnessEnabled(false)

        assertEquals(
            C.ENCODING_PCM_16BIT,
            processor.configure(
                AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
            ).encoding
        )
    }
}
