/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.logic.diagnostics

import android.content.Context
import android.os.Build
import android.util.AtomicFile
import androidx.media3.common.util.Log
import org.akanework.gramophone.BuildConfig
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Private, bounded diagnostics for user-initiated export. It never sends data over the network.
 */
object DiagnosticStore {
    private const val TAG = "DiagnosticStore"
    private const val DIRECTORY = "diagnostics"
    private const val EVENT_FILE = "events.jsonl"
    private const val MAX_EVENTS = 200
    private const val MAX_CRASH_FILES = 20
    private const val MAX_EVENT_BYTES = 128 * 1024
    private const val MAX_CRASH_BYTES = 64 * 1024
    private const val MAX_SUMMARY_EVENT_CHARS = 1024
    private val localPathRegex = Regex("/(?:storage|sdcard|data|mnt|Users)/[^\\s\\n]*")
    private val contentUriRegex = Regex("content://[^\\s\\n\\\"\\\\]+")
    private val eventExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "diagnostic-events").apply { isDaemon = true }
    }

    data class CrashRecord(val file: File, val timestamp: Long)

    fun recordEvent(
        context: Context,
        module: String,
        event: String,
        level: String = "INFO",
        errorCode: String? = null,
        details: Map<String, String> = emptyMap()
    ) {
        val line = runCatching {
            JSONObject().apply {
                put("time", System.currentTimeMillis())
                put("level", level)
                put("module", module)
                put("event", event)
                put("errorCode", errorCode?.let(::sanitize))
                put("details", JSONObject(details.mapValues { sanitize(it.value) }))
            }.toString()
        }.getOrElse {
            Log.w(TAG, "Unable to build a diagnostic event", it)
            return
        }
        eventExecutor.execute {
            runCatching { appendEventLine(context.applicationContext, line) }
                .onFailure { Log.w(TAG, "Unable to save a diagnostic event", it) }
        }
    }

    fun recordCrash(context: Context, threadName: String, throwable: Throwable) {
        runCatching {
            val now = System.currentTimeMillis()
            val summary = buildString {
                appendLine("本地听歌崩溃摘要")
                appendLine("time=$now")
                appendLine("version=${BuildConfig.MY_VERSION_NAME}")
                appendLine("releaseType=${BuildConfig.RELEASE_TYPE}")
                appendLine("device=${Build.BRAND} ${Build.MODEL}")
                appendLine("sdk=${Build.VERSION.SDK_INT}")
                appendLine("thread=$threadName")
                appendLine()
                append(sanitize(throwable.stackTraceToString()).take(MAX_CRASH_BYTES))
            }
            val crashFile = File(diagnosticDirectory(context), "crash_$now.txt")
            writeAtomically(crashFile, summary)
            trimCrashFiles(context)
            recordEventNow(
                context = context,
                module = "crash",
                event = "uncaught_exception",
                level = "ERROR",
                errorCode = throwable.javaClass.simpleName,
                details = mapOf("thread" to threadName, "message" to (throwable.message ?: ""))
            )
        }.onFailure { Log.e(TAG, "Unable to save crash diagnostics", it) }
    }

    fun crashRecords(context: Context): List<CrashRecord> = diagnosticDirectory(context)
        .listFiles { file -> file.name.startsWith("crash_") && file.extension == "txt" }
        ?.map { CrashRecord(it, it.lastModified()) }
        ?.sortedByDescending(CrashRecord::timestamp)
        .orEmpty()

    fun readCrash(record: CrashRecord): String = runCatching {
        record.file.readText().take(MAX_CRASH_BYTES)
    }.getOrDefault("崩溃记录已被清理或无法读取。")

    fun copySummary(context: Context): String {
        awaitPendingEvents()
        return buildString {
            appendLine("本地听歌诊断摘要")
            appendLine("version=${BuildConfig.MY_VERSION_NAME}")
            appendLine("device=${Build.BRAND} ${Build.MODEL}")
            appendLine("sdk=${Build.VERSION.SDK_INT}")
            crashRecords(context).firstOrNull()?.let { record ->
                appendLine("latestCrash=${record.timestamp}")
                append(sanitize(readCrash(record)).take(12 * 1024))
            } ?: appendLine("latestCrash=none")
            appendLine()
            appendLine("recentEvents:")
            val eventFile = File(diagnosticDirectory(context), EVENT_FILE)
            eventFile.takeIf(File::exists)?.readLines()?.takeLast(20)?.forEach { line ->
                appendLine(sanitize(line.replace("\\/", "/")).take(MAX_SUMMARY_EVENT_CHARS))
            } ?: appendLine("none")
        }
    }

    fun clear(context: Context) {
        awaitPendingEvents()
        synchronized(this) {
            diagnosticDirectory(context).listFiles()?.forEach(File::delete)
        }
    }

    fun export(context: Context): File {
        awaitPendingEvents()
        val directory = diagnosticDirectory(context)
        val export = File(context.cacheDir, "diagnostic-export-${System.currentTimeMillis()}.zip")
        ZipOutputStream(export.outputStream().buffered()).use { zip ->
            val manifest = JSONObject().apply {
                put("generatedAt", System.currentTimeMillis())
                put("version", BuildConfig.MY_VERSION_NAME)
                put("releaseType", BuildConfig.RELEASE_TYPE)
                put("device", "${Build.BRAND} ${Build.MODEL}")
                put("sdk", Build.VERSION.SDK_INT)
                put("privacy", "local-only, user-initiated export, no media paths or titles")
            }.toString(2)
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toByteArray())
            zip.closeEntry()
            directory.listFiles()?.sortedBy { it.name }?.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return export
    }

    private fun diagnosticDirectory(context: Context): File =
        File(context.filesDir, DIRECTORY).also(File::mkdirs)

    private fun recordEventNow(
        context: Context,
        module: String,
        event: String,
        level: String,
        errorCode: String?,
        details: Map<String, String>
    ) {
        val line = JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("level", level)
            put("module", module)
            put("event", event)
            put("errorCode", errorCode?.let(::sanitize))
            put("details", JSONObject(details.mapValues { sanitize(it.value) }))
        }.toString()
        appendEventLine(context.applicationContext, line)
    }

    private fun appendEventLine(context: Context, line: String) {
        synchronized(this) {
            val eventFile = File(diagnosticDirectory(context), EVENT_FILE)
            val lines = eventFile.takeIf(File::exists)?.readLines().orEmpty()
                .plus(line)
                .takeLast(MAX_EVENTS)
            var content = lines.joinToString(separator = "\n", postfix = "\n")
            while (content.toByteArray().size > MAX_EVENT_BYTES && lines.isNotEmpty()) {
                content = content.substringAfter("\n", missingDelimiterValue = "")
            }
            writeAtomically(eventFile, content)
        }
    }

    private fun awaitPendingEvents() {
        runCatching {
            eventExecutor.submit { }.get(5, TimeUnit.SECONDS)
        }.onFailure { Log.w(TAG, "Unable to flush diagnostic events", it) }
    }

    private fun trimCrashFiles(context: Context) {
        crashRecords(context).drop(MAX_CRASH_FILES).forEach { it.file.delete() }
    }

    private fun writeAtomically(file: File, content: String) {
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(content.toByteArray())
            atomicFile.finishWrite(stream)
        } catch (error: Throwable) {
            atomicFile.failWrite(stream)
            throw error
        }
    }

    private fun sanitize(value: String): String = contentUriRegex.replace(
        localPathRegex.replace(value, "<redacted-path>"),
        "<redacted-uri>"
    )
}
