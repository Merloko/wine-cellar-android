package com.winecellar.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes exported content to a private cache file and hands it to the Android
 * share sheet. The user picks the destination — nothing is uploaded anywhere.
 *
 * Suspending so the file write happens off the main thread; the share sheet is
 * launched back on the caller's (main) dispatcher.
 */
object ExportUtils {

    suspend fun share(context: Context, fileName: String, mimeType: String, content: String) {
        val uri = try {
            withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, fileName).apply { writeText(content) }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't prepare the export file.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Wine cellar export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Export cellar")
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        try {
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to share the export.", Toast.LENGTH_SHORT).show()
        }
    }
}
