package com.winecellar.data

import android.content.Context
import androidx.core.content.edit

/**
 * Remembers the CSV "sync file" the user linked through the system document
 * picker (Google Drive, Dropbox, local storage — whatever provider they chose).
 *
 * Only the file's content URI is stored, on-device. No cloud account, token, or
 * SDK is involved: the app talks to the file through Android's Storage Access
 * Framework, exactly as it already does for one-off export/import.
 */
class SyncPrefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)

    /** The linked file's content URI as a string, or null if none is linked. */
    var linkedUri: String?
        get() = prefs.getString(KEY_URI, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_URI) else putString(KEY_URI, value)
        }

    private companion object {
        const val KEY_URI = "linked_csv_uri"
    }
}
