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
}
