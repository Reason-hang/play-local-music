package org.akanework.gramophone.logic.library

import android.content.Context
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.akanework.gramophone.logic.getFile
import org.json.JSONArray
import org.json.JSONObject
import uk.akane.libphonograph.reader.MediaIdentity

/** App-private organization state. It never deletes or moves user media files. */
class LocalLibraryManager(context: Context) {
    private val preferences = context.getSharedPreferences("local_library", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(readState())
    val state: StateFlow<LibraryState> = mutableState
    val hiddenMediaKeys = MutableStateFlow(mutableState.value.hidden)

    fun isHidden(mediaItem: MediaItem): Boolean = mediaKeys(mediaItem).any(state.value.hidden::contains)

    fun hide(items: Collection<MediaItem>) = update { current ->
        val records = items.associate { mediaItem ->
            categoryKey(mediaItem) to HiddenMediaRecord(
                title = mediaItem.mediaMetadata.title?.toString()?.takeIf(String::isNotBlank)
                    ?: mediaItem.getFile()?.name
                    ?: mediaItem.mediaId,
                keys = mediaKeys(mediaItem)
            )
        }
        current.copy(
            hidden = current.hidden + records.values.flatMap(HiddenMediaRecord::keys),
            hiddenRecords = current.hiddenRecords + records
        )
    }

    fun restore(recordIds: Collection<String>) = update { current ->
        val records = current.hiddenRecords.filterKeys(recordIds::contains)
        current.copy(
            hidden = current.hidden - records.values.flatMap(HiddenMediaRecord::keys).toSet(),
            hiddenRecords = current.hiddenRecords - recordIds.toSet()
        )
    }

    fun restoreAll() = update { current ->
        current.copy(hidden = emptySet(), hiddenRecords = emptyMap())
    }

    fun addToCategory(name: String, items: Collection<MediaItem>) = update { current ->
        val cleanName = name.trim()
        require(cleanName.isNotEmpty())
        current.copy(categories = current.categories + (cleanName to
            ((current.categories[cleanName] ?: emptySet()) + items.map(::categoryKey))))
    }

    fun removeCategory(name: String) = update { current ->
        current.copy(categories = current.categories - name)
    }

    fun selectFilter(value: String) = update { current -> current.copy(activeFilter = value) }

    fun isPinned(mediaItem: MediaItem): Boolean = mediaKeys(mediaItem).any(state.value.pinned::containsKey)

    fun pinnedOrder(mediaItem: MediaItem): Long? = mediaKeys(mediaItem).mapNotNull {
        state.value.pinned[it]
    }.minOrNull()

    fun pin(items: Collection<MediaItem>) = update { current ->
        val pinned = current.pinned.toMutableMap()
        var nextOrder = (pinned.values.maxOrNull() ?: -1L) + 1L
        items.forEach { mediaItem ->
            val keys = mediaKeys(mediaItem)
            val existingOrder = keys.mapNotNull(pinned::get).minOrNull()
            val order = existingOrder ?: nextOrder++
            keys.forEach { key -> pinned[key] = order }
        }
        current.copy(pinned = pinned)
    }

    fun unpin(items: Collection<MediaItem>) = update { current ->
        current.copy(pinned = current.pinned - items.flatMap(::mediaKeys).toSet())
    }

    fun mediaKeys(mediaItem: MediaItem): Set<String> = mediaItem.getFile()?.absolutePath?.let {
        MediaIdentity.keys(mediaItem.mediaId, it)
    } ?: setOf("id:${mediaItem.mediaId}")

    fun categoryKey(mediaItem: MediaItem): String = mediaItem.getFile()?.absolutePath?.let(MediaIdentity::pathKey)
        ?: "id:${mediaItem.mediaId}"

    private fun update(transform: (LibraryState) -> LibraryState) {
        val next = transform(mutableState.value)
        mutableState.value = next
        hiddenMediaKeys.value = next.hidden
        preferences.edit { putString(STATE_KEY, encode(next)) }
    }

    private fun readState(): LibraryState = runCatching {
        val root = JSONObject(preferences.getString(STATE_KEY, "{}") ?: "{}")
        val hidden = root.optJSONArray("hidden").toStringSet()
        val hiddenRecords = root.optJSONObject("hiddenRecords")?.keys()?.asSequence()?.associateWith { id ->
            root.getJSONObject("hiddenRecords").getJSONObject(id).let { record ->
                HiddenMediaRecord(
                    title = record.optString("title", "已移除媒体"),
                    keys = record.optJSONArray("keys").toStringSet()
                )
            }
        }.orEmpty()
        val categories = root.optJSONObject("categories")?.keys()?.asSequence()?.associateWith { name ->
            root.getJSONObject("categories").optJSONArray(name).toStringSet()
        }.orEmpty()
        val pinnedObject = root.optJSONObject("pinned")
        val pinned = pinnedObject?.keys()?.asSequence()?.associateWith { key ->
            pinnedObject.optLong(key)
        }.orEmpty()
        LibraryState(
            hidden = hidden,
            hiddenRecords = hiddenRecords,
            categories = categories,
            activeFilter = root.optString("activeFilter", FILTER_ALL),
            pinned = pinned
        )
    }.getOrDefault(LibraryState())

    private fun encode(value: LibraryState) = JSONObject().apply {
        put("hidden", JSONArray(value.hidden.toList()))
        put("hiddenRecords", JSONObject().apply {
            value.hiddenRecords.forEach { (id, record) ->
                put(id, JSONObject().apply {
                    put("title", record.title)
                    put("keys", JSONArray(record.keys.toList()))
                })
            }
        })
        put("categories", JSONObject().apply {
            value.categories.forEach { (name, keys) -> put(name, JSONArray(keys.toList())) }
        })
        put("activeFilter", value.activeFilter)
        put("pinned", JSONObject().apply {
            value.pinned.forEach { (key, order) -> put(key, order) }
        })
    }.toString()

    private fun JSONArray?.toStringSet(): Set<String> = buildSet {
        if (this@toStringSet == null) return@buildSet
        for (index in 0 until this@toStringSet.length()) add(this@toStringSet.getString(index))
    }

    data class LibraryState(
        val hidden: Set<String> = emptySet(),
        val hiddenRecords: Map<String, HiddenMediaRecord> = emptyMap(),
        val categories: Map<String, Set<String>> = emptyMap(),
        val activeFilter: String = FILTER_ALL,
        val pinned: Map<String, Long> = emptyMap()
    )

    data class HiddenMediaRecord(
        val title: String,
        val keys: Set<String>
    )

    companion object {
        private const val STATE_KEY = "state_v1"
        const val FILTER_ALL = "all"
        const val FILTER_FAVORITES = "favorites"
        const val FILTER_MP3 = "mp3"
        const val FILTER_MP4 = "mp4"
        fun categoryFilter(name: String) = "category:$name"
    }

}
