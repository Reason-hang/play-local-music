/*
 *     Copyright (C) 2026 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 */

package org.akanework.gramophone.ui.fragments.settings

import android.content.ClipboardManager
import android.content.DialogInterface
import android.os.Looper
import org.akanework.gramophone.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DiagnosticsClipboardTest {
    @Test
    fun crashDialogProvidesCopyButtonAndSelectableText() {
        val activity = Robolectric.buildActivity(DiagnosticsSettingsActivity::class.java)
            .setup()
            .get()
        val dialog = showCrashDialog(activity, "Crash", "stack trace")

        val message = dialog.findViewById<android.widget.TextView>(android.R.id.message)
        assertTrue(message?.isTextSelectable == true)
        assertEquals(
            activity.getString(R.string.diagnostics_copy_crash),
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).text,
        )

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        val clipboard = activity.getSystemService(ClipboardManager::class.java)
        assertEquals("stack trace", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun pageCopyHelperCopiesTheProvidedDiagnosticText() {
        val context = RuntimeEnvironment.getApplication()

        copyDiagnosticText(context, "stack trace")

        val clipboard = context.getSystemService(ClipboardManager::class.java)
        assertEquals("stack trace", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }
}
