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

    @Test
    fun pinningPersistsOrderAndUnpinningDoesNotAffectOtherState() {
        val manager = LocalLibraryManager(context)
        val first = media("one", "第一项")
        val second = media("two", "第二项")

        manager.hide(listOf(first))
        manager.pin(listOf(first, second))

        val firstOrder = manager.pinnedOrder(first)
        val secondOrder = manager.pinnedOrder(second)
        assertTrue(manager.isPinned(first))
        assertTrue(manager.isPinned(second))
        assertTrue(firstOrder != null && secondOrder != null && firstOrder < secondOrder)

        manager.unpin(listOf(first))

        assertFalse(manager.isPinned(first))
        assertTrue(manager.isPinned(second))
        assertTrue(manager.isHidden(first))

        val reloaded = LocalLibraryManager(context)
        assertFalse(reloaded.isPinned(first))
        assertEquals(secondOrder, reloaded.pinnedOrder(second))
        assertTrue(reloaded.isHidden(first))
    }

    @Test
    fun oldStateWithoutPinnedFieldRemainsReadable() {
        context.getSharedPreferences("local_library", 0).edit()
            .putString("state_v1", "{\"hidden\":[],\"hiddenRecords\":{},\"categories\":{},\"activeFilter\":\"all\"}")
            .commit()

        val manager = LocalLibraryManager(context)

        assertTrue(manager.state.value.pinned.isEmpty())
        assertEquals(LocalLibraryManager.FILTER_ALL, manager.state.value.activeFilter)
    }

    private fun media(id: String, title: String) = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .build()
}
