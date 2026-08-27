/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.ui.components

import androidx.media3.common.PlaybackParameters
import kotlin.math.abs

/** The product-owned playback-speed contract shared by portrait and full-screen video UI. */
object PlaybackSpeedPolicy {
    val presets = floatArrayOf(0.75f, 1f, 1.3f, 1.5f, 1.7f, 2f)

    fun presetIndex(speed: Float): Int? =
        presets.indexOfFirst { abs(it - speed) < EPSILON }.takeIf { it >= 0 }

    fun nearestPreset(speed: Float): Float = presets.minBy { abs(it - speed) }

    fun parameters(speed: Float, pitch: Float, locked: Boolean): PlaybackParameters {
        val selectedSpeed = nearestPreset(speed)
        return PlaybackParameters(
            selectedSpeed,
            if (locked) selectedSpeed else pitch.coerceIn(MIN_PITCH, MAX_PITCH)
        )
    }

    fun defaultParameters(): PlaybackParameters = PlaybackParameters(1f, 1f)

    private const val MIN_PITCH = 0.25f
    private const val MAX_PITCH = 4f
    private const val EPSILON = 0.001f
}
