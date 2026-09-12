/*
 *     Copyright (C) 2026 The Gramophone authors
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.logic.artwork

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.util.Log
import java.io.File

/**
 * Resolves local artwork without broad image-library access.
 *
 * Coil owns image memory and disk caches. This object owns only the stable media cache key and
 * the optional user-authorized document tree used for sidecar images.
 */
object ArtworkResolver {
    private const val TAG = "ArtworkResolver"
    private const val PREFERENCES = "artwork_resolver"
    private const val TREE_URI = "sidecar_tree_uri"
    private const val TREE_VERSION = "sidecar_tree_version"

    enum class Kind {
        AUDIO,
        VIDEO,
    }

    enum class Source {
        EMBEDDED,
        SIDECAR,
        MEDIASTORE_THUMBNAIL,
        VIDEO_FRAME,
        PLACEHOLDER,
    }

    fun cacheKey(kind: Kind, mediaStoreId: Long, sizeBytes: Long, modifiedSeconds: Long,
                 folderVersion: Long): String =
        "artwork:${kind.name.lowercase()}:$mediaStoreId:$sizeBytes:$modifiedSeconds:$folderVersion"

    fun cacheKey(context: Context, kind: Kind, mediaStoreId: Long, sizeBytes: Long,
                 modifiedSeconds: Long): String =
        cacheKey(kind, mediaStoreId, sizeBytes, modifiedSeconds, folderVersion(context))

    fun selectedTreeUri(context: Context): Uri? = context.getSharedPreferences(PREFERENCES, 0)
        .getString(TREE_URI, null)
        ?.let(Uri::parse)

    fun selectedFolderName(context: Context): String? = selectedTreeUri(context)?.let { uri ->
        DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment
    }

    fun setSelectedTree(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFERENCES, 0).edit()
            .putString(TREE_URI, uri.toString())
            .putLong(TREE_VERSION, folderVersion(context) + 1)
            .apply()
    }

    fun clearSelectedTree(context: Context) {
        context.getSharedPreferences(PREFERENCES, 0).edit()
            .remove(TREE_URI)
            .putLong(TREE_VERSION, folderVersion(context) + 1)
            .apply()
    }

    fun folderVersion(context: Context): Long = context.getSharedPreferences(PREFERENCES, 0)
        .getLong(TREE_VERSION, 0)

    /** Returns a sidecar image only when it belongs to the user-authorized document tree. */
    fun findSidecar(context: Context, mediaFile: File): Uri? {
        val treeUri = selectedTreeUri(context) ?: return null
        val rootPath = treePath(treeUri) ?: return null
        val mediaFolder = mediaFile.parentFile ?: return null
        val relativeSegments = relativeSegments(rootPath, mediaFolder) ?: return null
        var document = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        for (segment in relativeSegments) {
            document = document.findFile(segment)?.takeIf(DocumentFile::isDirectory) ?: return null
        }
        return document.listFiles()
            .asSequence()
            .filter(DocumentFile::isFile)
            .mapNotNull { file -> sidecarScore(file.name)?.let { score -> file to score } }
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?.uri
    }

    internal fun bestSidecarName(names: Iterable<String>): String? = names
        .mapNotNull { name -> sidecarScore(name)?.let { score -> name to score } }
        .maxByOrNull { (_, score) -> score }
        ?.first

    internal fun sidecarScore(name: String?): Int? {
        val value = name?.lowercase() ?: return null
        val extension = value.substringAfterLast('.', missingDelimiterValue = "")
        if (extension !in setOf("jpg", "jpeg", "png", "webp")) return null
        val base = value.substringBeforeLast('.')
        return when {
            base == "albumart" -> 500
            base == "cover" -> 450
            base == "folder" -> 400
            base.startsWith("albumart") -> 300
            base.startsWith("cover") -> 250
            base.startsWith("folder") -> 200
            base.contains("albumart") -> 150
            base.contains("cover") -> 100
            else -> null
        }
    }

    fun logSource(kind: Kind, source: Source) {
        // Deliberately exclude file paths, titles and MediaStore IDs from diagnostics.
        Log.d(TAG, "source=${source.name.lowercase()} kind=${kind.name.lowercase()}")
    }

    private fun treePath(uri: Uri): File? = try {
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        val separator = documentId.indexOf(':')
        if (separator < 0) return null
        val volume = documentId.substring(0, separator)
        val relative = documentId.substring(separator + 1)
        val root = if (volume.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory()
        } else {
            File("/storage/$volume")
        }
        File(root, relative).canonicalFile
    } catch (_: Exception) {
        null
    }

    private fun relativeSegments(root: File, child: File): List<String>? = try {
        val rootPath = root.canonicalPath
        val childPath = child.canonicalPath
        when {
            childPath == rootPath -> emptyList()
            childPath.startsWith("$rootPath${File.separator}") ->
                childPath.removePrefix("$rootPath${File.separator}")
                    .split(File.separator)
                    .filter(String::isNotEmpty)
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
