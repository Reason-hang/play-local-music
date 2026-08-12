/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ContextThemeWrapper
import android.widget.TextView
import org.akanework.gramophone.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class LibraryPresentationTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun compactSongCardUsesThreeSmallerTitleLines() {
        val themedContext = ContextThemeWrapper(context, R.style.Theme_Gramophone)
        val card = LayoutInflater.from(themedContext)
            .inflate(R.layout.adapter_song_list_card, null, false)
        val title = card.findViewById<TextView>(R.id.title)
        val artist = card.findViewById<TextView>(R.id.artist)

        assertEquals(3, title.maxLines)
        assertEquals(16f, title.textSize / context.resources.displayMetrics.scaledDensity, 0.01f)
        assertEquals(View.GONE, artist.visibility)
    }

    @Test
    fun songMenuOffersPrivateRemovalInsteadOfDelete() {
        val parser = context.resources.getXml(R.menu.more_menu_song)
        val ids = mutableSetOf<Int>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                ids += parser.getAttributeResourceValue(
                    "http://schemas.android.com/apk/res/android", "id", View.NO_ID
                )
            }
            parser.next()
        }

        assertTrue(ids.contains(R.id.remove_from_library))
        assertFalse(ids.contains(R.id.delete))
    }
}
