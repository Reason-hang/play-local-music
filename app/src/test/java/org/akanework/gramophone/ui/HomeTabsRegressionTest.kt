package org.akanework.gramophone.ui

import androidx.preference.PreferenceManager
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter.Companion.Tab
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter.Companion.mapSettingToTabList
import org.akanework.gramophone.ui.components.TabOrderPreference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HomeTabsRegressionTest {
    @Test fun defaultHomepageKeepsFourDistinctMediaEntrances() {
        assertEquals(listOf(Tab.Songs,Tab.Artists,Tab.Folders,Tab.Playlist,null),mapSettingToTabList(""))
    }
    @Test fun legacyTabOrderPreservesVisibleAndHiddenChoicesWhileRemovingFilesystem() {
        assertEquals(listOf(Tab.Folders,Tab.Songs,null,Tab.Artists,Tab.Playlist),
            mapSettingToTabList("Folders,FileSystem,Songs,,Artists,Playlist"))
        assertFalse(TabOrderPreference.TabOrderAdapter("FileSystem,Songs,Folders,Artists,Playlist,").value.contains(Tab.FileSystem))
    }
    @Test fun legacyFilesystemOnlyHomepageFallsBackToSongsInsteadOfEmptyPage() {
        val tabs = mapSettingToTabList("FileSystem,,Songs,Artists,Folders,Playlist")
        assertEquals(Tab.Songs,tabs.first())
        assertEquals(1,tabs.indexOf(null))
        assertFalse(tabs.contains(Tab.FileSystem))
    }
    @Test fun tabOrderSettingsRenderFourLabelsWithoutRewritingLegacyPreference() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val original = prefs.getString("tabs",null)
        val legacy = "Songs,Artists,Folders,FileSystem,Playlist,"
        prefs.edit().putString("tabs",legacy).commit()
        try {
            val themed = android.view.ContextThemeWrapper(context,R.style.Theme_Gramophone)
            val parent = android.widget.FrameLayout(themed)
            val adapter = TabOrderPreference.TabOrderAdapter(prefs.getString("tabs","")!!)
            val visible = adapter.value.indexOf(null)
            assertEquals(4,visible)
            val labels = (0 until visible).map { position ->
                val holder = adapter.onCreateViewHolder(parent,adapter.getItemViewType(position))
                adapter.onBindViewHolder(holder,position)
                holder.itemView.findViewById<android.widget.TextView>(R.id.tabName).text.toString()
            }
            assertEquals(listOf(R.string.category_songs,R.string.category_artists,R.string.folders,R.string.category_playlists)
                .map { context.getString(it) },labels)
            assertEquals(legacy,prefs.getString("tabs",null))
        } finally {
            prefs.edit().apply { if (original==null) remove("tabs") else putString("tabs",original) }.commit()
        }
    }
}
