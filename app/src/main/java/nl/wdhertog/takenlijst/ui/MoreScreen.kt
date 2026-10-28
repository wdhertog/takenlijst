package nl.wdhertog.takenlijst.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import nl.wdhertog.takenlijst.BuildConfig
import nl.wdhertog.takenlijst.models.Model
import nl.wdhertog.takenlijst.ui.theme.App

@Composable
fun MoreScreen(vm: AppViewModel, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val notificationsOff = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Meer", style = MaterialTheme.typography.headlineLarge)

        Card("Meldingen") {
            Text(
                if (notificationsOff) "Meldingen staan uit, dus herinneringen komen niet door."
                else "Herinneringen komen op de ingestelde tijd, soms een paar minuten later als je telefoon slaapt.",
                style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
            )
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }) { Text("Meldingsinstellingen openen") }
        }

        SectionHeader("Spraak en handschrift")
        ModelCard(Model.SPEECH, "Voor het inspreken van boodschappen. Werkt na downloaden zonder internet.", allowDelete = true)
        ModelCard(Model.HANDWRITING, "Voor het lezen van handgeschreven lijstjes. Werkt na downloaden zonder internet.", allowDelete = true)

        Card("Voorbeelden") {
            Text(
                "Voeg de voorbeeldtaken (honden uitlaten, heg snoeien, kippenvoer…) opnieuw toe.",
                style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
            )
            OutlinedButton(onClick = { vm.addExamples() }) { Text("Voorbeeldtaken toevoegen") }
        }

        Card("Over deze app") {
            Text(
                "Takenlijst ${BuildConfig.VERSION_NAME}\n" +
                    "Alles blijft op je telefoon. Internet wordt alleen gebruikt om de spraak- en handschriftmodellen eenmalig te downloaden.\n\n" +
                    "Gebruikt Tesseract en Tesseract4Android (Apache 2.0) met het Nederlandse taalmodel uit tessdata_fast (Apache 2.0), " +
                    "whisper.cpp (MIT) met het Whisper-model van OpenAI (MIT), TrOCR van Microsoft (MIT) via ONNX Runtime (MIT), " +
                    "en het lettertype Figtree (SIL Open Font License).",
                style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
            )
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(App.colors.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
