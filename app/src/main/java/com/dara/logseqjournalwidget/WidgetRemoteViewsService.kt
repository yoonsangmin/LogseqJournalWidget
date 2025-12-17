package com.dara.logseqjournalwidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetRemoteViewsFactory(this.applicationContext)
    }
}

class WidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var journalLines: List<String> = emptyList()

    override fun onCreate() {
        // Initialize
    }

    override fun onDataSetChanged() {
        // This is called when the data set needs to be updated (e.g., on refresh).
        journalLines = readJournalFile(context).lines()
    }

    override fun onDestroy() {
        // Clean up resources
    }

    override fun getCount(): Int {
        return journalLines.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val line = journalLines.getOrNull(position) ?: ""
        return RemoteViews(context.packageName, R.layout.widget_list_item).apply {
            val trimmedLine = line.trim()
            val finalLine: String

            if (trimmedLine == "-") {
                finalLine = ""
            } else if (trimmedLine.startsWith("-")) {
                val hyphenIndex = line.indexOf('-')
                val indentation = line.substring(0, hyphenIndex)
                val restOfLine = line.substring(hyphenIndex + 1)
                val content = restOfLine.trimStart()
                finalLine = "$indentation● $content"
            } else {
                finalLine = line
            }
            setTextViewText(R.id.list_item_text, finalLine)

            // Create an Intent to launch the Logseq app
            val logseqLaunchIntent = context.packageManager.getLaunchIntentForPackage("com.logseq.app")
            if (logseqLaunchIntent != null) {
                // Use the position as the requestCode to ensure each PendingIntent is unique
                val logseqPendingIntent = PendingIntent.getActivity(context, position, logseqLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                setOnClickPendingIntent(R.id.list_item_text, logseqPendingIntent)
            }
        }
    }

    override fun getLoadingView(): RemoteViews? {
        // Return a loading view, or null for the default loading view.
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun readJournalFile(context: Context): String {
        val pathUriString = getJournalPath(context)

        if (pathUriString == null) {
            return "Please run the app to set the journal directory."
        }

        return try {
            val dateFormat = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault())
            val todayDate = dateFormat.format(Date())
            val journalFileName = "$todayDate.md"

            val directoryUri = Uri.parse(pathUriString)
            val selectedDir = DocumentFile.fromTreeUri(context, directoryUri)
            val journalsDir = selectedDir?.findFile("journals")

            if (journalsDir == null || !journalsDir.isDirectory) {
                return "Could not find 'journals' directory in the selected folder."
            }

            val journalFile = journalsDir.findFile(journalFileName)

            if (journalFile != null && journalFile.exists()) {
                context.contentResolver.openInputStream(journalFile.uri)?.use {
                    it.bufferedReader().readText()
                } ?: "Could not read the file."
            } else {
                "Today's journal not found."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error reading journal: ${e.message}"
        }
    }

    private fun getJournalPath(context: Context): String? {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        return prefs.getString("journal_path_global", null)
    }
}