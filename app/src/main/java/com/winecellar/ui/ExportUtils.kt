package com.winecellar.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Helpers for user-initiated cellar export. Nothing is uploaded anywhere — the
 * file lives in the app's private cache and the user picks the destination.
 *
 * The write ([writeExport]) is suspending so it can run off the main thread on a
 * lifecycle-independent scope; [launchShare] is a quick, synchronous intent so
 * it can't be cancelled out from under the user by leaving composition.
 */
object ExportUtils {

    /** Write [content] to a private cache file and return a shareable content Uri, or null on failure. */
    suspend fun writeExport(context: Context, fileName: String, content: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, fileName).apply { writeText(content) }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: CancellationException) {
                throw e // never swallow cancellation
            } catch (e: Exception) {
                null
            }
        }

    /** Launch the system share sheet for [uri]. Returns false if no app can handle it. */
    fun launchShare(context: Context, uri: Uri, mimeType: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Wine cellar export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Export cellar")
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
