/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.logic.library

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalLibraryManagerTest {
    private val context = RuntimeEnvironment.getApplication()

    @After
    fun clearState() {
        context.getSharedPreferences("local_library", 0).edit().clear().commit()
    }

    @Test
    fun restoringSelectedRecordKeepsOtherMediaHidden() {
        val manager = LocalLibraryManager(context)
        val first = media("one", "第一项")
        val second = media("two", "第二项")

        manager.hide(listOf(first, second))
        assertEquals(2, manager.state.value.hiddenRecords.size)

        manager.restore(listOf(manager.categoryKey(first)))

        assertFalse(manager.isHidden(first))
        assertTrue(manager.isHidden(second))
        assertEquals(listOf("第二项"), manager.state.value.hiddenRecords.values.map { it.title })
    }

    private fun media(id: String, title: String) = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .build()
}
