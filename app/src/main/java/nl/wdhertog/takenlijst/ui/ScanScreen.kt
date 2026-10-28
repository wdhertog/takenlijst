package nl.wdhertog.takenlijst.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.domain.Aisle
import nl.wdhertog.takenlijst.models.Model
import nl.wdhertog.takenlijst.models.ModelManager
import nl.wdhertog.takenlijst.ui.theme.App
import java.io.File

/** Producten toevoegen van een foto ([voice] = false) of door in te spreken ([voice] = true). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(vm: AppViewModel, voice: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    val state by vm.scan.collectAsState()
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        photoUri?.takeIf { ok }?.let(vm::startScan)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::startScan)
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording()
    }

    fun takePhoto() {
        val file = File(context.cacheDir, "scan/foto.jpg").apply { parentFile?.mkdirs() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        photoUri = uri
        camera.launch(uri)
    }

    fun again() {
        if (voice) {
            vm.resetScan()
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else takePhoto()
    }

    fun close() {
        vm.stopRecording()
        vm.resetScan()
        onDone()
    }

    // Opname stoppen als het scherm verdwijnt.
    DisposableEffect(Unit) { onDispose { vm.stopRecording() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (voice) "Inspreken" else "Lijstje van foto", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
            when (val s = state) {
                ScanState.Idle ->
                    if (voice) VoiceIntro(onStart = { micPermission.launch(Manifest.permission.RECORD_AUDIO) })
                    else PhotoIntro(onCamera = ::takePhoto, onGallery = {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                ScanState.Recording -> Listening(vm)
                is ScanState.Working -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(s.message, style = MaterialTheme.typography.titleMedium)
                        Text("Dit gebeurt op je telefoon", style = MaterialTheme.typography.bodySmall, color = App.colors.muted)
                    }
                }
                is ScanState.Failed -> Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(s.message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = ::again, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(if (voice) "Opnieuw inspreken" else "Nieuwe foto maken")
                    }
                    OutlinedButton(onClick = vm::resetScan, modifier = Modifier.fillMaxWidth()) { Text("Terug") }
                }
                is ScanState.Result -> Results(s, vm, voice, onAdd = {
                    vm.commitScan()
                    onDone()
                }, onRetry = ::again)
            }
        }
    }
}

@Composable
private fun PhotoIntro(onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { DrawableIcon(R.drawable.ic_camera, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Text("Foto van een gedrukt lijstje", style = MaterialTheme.typography.titleLarge)
        Text(
            "Werkt met getypte of gedrukte tekst, zoals een uitgeprint lijstje of een recept. " +
                "De tekst wordt op je telefoon herkend; de foto gaat nergens heen. Voor een handgeschreven briefje kun je het beter inspreken.",
            style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
        )
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
private fun VoiceIntro(onStart: () -> Unit) {
    val context = LocalContext.current
    ModelManager.changes.collectAsState().value
    val ready = ModelManager.isDownloaded(context, Model.SPEECH)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Zeg wat je nodig hebt", style = MaterialTheme.typography.titleLarge)
        Text(
            "Bijvoorbeeld: “melk, twee pakken yoghurt, tien kilo hondenbrokken en appels”. " +
                "Daarna kun je alles nog nalopen voordat het op de lijst komt.",
            style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
        )
        if (!ready) {
            ModelCard(Model.SPEECH, "Eenmalig downloaden; daarna wordt je stem op je telefoon uitgeschreven, zonder internet.")
        } else {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(onClick = onStart, shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(120.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        DrawableIcon(R.drawable.ic_mic, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimary, description = "Start inspreken")
                    }
                }
            }
            Text("Tik om te beginnen", style = MaterialTheme.typography.labelLarge, color = App.colors.muted, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun Listening(vm: AppViewModel) {
    val level by vm.recorder.level.collectAsState()
    val scale by animateFloatAsState(1f + level * 0.35f, label = "niveau")
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(150.dp).scale(scale).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
            Surface(onClick = vm::stopRecording, shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(120.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.onPrimary))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Ik luister…", style = MaterialTheme.typography.titleLarge)
        Text("Tik op de knop als je klaar bent", style = MaterialTheme.typography.bodyMedium, color = App.colors.muted)
    }
}

@Composable
private fun Results(s: ScanState.Result, vm: AppViewModel, voice: Boolean, onAdd: () -> Unit, onRetry: () -> Unit) {
    val selected = s.rows.count { it.selected && it.name.isNotBlank() }
    Column(Modifier.fillMaxSize()) {
        if (s.heard != null) {
            Text(
                "“${s.heard}”",
                style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = App.colors.muted,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
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
