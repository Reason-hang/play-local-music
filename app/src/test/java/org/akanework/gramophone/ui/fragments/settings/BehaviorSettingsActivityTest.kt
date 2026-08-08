/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.ui.fragments.settings

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BehaviorSettingsActivityTest {
    @Test
    fun opensWithoutReferencingRemovedAlbumCoverPreference() {
        Robolectric.buildActivity(BehaviorSettingsActivity::class.java)
            .setup()
            .pause()
            .stop()
            .destroy()
    }
}
