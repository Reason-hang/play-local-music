/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.logic.library

import android.content.Context
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.akanework.gramophone.logic.getFile
import org.json.JSONObject
import uk.akane.libphonograph.items.modifiedDate
import uk.akane.libphonograph.reader.MediaIdentity
import kotlin.math.min

/** App-private per-video resume state. It does not store titles, paths, or audio progress. */
class VideoResumeProgressStore(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val records = readRecords().toMutableMap()

    @Synchronized
    fun save(item: MediaItem, positionMs: Long) {
        if (!item.isVideo()) return
        val fingerprint = item.fingerprint() ?: return
        val durationMs = item.mediaMetadata.durationMs ?: return
        if (durationMs <= 0 || positionMs < MIN_SAVE_POSITION_MS || isFinished(positionMs, durationMs)) {
            if (records.remove(fingerprint.key) != null) persist()
            return
        }
        records[fingerprint.key] = Record(
            positionMs = positionMs.coerceAtMost(durationMs),
            durationMs = durationMs,
            modifiedDate = fingerprint.modifiedDate,
            updatedAtMs = now()
        )
        trim()
        persist()
    }

    @Synchronized
    fun resumePosition(item: MediaItem): Long? {
        if (!item.isVideo()) return null
        val fingerprint = item.fingerprint() ?: return null
        val record = records[fingerprint.key] ?: return null
        if (record.durationMs != item.mediaMetadata.durationMs ||
            record.modifiedDate != fingerprint.modifiedDate ||
            isExpired(record)
        ) {
            records.remove(fingerprint.key)
            persist()
            return null
        }
        return record.positionMs.takeIf { it >= MIN_SAVE_POSITION_MS && !isFinished(it, record.durationMs) }
    }

    private fun MediaItem.isVideo(): Boolean =
        mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO ||
            localConfiguration?.mimeType?.startsWith("video/", true) == true

    private fun MediaItem.fingerprint(): Fingerprint? {
        val key = getFile()?.absolutePath?.let(MediaIdentity::pathKey)
            ?: mediaId.takeIf(String::isNotBlank)?.let { "id:$it" }
            ?: return null
        return Fingerprint(key, mediaMetadata.modifiedDate)
    }

    private fun isFinished(positionMs: Long, durationMs: Long): Boolean =
        durationMs - positionMs <= min(FINISHED_THRESHOLD_MS, durationMs / 10)

    private fun isExpired(record: Record): Boolean = now() - record.updatedAtMs > MAX_AGE_MS

    private fun trim() {
        records.entries.removeAll { isExpired(it.value) }
        if (records.size > MAX_RECORDS) {
            records.entries.sortedBy { it.value.updatedAtMs }
                .take(records.size - MAX_RECORDS)
                .forEach { records.remove(it.key) }
        }
    }

    private fun readRecords(): Map<String, Record> = runCatching {
        val root = JSONObject(preferences.getString(STATE_KEY, "{}"))
        root.keys().asSequence().associateWith { key ->
            root.getJSONObject(key).let {
                Record(
                    positionMs = it.getLong("positionMs"),
                    durationMs = it.getLong("durationMs"),
                    modifiedDate = it.optLong("modifiedDate", NO_MODIFIED_DATE)
                        .takeUnless { value -> value == NO_MODIFIED_DATE },
                    updatedAtMs = it.getLong("updatedAtMs")
                )
            }
        }.filterValues { !isExpired(it) }
    }.getOrDefault(emptyMap())

    private fun persist() {
        preferences.edit {
            putString(STATE_KEY, JSONObject().apply {
                records.forEach { (key, record) ->
                    put(key, JSONObject().apply {
                        put("positionMs", record.positionMs)
                        put("durationMs", record.durationMs)
                        put("modifiedDate", record.modifiedDate ?: NO_MODIFIED_DATE)
                        put("updatedAtMs", record.updatedAtMs)
                    })
                }
            }.toString())
        }
    }

    private data class Fingerprint(val key: String, val modifiedDate: Long?)
    private data class Record(
        val positionMs: Long,
        val durationMs: Long,
        val modifiedDate: Long?,
        val updatedAtMs: Long
    )

    companion object {
        private const val PREFERENCES = "video_resume_progress"
        private const val STATE_KEY = "records_v1"
        private const val MIN_SAVE_POSITION_MS = 5_000L
        private const val FINISHED_THRESHOLD_MS = 30_000L
        private const val MAX_AGE_MS = 180L * 24 * 60 * 60 * 1000
        private const val MAX_RECORDS = 500
        private const val NO_MODIFIED_DATE = Long.MIN_VALUE
    }
}
