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
        val fingerprints = item.fingerprints()
        if (fingerprints.isEmpty()) return
        val durationMs = item.mediaMetadata.durationMs ?: return
        if (durationMs <= 0 || positionMs < MIN_SAVE_POSITION_MS || isFinished(positionMs, durationMs)) {
            var removed = false
            fingerprints.forEach { fingerprint ->
                removed = records.remove(fingerprint.key) != null || removed
            }
            if (removed) persist()
            return
        }
        val record = Record(
            positionMs = positionMs.coerceAtMost(durationMs),
            durationMs = durationMs,
            modifiedDate = fingerprints.firstNotNullOfOrNull { it.modifiedDate },
            updatedAtMs = now()
        )
        fingerprints.forEach { records[it.key] = record }
        trim()
        persist()
    }

    @Synchronized
    fun resumePosition(item: MediaItem): Long? {
        if (!item.isVideo()) return null
        val fingerprints = item.fingerprints()
        if (fingerprints.isEmpty()) return null
        val invalidKeys = mutableSetOf<String>()
        fingerprints.forEach { fingerprint ->
            val record = records[fingerprint.key] ?: return@forEach
            if (!record.matches(item.mediaMetadata.durationMs, fingerprint.modifiedDate) || isExpired(record)) {
                invalidKeys += fingerprint.key
                return@forEach
            }
            val positionMs = record.positionMs
                .takeIf { it >= MIN_SAVE_POSITION_MS && !isFinished(it, record.durationMs) }
                ?: return@forEach
            // Migrate records written by 1.6.2, which only used one identity representation.
            fingerprints.forEach { records[it.key] = record }
            if (invalidKeys.isNotEmpty()) invalidKeys.forEach(records::remove)
            persist()
            return positionMs
        }
        if (invalidKeys.isNotEmpty()) {
            invalidKeys.forEach(records::remove)
            persist()
        }
        return null
    }

    private fun MediaItem.isVideo(): Boolean =
        mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO ||
            localConfiguration?.mimeType?.startsWith("video/", true) == true

    private fun MediaItem.fingerprints(): List<Fingerprint> = buildList {
        mediaId.takeIf(String::isNotBlank)?.let { add(Fingerprint("id:$it", mediaMetadata.modifiedDate)) }
        getFile()?.absolutePath?.let(MediaIdentity::pathKey)
            ?.takeUnless { pathKey -> any { it.key == pathKey } }
            ?.let { add(Fingerprint(it, mediaMetadata.modifiedDate)) }
    }

    private fun Record.matches(durationMs: Long?, modifiedDate: Long?): Boolean =
        (durationMs == null || this.durationMs == durationMs) &&
            (modifiedDate == null || this.modifiedDate == null || this.modifiedDate == modifiedDate)

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
        val root = JSONObject(preferences.getString(STATE_KEY, "{}") ?: "{}")
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
