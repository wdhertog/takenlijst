package nl.wdhertog.takenlijst.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.domain.Aisle
import nl.wdhertog.takenlijst.ui.theme.App
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(vm: AppViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val state by vm.scan.collectAsState()
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        photoUri?.takeIf { ok }?.let(vm::startScan)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::startScan)
    }

    fun takePhoto() {
        val file = File(context.cacheDir, "scan/foto.jpg").apply { parentFile?.mkdirs() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        photoUri = uri
        camera.launch(uri)
    }

    fun close() {
        vm.resetScan()
        onDone()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Lijstje van foto", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
            when (val s = state) {
                ScanState.Idle -> Intro(onCamera = ::takePhoto, onGallery = {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
                ScanState.Working -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Tekst herkennen…", style = MaterialTheme.typography.titleMedium)
                        Text("Dit gebeurt op je telefoon", style = MaterialTheme.typography.bodySmall, color = App.colors.muted)
                    }
                }
                is ScanState.Failed -> Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(s.message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = ::takePhoto, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Nieuwe foto maken") }
                    OutlinedButton(onClick = vm::resetScan, modifier = Modifier.fillMaxWidth()) { Text("Terug") }
                }
                is ScanState.Result -> Results(s, vm, onAdd = {
                    vm.commitScan()
                    onDone()
                }, onRetry = ::takePhoto)
            }
        }
    }
}

@Composable
private fun Intro(onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { DrawableIcon(R.drawable.ic_camera, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Text("Maak een foto van je boodschappenbriefje", style = MaterialTheme.typography.titleLarge)
        Text(
            "De tekst wordt op je telefoon herkend; de foto gaat nergens heen. Gedrukte tekst en duidelijk " +
                "handschrift werken het best: leg het briefje plat, in goed licht, en houd de telefoon recht erboven.",
            style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onCamera, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
            DrawableIcon(R.drawable.ic_camera, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
            Text("  Foto maken", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
            DrawableIcon(R.drawable.ic_image, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text("  Kies uit galerij", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Results(s: ScanState.Result, vm: AppViewModel, onAdd: () -> Unit, onRetry: () -> Unit) {
    val selected = s.rows.count { it.selected && it.name.isNotBlank() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${s.rows.size} GEVONDEN", style = MaterialTheme.typography.labelSmall, color = App.colors.muted, modifier = Modifier.weight(1f))
            Text("Pas aan waar nodig", style = MaterialTheme.typography.labelMedium, color = App.colors.muted)
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
            items(s.rows, key = { it.key }) { row ->
                val colors = App.colors
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (row.uncertain) colors.lateBg else colors.card)
                        .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
                ) {
                    if (row.uncertain) {
                        Text(
                            "Klopt dit? Er stond “${row.seen}”",
                            style = MaterialTheme.typography.labelMedium, color = colors.lateFg,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = row.selected,
                            onCheckedChange = { vm.editScanRow(row.copy(selected = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = if (row.uncertain) colors.lateFg else MaterialTheme.colorScheme.primary),
                        )
                        OutlinedTextField(
                            row.name, { vm.editScanRow(row.copy(name = it)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = if (row.uncertain) colors.lateRing else colors.card,
                                focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            row.quantity, { vm.editScanRow(row.copy(quantity = it)) },
                            singleLine = true, placeholder = { Text("aantal") },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = colors.line,
                                focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                            ),
                            modifier = Modifier.width(84.dp),
                        )
                    }
                    Text(
                        Aisle.guess(row.name).label,
                        style = MaterialTheme.typography.labelMedium, color = colors.muted,
                        modifier = Modifier.padding(start = 48.dp, top = 2.dp),
                    )
                }
            }
        }
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(18.dp)) { Text("Opnieuw") }
            Button(onClick = onAdd, enabled = selected > 0, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(18.dp)) {
                Text("$selected toevoegen aan lijst", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
