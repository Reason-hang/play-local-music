/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.logic.library

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import uk.akane.libphonograph.items.EXTRA_FILE
import uk.akane.libphonograph.items.EXTRA_MODIFIED_DATE

@RunWith(RobolectricTestRunner::class)
class VideoResumeProgressStoreTest {
    private val context = RuntimeEnvironment.getApplication()

    @After
    fun clearState() {
        context.getSharedPreferences("video_resume_progress", 0).edit().clear().commit()
    }

    @Test
    fun restoresOnlyMatchingVideoFingerprint() {
        val store = VideoResumeProgressStore(context)
        val original = video("one", 3_600_000, 10)
        val changed = video("one", 3_600_000, 11)

        store.save(original, 123_000)

        assertEquals(123_000L, store.resumePosition(original))
        assertNull(store.resumePosition(changed))
    }

    @Test
    fun finishedVideoStartsFromBeginning() {
        val store = VideoResumeProgressStore(context)
        val item = video("one", 60_000, 10)

        store.save(item, 35_000)
        assertEquals(35_000L, store.resumePosition(item))

        store.save(item, 55_000)
        assertNull(store.resumePosition(item))
    }

    @Test
    fun audioDoesNotCreateVideoResumeRecord() {
        val store = VideoResumeProgressStore(context)
        val item = MediaItem.Builder()
            .setMediaId("audio")
            .setMimeType("audio/mpeg")
            .setMediaMetadata(MediaMetadata.Builder().setDurationMs(60_000).build())
            .build()

        store.save(item, 10_000)

        assertNull(store.resumePosition(item))
    }

    @Test
    fun keepsSeparateProgressForEachVideoAfterStoreIsRecreated() {
        val firstVideo = video("first", 3_600_000, 10)
        val secondVideo = video("second", 3_600_000, 20)

        VideoResumeProgressStore(context).apply {
            save(firstVideo, 755_000)
            save(secondVideo, 200_000)
        }

        val restoredStore = VideoResumeProgressStore(context)
        assertEquals(755_000L, restoredStore.resumePosition(firstVideo))
        assertEquals(200_000L, restoredStore.resumePosition(secondVideo))
    }

    private fun video(id: String, duration: Long, modifiedDate: Long): MediaItem = MediaItem.Builder()
        .setMediaId("MediaStore:$id")
        .setMimeType("video/mp4")
        .setMediaMetadata(MediaMetadata.Builder()
            .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
            .setDurationMs(duration)
            .setExtras(Bundle().apply {
                putString(EXTRA_FILE, "/storage/emulated/0/Movies/$id.mp4")
                putLong(EXTRA_MODIFIED_DATE, modifiedDate)
            })
            .build())
        .build()
}
