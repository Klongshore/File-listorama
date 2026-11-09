package com.klongshore.filelistorama.ui.access

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Shows [content] once All files access is granted; otherwise explains and asks for it. */
@Composable
fun StorageAccessGate(
    modifier: Modifier = Modifier,
    viewModel: StorageAccessViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val access by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val request = {
        viewModel.requestAccess()
        openAllFilesAccessSettings(context)
    }

    when (access) {
        StorageAccess.Granted -> content()
        StorageAccess.Explaining -> Message(
            modifier = modifier,
            title = "Allow access to your files",
            body = "File-listorama needs All files access to list the files and folders on " +
                "your device. You will be taken to Android settings to turn it on.",
            action = "Open settings",
            onAction = request,
        )
        StorageAccess.Denied -> Message(
            modifier = modifier,
            title = "Access not granted",
            body = "Without All files access the app cannot show your files. " +
                "You can try again whenever you are ready.",
            action = "Try again",
            onAction = request,
        )
    }
}

@Composable
private fun Message(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = onAction) { Text(action) }
    }
}

private fun openAllFilesAccessSettings(context: Context) {
    val appIntent = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        Uri.fromParts("package", context.packageName, null),
    )
    try {
        context.startActivity(appIntent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
}
