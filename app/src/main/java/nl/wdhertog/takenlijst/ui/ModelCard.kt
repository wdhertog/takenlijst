package nl.wdhertog.takenlijst.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import nl.wdhertog.takenlijst.models.Model
import nl.wdhertog.takenlijst.models.ModelManager
import nl.wdhertog.takenlijst.ui.theme.App

/** Status van een downloadbaar model, met knoppen om het te downloaden of te verwijderen. */
@Composable
fun ModelCard(model: Model, explanation: String, allowDelete: Boolean = false) {
    val context = LocalContext.current
    val progress by ModelManager.progress.collectAsState()
    val error by ModelManager.error.collectAsState()
    ModelManager.changes.collectAsState().value // opnieuw tekenen na download of verwijderen
    val downloaded = ModelManager.isDownloaded(context, model)
    val running = progress[model]

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(App.colors.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(model.label, style = MaterialTheme.typography.titleMedium)
        Text(explanation, style = MaterialTheme.typography.bodyMedium, color = App.colors.muted)
        when {
            running != null -> {
                LinearProgressIndicator(progress = { running }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                Text("Downloaden… ${(running * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = App.colors.muted)
            }
            downloaded -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Geïnstalleerd · werkt offline", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                if (allowDelete) TextButton(onClick = { ModelManager.delete(context, model) }) { Text("Verwijderen") }
            }
            else -> Button(onClick = { ModelManager.download(context, model) }) { Text("Downloaden (~${model.sizeMb} MB)") }
        }
        if (error != null && running == null && !downloaded) {
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
