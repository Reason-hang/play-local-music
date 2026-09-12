/*
 *     Copyright (C) 2026 The Gramophone authors
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.logic.artwork

import org.akanework.gramophone.logic.GramophoneAlbumArtProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ArtworkResolverTest {
    @Test
    fun cacheKeyChangesWhenMediaOrFolderConfigurationChanges() {
        val baseline = ArtworkResolver.cacheKey(
            ArtworkResolver.Kind.VIDEO, 42, 1000, 123, 1
        )

        assertNotEquals(baseline, ArtworkResolver.cacheKey(
            ArtworkResolver.Kind.VIDEO, 42, 1001, 123, 1
        ))
        assertNotEquals(baseline, ArtworkResolver.cacheKey(
            ArtworkResolver.Kind.VIDEO, 42, 1000, 124, 1
        ))
        assertNotEquals(baseline, ArtworkResolver.cacheKey(
            ArtworkResolver.Kind.VIDEO, 42, 1000, 123, 2
        ))
    }

    @Test
    fun sidecarNamesFollowExplicitOfflinePriority() {
        assertEquals(
            "albumart.jpg",
            ArtworkResolver.bestSidecarName(listOf("photo.jpg", "folder.jpg", "cover.jpg", "albumart.jpg"))
        )
        assertEquals(
            "folder.png",
            ArtworkResolver.bestSidecarName(listOf("photo.jpg", "folder.png"))
        )
        assertNull(ArtworkResolver.bestSidecarName(listOf("photo.jpg", "notes.txt")))
    }

    @Test
    fun videoArtworkUriKeepsVideoIdentityAndCacheKey() {
        val uri = GramophoneAlbumArtProvider.buildVideoUri(
            7,
            File("/storage/emulated/0/Movies/course.mp4"),
            "artwork:video:7:2048:300:1",
        )

        assertEquals(listOf("video", "7"), uri.pathSegments)
        assertEquals("artwork:video:7:2048:300:1", uri.getQueryParameter("cacheKey"))
    }
}
