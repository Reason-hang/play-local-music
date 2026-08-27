/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.widget.NestedScrollView
import androidx.media3.common.Player
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.slider.Slider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.dpToPx

/**
 * Product-owned speed editor. Both rendering surfaces send their change through the same
 * MediaSession-backed [Player], so the playback service remains the single state owner.
 */
object PlaybackSpeedDialog {
    private const val LOCK_PREFERENCE = "playback_tempo_pitch_locked"

    fun show(context: Context, player: Player, preferences: SharedPreferences) {
        val initialParameters = player.playbackParameters
        val initiallyLocked = preferences.getBoolean(LOCK_PREFERENCE, false)
        var selectedSpeed = PlaybackSpeedPolicy.nearestPreset(initialParameters.speed)
        val speedLabels = context.resources.getStringArray(R.array.playback_speed_presets)
        check(speedLabels.size == PlaybackSpeedPolicy.presets.size) {
            "Playback-speed labels and product presets must have the same size."
        }

        val tempoText = valueText(context, R.string.tempo, initialParameters.speed)
        val speedRadioIds = IntArray(PlaybackSpeedPolicy.presets.size)
        val speedOptions = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
            PlaybackSpeedPolicy.presets.forEachIndexed { index, speed ->
                addView(MaterialRadioButton(context).apply {
                    id = View.generateViewId().also { speedRadioIds[index] = it }
                    text = speedLabels[index]
                    isChecked = PlaybackSpeedPolicy.presetIndex(selectedSpeed) == index
                })
            }
        }
        val pitchSlider = Slider(context).apply {
            valueFrom = 0.25f
            valueTo = 4f
            stepSize = 0.01f
            value = initialParameters.pitch.coerceIn(valueFrom, valueTo)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val pitchText = valueText(context, R.string.pitch, pitchSlider.value)
        val lockCheckbox = MaterialCheckBox(context).apply {
            text = context.getString(R.string.lock_tempo_pitch)
            isChecked = initiallyLocked
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        fun applyParameters() {
            player.playbackParameters = PlaybackSpeedPolicy.parameters(
                selectedSpeed,
                pitchSlider.value,
                lockCheckbox.isChecked
            )
        }

        fun updatePitchAvailability() {
            pitchSlider.isEnabled = !lockCheckbox.isChecked
            pitchText.isEnabled = !lockCheckbox.isChecked
        }

        updatePitchAvailability()
        speedOptions.setOnCheckedChangeListener { _, checkedId ->
            val index = speedRadioIds.indexOf(checkedId)
            if (index < 0) return@setOnCheckedChangeListener
            selectedSpeed = PlaybackSpeedPolicy.presets[index]
            if (lockCheckbox.isChecked) pitchSlider.value = selectedSpeed
            tempoText.text = context.getString(
                R.string.tempo_pitch_value, context.getString(R.string.tempo), selectedSpeed
            )
            applyParameters()
        }
        pitchSlider.addOnChangeListener { _, value, fromUser ->
            pitchText.text = context.getString(
                R.string.tempo_pitch_value, context.getString(R.string.pitch), value
            )
            if (fromUser) applyParameters()
        }
        lockCheckbox.setOnCheckedChangeListener { _, locked ->
            updatePitchAvailability()
            if (locked) pitchSlider.value = selectedSpeed
            applyParameters()
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48.dpToPx(context), 16.dpToPx(context), 48.dpToPx(context), 0)
            addView(tempoText)
            addView(speedOptions)
            addView(pitchText)
            addView(pitchSlider)
            addView(lockCheckbox)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.playback_speed)
            .setView(NestedScrollView(context).apply { addView(container) })
            .setPositiveButton(android.R.string.ok) { _, _ ->
                preferences.edit { putBoolean(LOCK_PREFERENCE, lockCheckbox.isChecked) }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                player.playbackParameters = initialParameters
            }
            .setNeutralButton(R.string.reset) { _, _ ->
                preferences.edit { putBoolean(LOCK_PREFERENCE, false) }
                player.playbackParameters = PlaybackSpeedPolicy.defaultParameters()
            }
            .show()
    }

    private fun valueText(context: Context, labelId: Int, value: Float) = TextView(context).apply {
        text = context.getString(R.string.tempo_pitch_value, context.getString(labelId), value)
        gravity = Gravity.CENTER
        textSize = 16f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}
