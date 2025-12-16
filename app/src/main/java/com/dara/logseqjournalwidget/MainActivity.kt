package com.dara.logseqjournalwidget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dara.logseqjournalwidget.ui.theme.LogseqJournalWidgetTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectDirLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Persist read permissions for the selected URI.
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                saveJournalPath(this, uri)
                // Recreate the activity to update the path text on screen.
                recreate()
            }
        }

        setContent {
            LogseqJournalWidgetTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(this) { selectDirLauncher.launch(null) }
                }
            }
        }
    }
}

@Composable
fun MainScreen(context: Context, onSelectClick: () -> Unit) {
    val savedPath = getJournalPath(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Logseq Journal Directory", style = MaterialTheme.typography.headlineSmall)
        Text("Selected Path: ${savedPath ?: "None"}", modifier = Modifier.padding(16.dp))
        Button(onClick = onSelectClick) {
            Text("Select Directory")
        }
    }
}

private fun saveJournalPath(context: Context, uri: Uri) {
    val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
    // Save the path to be used globally by the app.
    prefs.putString("journal_path_global", uri.toString())
    prefs.apply()
}

fun getJournalPath(context: Context): String? {
    val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    return prefs.getString("journal_path_global", null)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LogseqJournalWidgetTheme {
        MainScreen(context = LocalContext.current) {}
    }
}