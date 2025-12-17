package com.dara.logseqjournalwidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class WidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetRemoteViewsFactory(this.applicationContext)
    }
}

class WidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var journalLines: List<String> = emptyList()
    private var tagColor: Int = Color.WHITE

    override fun onCreate() {
        // Initialize
    }

    override fun onDataSetChanged() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        tagColor = prefs.getInt("tag_color_hex", ContextCompat.getColor(context, R.color.default_tag_color))
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
                finalLine = "$indentation• $content"
            } else {
                finalLine = line
            }

            val styledText = applyStyling(finalLine)
            setTextViewText(R.id.list_item_text, styledText)

            // Create an Intent to launch the Logseq app
            val logseqLaunchIntent = context.packageManager.getLaunchIntentForPackage("com.logseq.app")
            if (logseqLaunchIntent != null) {
                val logseqPendingIntent = PendingIntent.getActivity(context, position, logseqLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                setOnClickPendingIntent(R.id.list_item_text, logseqPendingIntent)
            }
        }
    }

    private fun applyStyling(line: String): CharSequence {
        val spannable = SpannableStringBuilder(line)
        // Use raw string """...""" to avoid escaping issues with backslashes.
        // The regex now correctly captures #tags in group 1, and content of [[...]] in group 3.
        val pattern = Pattern.compile("""(#\w+)|(\[\[(.*?)\]\])""", Pattern.DOTALL)
        val matcher = pattern.matcher(line)
        val replacements = mutableListOf<Triple<Int, Int, CharSequence>>()

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            val tagMatch = matcher.group(1)
            val linkContent = matcher.group(3)

            if (tagMatch != null) {
                // It's a #tag, style the whole thing
                val styled = SpannableString(tagMatch)
                styled.setSpan(ForegroundColorSpan(tagColor), 0, styled.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                styled.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, styled.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                replacements.add(Triple(start, end, styled))
            } else if (linkContent != null) {
                // It's a [[link]], style only the content inside
                val styled = SpannableString(linkContent)
                styled.setSpan(ForegroundColorSpan(tagColor), 0, styled.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                styled.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, styled.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                // Replace the whole [[content]] with just the styled content
                replacements.add(Triple(start, end, styled))
            }
        }

        // Apply replacements in reverse order to avoid index shifts
        for ((start, end, replacement) in replacements.asReversed()) {
            spannable.replace(start, end, replacement)
        }

        return spannable
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