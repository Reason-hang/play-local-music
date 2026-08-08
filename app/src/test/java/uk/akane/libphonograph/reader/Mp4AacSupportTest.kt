/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package uk.akane.libphonograph.reader

import android.media.MediaFormat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp4AacSupportTest {
    @Test
    fun acceptsMp4WithAacAudioTrack() {
        assertTrue(
            Reader.isSupportedMp4Aac(
                "video/mp4",
                "locked-screen-sample.mp4",
                listOf("video/avc", MediaFormat.MIMETYPE_AUDIO_AAC)
            )
        )
    }

    @Test
    fun identifiesVideoTrackForFullPlayerRendering() {
        assertTrue(Reader.hasVideoTrack(listOf("video/hevc", MediaFormat.MIMETYPE_AUDIO_AAC)))
        assertFalse(Reader.hasVideoTrack(listOf(MediaFormat.MIMETYPE_AUDIO_AAC)))
    }

    @Test
    fun acceptsMp4ExtensionWhenMediaStoreMimeTypeIsMissing() {
        assertTrue(
            Reader.isSupportedMp4Aac(
                null,
                "scanner-fallback.MP4",
                listOf(MediaFormat.MIMETYPE_AUDIO_AAC)
            )
        )
    }

    @Test
    fun rejectsMp4WithoutAacAudioTrack() {
        assertFalse(
            Reader.isSupportedMp4Aac(
                "video/mp4",
                "unsupported-audio.mp4",
                listOf("video/avc", "audio/mpeg")
            )
        )
    }

    @Test
    fun rejectsAacTrackOutsideMp4Container() {
        assertFalse(
            Reader.isSupportedMp4Aac(
                "video/webm",
                "unsupported.webm",
                listOf(MediaFormat.MIMETYPE_AUDIO_AAC)
            )
        )
    }
}
