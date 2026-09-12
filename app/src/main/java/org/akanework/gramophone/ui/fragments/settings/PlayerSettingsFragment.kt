/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.ui.fragments.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.material.color.DynamicColors
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.GramophoneApplication
import org.akanework.gramophone.logic.artwork.ArtworkResolver
import org.akanework.gramophone.ui.fragments.BasePreferenceFragment
import org.akanework.gramophone.ui.fragments.BaseSettingsActivity

class PlayerSettingsActivity : BaseSettingsActivity(
    R.string.settings_player_ui,
    { PlayerSettingsFragment() })

class PlayerSettingsFragment : BasePreferenceFragment() {
    private val artworkFolderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Some document providers cannot persist the read grant. Keep the prior setting intact.
            return@registerForActivityResult
        }
        ArtworkResolver.setSelectedTree(requireContext(), uri)
        refreshArtworkUris()
        updateArtworkFolderPreferences()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_player, rootKey)
        findPreference<Preference>("content_based_color")!!
            .isVisible = DynamicColors.isDynamicColorAvailable()

        val cookieCoverPref = findPreference<SwitchPreferenceCompat>("cookie_cover")
        val roundCornerPref = findPreference<SeekBarPreference>("album_round_corner")

        cookieCoverPref?.setOnPreferenceChangeListener { _, newValue ->
            roundCornerPref?.isEnabled = !(newValue as Boolean)
            true
        }

        roundCornerPref?.isEnabled = !(cookieCoverPref?.isChecked ?: false)

        findPreference<Preference>("artwork_folder")?.setOnPreferenceClickListener {
            artworkFolderPicker.launch(null)
            true
        }
        findPreference<Preference>("artwork_folder_clear")?.setOnPreferenceClickListener {
            ArtworkResolver.selectedTreeUri(requireContext())?.let { uri ->
                try {
                    requireContext().contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    // The document provider may already have revoked the persisted grant.
                }
            }
            ArtworkResolver.clearSelectedTree(requireContext())
            refreshArtworkUris()
            updateArtworkFolderPreferences()
            true
        }
        updateArtworkFolderPreferences()

    }

    private fun updateArtworkFolderPreferences() {
        val folderName = ArtworkResolver.selectedFolderName(requireContext())
        findPreference<Preference>("artwork_folder")?.summary = folderName
            ?: getString(R.string.settings_artwork_folder_summary)
        findPreference<Preference>("artwork_folder_clear")?.isVisible = folderName != null
    }

    private fun refreshArtworkUris() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            (requireContext().applicationContext as GramophoneApplication).reader.refresh()
        }
    }
}
