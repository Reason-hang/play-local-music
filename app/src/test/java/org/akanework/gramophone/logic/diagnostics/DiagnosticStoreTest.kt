/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.logic.diagnostics

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DiagnosticStoreTest {
    private val context = RuntimeEnvironment.getApplication()

    @After
    fun clearRecords() {
        DiagnosticStore.clear(context)
    }

    @Test
    fun crashRecordsArePrivateAndRedactLocalPaths() {
        DiagnosticStore.recordCrash(
            context,
            "main",
            IllegalStateException("failed at /storage/emulated/0/Music/private.mp4")
        )

        val crash = DiagnosticStore.crashRecords(context).single()
        val contents = DiagnosticStore.readCrash(crash)

        assertTrue(contents.contains("<redacted-path>"))
        assertFalse(contents.contains("/storage/emulated/0/Music/private.mp4"))
    }

    @Test
    fun copiedSummaryIncludesSanitizedRecentPlaybackEvents() {
        DiagnosticStore.recordEvent(
            context,
            module = "player",
            event = "playback_state",
            details = mapOf(
                "mimeType" to "video/hevc",
                "message" to "failed at /storage/emulated/0/Movies/private.mp4 from content://media/external/video/media/42"
            )
        )

        val summary = DiagnosticStore.copySummary(context)

        assertTrue(summary.contains("playback_state"))
        assertTrue(summary.contains("video/hevc"))
        assertTrue(summary.contains("<redacted-path>"))
        assertTrue(summary.contains("<redacted-uri>"))
        assertFalse(summary.contains("/storage/emulated/0/Movies/private.mp4"))
        assertFalse(summary.contains("content://media/external/video/media/42"))
    }
}
