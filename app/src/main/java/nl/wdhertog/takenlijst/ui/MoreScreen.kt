package nl.wdhertog.takenlijst.ui

import androidx.compose.ui.Alignment
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import nl.wdhertog.takenlijst.BuildConfig
import nl.wdhertog.takenlijst.models.Model
import nl.wdhertog.takenlijst.ui.theme.App
import nl.wdhertog.takenlijst.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(vm: AppViewModel, contentPadding: PaddingValues, onGroups: () -> Unit, onHistory: () -> Unit) {
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

        Card("Gedaan") {
            Text(
                "Alles wat je afvinkte, per dag. Opschonen kan per taak, in het bewerkscherm onder Geschiedenis.",
                style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
            )
            OutlinedButton(onClick = onHistory) { Text("Bekijk wat je gedaan hebt") }
        }

        Card("Weergave") {
            val mode by vm.themeMode.collectAsState()
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = m == mode,
                        onClick = { vm.setThemeMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) { Text(m.label) }
                }
            }
        }

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

        Card("Groepen") {
            val groups = LocalGroups.current
            Text(
                groups.joinToString(", ") { it.name } + " · je kunt er tot ${nl.wdhertog.takenlijst.data.TaskGroup.MAX} instellen, met eigen naam, kleur en icoon.",
                style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
            )
            OutlinedButton(onClick = onGroups) { Text("Groepen beheren") }
        }

        Card("Barcodes") {
            val lookup by vm.productLookup.collectAsState()
            val known by vm.productCount.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Onbekende producten opzoeken via Open Food Facts", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = lookup, onCheckedChange = vm::setProductLookup)
            }
            Text(
                "Alleen het barcodenummer wordt verstuurd. Gescande producten en je keuze tussen ruim en precies " +
                    "worden op je telefoon onthouden" + if (known > 0) " ($known nu)." else ".",
                style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
            )
            if (known > 0) OutlinedButton(onClick = vm::forgetProducts) { Text("Onthouden producten vergeten") }
        }

        SectionHeader("Spraak")
        ModelCard(Model.SPEECH, "Voor het inspreken van boodschappen. Werkt na downloaden zonder internet.", allowDelete = true)

        Card("Voorbeelden") {
            Text(
                "Voeg de voorbeeldtaken (honden uitlaten, heg snoeien, kippenvoer…) opnieuw toe.",
                style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
            )
            OutlinedButton(onClick = { vm.addExamples() }) { Text("Voorbeeldtaken toevoegen") }
        }

        Card("Over deze app") {
            Text(
                "Takenlijst ${BuildConfig.VERSION_NAME} · vrije software onder GPL-3.0\n" +
                    "Alles blijft op je telefoon. Internet wordt alleen gebruikt om het spraakmodel te downloaden en, als je dat aanzet, om barcodes op te zoeken.\n\n" +
                    "Gebruikt Tesseract en Tesseract4Android (Apache 2.0) met het Nederlandse taalmodel uit tessdata_fast (Apache 2.0), " +
                    "whisper.cpp (MIT) met het Whisper-model van OpenAI (MIT), " +
                    "ZXing en zxing-android-embedded (Apache 2.0), productgegevens van Open Food Facts (ODbL), " +
                    "iconen van Lucide (ISC) en het lettertype Figtree (SIL Open Font License).",
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
