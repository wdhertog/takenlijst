package nl.wdhertog.takenlijst.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import nl.wdhertog.takenlijst.ui.theme.App

/**
 * Na het scannen van een barcode: kiezen tussen de precieze naam ("AH Halfvolle melk 1 L") en de
 * ruime ("Melk"). Beide zijn aan te passen; de keuze wordt bij de barcode onthouden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeSheet(
    state: BarcodeState,
    onLookup: (always: Boolean) -> Unit,
    onAdd: (specific: String, broad: String, useBroad: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    if (state == BarcodeState.Idle) return
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().imePadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Op je lijstje zetten", style = MaterialTheme.typography.titleLarge)
            when (state) {
                is BarcodeState.Looking -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.padding(4.dp))
                        Text("Barcode ${state.barcode} opzoeken…", style = MaterialTheme.typography.bodyMedium, color = App.colors.muted)
                    }
                }
                is BarcodeState.Choose -> ChooseNames(state, onLookup, onAdd)
                BarcodeState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun ChooseNames(
    state: BarcodeState.Choose,
    onLookup: (always: Boolean) -> Unit,
    onAdd: (specific: String, broad: String, useBroad: Boolean) -> Unit,
) {
    var specific by remember(state) { mutableStateOf(state.specific) }
    var broad by remember(state) { mutableStateOf(state.broad) }
    var useBroad by remember(state) { mutableStateOf(state.useBroad) }

    Text("${state.note}\nBarcode ${state.barcode}", style = MaterialTheme.typography.bodyMedium, color = App.colors.muted)

    if (state.canLookup) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onLookup(false) }) { Text("Eén keer opzoeken") }
            OutlinedButton(onClick = { onLookup(true) }) { Text("Altijd opzoeken") }
        }
        Text(
            "Opzoeken gaat via Open Food Facts; alleen het barcodenummer wordt verstuurd.",
            style = MaterialTheme.typography.bodySmall, color = App.colors.muted,
        )
    }

    NameOption("Ruim", "Zoals je het op een briefje zou schrijven", broad, { broad = it }, "Bijv. Melk", selected = useBroad) { useBroad = true }
    NameOption("Precies", "Met merk en inhoud", specific, { specific = it }, "Bijv. AH Halfvolle melk 1 L", selected = !useBroad) { useBroad = false }

    val chosen = (if (useBroad) broad.ifBlank { specific } else specific.ifBlank { broad }).trim()
    Button(
        onClick = { onAdd(specific, broad, useBroad) },
        enabled = chosen.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(if (chosen.isEmpty()) "Vul een naam in" else "“$chosen” toevoegen", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun NameOption(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = App.colors.card,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
    ) {
        Column(Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = App.colors.muted)
                }
            }
            Box(Modifier.padding(start = 12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it); onSelect() },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
