package nl.wdhertog.takenlijst.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.data.ShoppingItem
import nl.wdhertog.takenlijst.domain.Aisle
import nl.wdhertog.takenlijst.ui.theme.App

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingScreen(vm: AppViewModel, snackbar: SnackbarHostState, contentPadding: PaddingValues, onScan: () -> Unit) {
    val items by vm.shopping.collectAsState()
    val shopMode by vm.shopMode.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    val scope = rememberCoroutineScope()
    val done = items.count { it.checked }

    // In winkelmodus blijft het scherm aan.
    val activity = LocalContext.current as? Activity
    DisposableEffect(shopMode) {
        if (shopMode) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    fun add() {
        if (input.isNotBlank()) vm.addShopping(input)
        input = ""
    }

    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Boodschappen", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = shopMode,
                    onClick = { vm.shopMode.value = !shopMode },
                    label = { Text("Winkelmodus") },
                    leadingIcon = { DrawableIcon(R.drawable.ic_cart, Modifier.size(16.dp), tint = if (shopMode) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = App.colors.card,
                        selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
            if (items.isNotEmpty()) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { done / items.size.toFloat() },
                        modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                    Text("  $done / ${items.size}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            if (!shopMode) {
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        placeholder = { Text("Product toevoegen, bijv. 2 melk") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { add() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = App.colors.card, unfocusedContainerColor = App.colors.card,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = onScan, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp)) {
                        DrawableIcon(R.drawable.ic_camera, tint = MaterialTheme.colorScheme.onPrimary, description = "Lijstje van foto lezen")
                    }
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            if (items.isEmpty()) {
                item {
                    Text(
                        "Je lijst is leeg. Typ hierboven een product, of maak een foto van een briefje.",
                        style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
                        modifier = Modifier.padding(top = 24.dp, start = 4.dp),
                    )
                }
            }
            items.byAisle().forEach { (aisle, list) ->
                val left = list.count { !it.checked }
                item(key = "h${aisle.name}") {
                    SectionHeader(aisle.label, trailing = if (left == 0) "alles in mandje" else "$left over", modifier = Modifier.animateItem())
                }
                item(key = "c${aisle.name}") {
                    Column(Modifier.animateItem().clip(RoundedCornerShape(18.dp)).background(App.colors.card)) {
                        list.forEachIndexed { i, item ->
                            if (i > 0) HorizontalDivider(color = App.colors.line)
                            ShoppingRow(item, big = shopMode, onToggle = { vm.toggle(item) }, onLongPress = { editing = item })
                        }
                    }
                }
            }
        }

        if (done > 0) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).padding(bottom = contentPadding.calculateBottomPadding())) {
                Button(
                    onClick = {
                        scope.launch {
                            val ticked = vm.finishShopping()
                            vm.shopMode.value = false
                            snackbar.showSnackbar(if (ticked) "Boodschappen gedaan en afgevinkt" else "Afgevinkte producten opgeruimd")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.Check, null)
                    Text("  Klaar · $done uit de lijst halen", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }
    }

    editing?.let { item -> EditItemDialog(item, onDismiss = { editing = null }, onSave = { vm.updateShopping(it); editing = null }, onDelete = { vm.deleteShopping(item); editing = null }) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingRow(item: ShoppingItem, big: Boolean, onToggle: () -> Unit, onLongPress: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(role = Role.Checkbox, onClick = onToggle, onLongClick = onLongPress, onLongClickLabel = "Bewerken")
            .semantics { stateDescription = if (item.checked) "In mandje" else "Nog halen" }
            .heightIn(min = if (big) 60.dp else 52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(8.dp))
                .background(if (item.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            contentAlignment = Alignment.Center,
        ) {
            if (item.checked) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            else Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(App.colors.card))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            item.name,
            style = if (big) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.titleMedium,
            color = if (item.checked) App.colors.muted else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        if (item.quantity.isNotBlank()) Text(item.quantity, style = MaterialTheme.typography.bodyMedium, color = App.colors.muted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemDialog(item: ShoppingItem, onDismiss: () -> Unit, onSave: (ShoppingItem) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity) }
    var aisle by remember { mutableStateOf(item.aisle) }
    var open by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product bewerken") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Naam") }, singleLine = true)
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Hoeveelheid") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
                    OutlinedTextField(
                        aisle.label, {}, readOnly = true, label = { Text("Afdeling") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        Aisle.entries.forEach { a -> DropdownMenuItem(text = { Text(a.label) }, onClick = { aisle = a; open = false }) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(item.copy(name = name.trim(), quantity = quantity.trim(), aisle = aisle)) }) { Text("Opslaan") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("Verwijderen", color = MaterialTheme.colorScheme.error) }
        },
    )
}
