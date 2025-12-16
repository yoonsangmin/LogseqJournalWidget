package com.dara.logseqjournalwidget

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
            setTextViewText(R.id.list_item_text, line)
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
}