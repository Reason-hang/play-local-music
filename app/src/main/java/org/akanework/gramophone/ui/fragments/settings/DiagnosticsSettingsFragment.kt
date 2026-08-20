/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.ui.fragments.settings

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.diagnostics.DiagnosticStore
import org.akanework.gramophone.ui.fragments.BasePreferenceFragment
import org.akanework.gramophone.ui.fragments.BaseSettingsActivity
import java.text.DateFormat
import java.util.Date

class DiagnosticsSettingsActivity : BaseSettingsActivity(
    R.string.settings_diagnostics,
    { DiagnosticsSettingsFragment() })

class DiagnosticsSettingsFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_diagnostics, rootKey)
        findPreference<Preference>("diagnostics_copy")?.setOnPreferenceClickListener {
            val appContext = requireContext().applicationContext
            viewLifecycleOwner.lifecycleScope.launch {
                val text = withContext(Dispatchers.IO) {
                    DiagnosticStore.copySummary(appContext)
                }
                if (!isAdded) return@launch
                val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("本地听歌诊断摘要", text))
                Toast.makeText(requireContext(), R.string.diagnostics_copy_done, Toast.LENGTH_SHORT).show()
            }
            true
        }
        findPreference<Preference>("diagnostics_export")?.setOnPreferenceClickListener {
            exportDiagnostics()
            true
        }
        findPreference<Preference>("diagnostics_clear")?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.diagnostics_clear_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.diagnostics_clear) { _, _ ->
                    val appContext = requireContext().applicationContext
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        DiagnosticStore.clear(appContext)
                        withContext(Dispatchers.Main) {
                            if (isAdded) refreshCrashRecords()
                        }
                    }
                }
                .show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCrashRecords()
    }

    private fun refreshCrashRecords() {
        val category = findPreference<PreferenceCategory>("diagnostics_crashes") ?: return
        val appContext = requireContext().applicationContext
        category.removeAll()
        viewLifecycleOwner.lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                DiagnosticStore.crashRecords(appContext)
            }
            if (!isAdded) return@launch
            if (records.isEmpty()) {
                category.addPreference(Preference(requireContext()).apply {
                    isSelectable = false
                    summary = getString(R.string.diagnostics_no_crash)
                })
                return@launch
            }
            records.forEach { record ->
                category.addPreference(Preference(requireContext()).apply {
                    title = DateFormat.getDateTimeInstance().format(Date(record.timestamp))
                    summary = getString(R.string.diagnostics_view_crash)
                    setOnPreferenceClickListener {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val crash = withContext(Dispatchers.IO) {
                                DiagnosticStore.readCrash(record)
                            }
                            if (!isAdded) return@launch
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(title)
                                .setMessage(crash)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                        true
                    }
                })
            }
        }
    }

    private fun exportDiagnostics() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            runCatching { DiagnosticStore.export(appContext) }
                .onSuccess { file -> withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    val uri = FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileProvider",
                        file
                    )
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, getString(R.string.diagnostics_export)))
                } }
                .onFailure { withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), R.string.diagnostics_export_failed, Toast.LENGTH_LONG).show()
                    }
                } }
        }
    }
}
