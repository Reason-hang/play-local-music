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
        current.copy(hidden = current.hidden + items.flatMap(::mediaKeys))
    }

    fun restoreAll() = update { current -> current.copy(hidden = emptySet()) }

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
        val root = JSONObject(preferences.getString(STATE_KEY, "{}"))
        val hidden = root.optJSONArray("hidden").toStringSet()
        val categories = root.optJSONObject("categories")?.keys()?.asSequence()?.associateWith { name ->
            root.getJSONObject("categories").optJSONArray(name).toStringSet()
        }.orEmpty()
        LibraryState(hidden, categories, root.optString("activeFilter", FILTER_ALL))
    }.getOrDefault(LibraryState())

    private fun encode(value: LibraryState) = JSONObject().apply {
        put("hidden", JSONArray(value.hidden.toList()))
        put("categories", JSONObject().apply {
            value.categories.forEach { (name, keys) -> put(name, JSONArray(keys.toList())) }
        })
        put("activeFilter", value.activeFilter)
    }.toString()

    private fun JSONArray?.toStringSet(): Set<String> = buildSet {
        if (this@toStringSet == null) return@buildSet
        for (index in 0 until this@toStringSet.length()) add(this@toStringSet.getString(index))
    }

    data class LibraryState(
        val hidden: Set<String> = emptySet(),
        val categories: Map<String, Set<String>> = emptyMap(),
        val activeFilter: String = FILTER_ALL
    )

    companion object {
        private const val STATE_KEY = "state_v1"
        const val FILTER_ALL = "all"
        const val FILTER_MP3 = "mp3"
        const val FILTER_MP4 = "mp4"
        fun categoryFilter(name: String) = "category:$name"
    }

}
