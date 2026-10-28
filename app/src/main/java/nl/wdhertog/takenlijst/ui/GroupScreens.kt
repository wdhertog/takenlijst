package nl.wdhertog.takenlijst.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import nl.wdhertog.takenlijst.data.TaskGroup
import nl.wdhertog.takenlijst.ui.theme.App
import nl.wdhertog.takenlijst.ui.theme.bg
import nl.wdhertog.takenlijst.ui.theme.fg
import nl.wdhertog.takenlijst.ui.theme.groupBg
import nl.wdhertog.takenlijst.ui.theme.groupFg
import java.util.UUID

/** Overzicht van de groepen: volgorde aanpassen, bewerken en (tot het maximum) toevoegen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(vm: AppViewModel, onEdit: (String?) -> Unit, onBack: () -> Unit) {
    val groups = LocalGroups.current
    val tasks by vm.tasks.collectAsState()
    val counts = tasks.orEmpty().groupingBy { it.groupId }.eachCount()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Groepen", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Deel je taken in zoals het bij jou past, bijvoorbeeld Huis, Tuin, Werk, Kinderen of Vereniging. " +
                    "Je kunt er hooguit ${TaskGroup.MAX} hebben; de volgorde is ook die van de filters op Vandaag.",
                style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
            )
            Column(Modifier.clip(RoundedCornerShape(18.dp)).background(App.colors.card)) {
                groups.forEachIndexed { i, g ->
                    if (i > 0) HorizontalDivider(color = App.colors.line)
                    Row(
                        Modifier.fillMaxWidth().clickable { onEdit(g.id) }.heightIn(min = 64.dp).padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(g.bg), contentAlignment = Alignment.Center) {
                            DrawableIcon(g.icon(), Modifier.size(22.dp), tint = g.fg)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium)
                            val n = counts[g.id] ?: 0
                            Text(if (n == 1) "1 taak" else "$n taken", style = MaterialTheme.typography.bodySmall, color = App.colors.muted)
                        }
                        IconButton(onClick = { vm.moveGroup(g.id, -1) }, enabled = i > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, "${g.name} omhoog")
                        }
                        IconButton(onClick = { vm.moveGroup(g.id, 1) }, enabled = i < groups.lastIndex) {
                            Icon(Icons.Default.KeyboardArrowDown, "${g.name} omlaag")
                        }
                    }
                }
            }
            if (groups.size < TaskGroup.MAX) {
                OutlinedButton(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.Add, null)
                    Text("  Groep toevoegen", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text("Je hebt het maximum van ${TaskGroup.MAX} groepen.", style = MaterialTheme.typography.bodySmall, color = App.colors.muted)
            }
        }
    }
}

/** Naam, kleur en icoon van een groep instellen; [groupId] null is een nieuwe groep. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditScreen(
    vm: AppViewModel,
    groupId: String?,
    onPickIcon: (current: String, color: Int) -> Unit,
    pickedIcon: String?,
    onPickedIconHandled: () -> Unit,
    onDone: () -> Unit,
) {
    val groups = LocalGroups.current
    val tasks by vm.tasks.collectAsState()
    val original = groups.firstOrNull { it.id == groupId }
    // Een nieuwe groep krijgt een kleur die nog niet gebruikt wordt.
    val freeColor = (0 until App.colors.palette.size).firstOrNull { c -> groups.none { it.color == c } } ?: 0

    var name by rememberSaveable { mutableStateOf(original?.name ?: "") }
    var color by rememberSaveable { mutableIntStateOf(original?.color ?: freeColor) }
    var icon by rememberSaveable { mutableStateOf(original?.icon ?: "huis") }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(pickedIcon) {
        if (pickedIcon != null) {
            if (pickedIcon.isNotEmpty()) icon = pickedIcon
            onPickedIconHandled()
        }
    }

    fun save() {
        vm.saveGroup(
            TaskGroup(
                id = original?.id ?: UUID.randomUUID().toString(),
                name = name.trim(), icon = icon, color = color,
                position = original?.position ?: groups.size,
            )
        )
        onDone()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (original == null) "Nieuwe groep" else "Groep bewerken", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                actions = {
                    if (original != null && groups.size > 1) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Verwijderen") }
                    Button(onClick = ::save, enabled = name.isNotBlank(), modifier = Modifier.padding(end = 8.dp)) { Text("Opslaan") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        ) {
            // Voorbeeld zoals de groep in de app verschijnt.
            Row(
                Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(App.colors.card).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(groupBg(color)), contentAlignment = Alignment.Center) {
                    DrawableIcon(TaskIcon.byKey(icon)?.drawable ?: nl.wdhertog.takenlijst.R.drawable.ic_home, Modifier.size(26.dp), tint = groupFg(color))
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    name.ifBlank { "Naam van de groep" },
                    style = MaterialTheme.typography.labelLarge, color = groupFg(color),
                    modifier = Modifier.clip(CircleShape).background(groupBg(color)).padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            SectionHeader("Naam")
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(20) },
                placeholder = { Text("Bijv. Kinderen") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = App.colors.card, unfocusedContainerColor = App.colors.card,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader("Kleur")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                App.colors.palette.indices.forEach { c ->
                    val selected = c == color
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(groupBg(c))
                            .clickable(role = Role.RadioButton) { color = c }
                            .semantics { this.selected = selected; contentDescription = "Kleur ${c + 1}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(groupFg(c)), contentAlignment = Alignment.Center) {
                            if (selected) Icon(Icons.Default.Check, null, tint = groupBg(c), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            SectionHeader("Icoon")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(App.colors.card)
                    .clickable { onPickIcon(icon, color) }.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(groupBg(color)), contentAlignment = Alignment.Center) {
                    DrawableIcon(TaskIcon.byKey(icon)?.drawable ?: nl.wdhertog.takenlijst.R.drawable.ic_home, Modifier.size(22.dp), tint = groupFg(color))
                }
                Text(
                    TaskIcon.byKey(icon)?.label ?: "Geen icoon", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                Text("Wijzigen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (confirmDelete && original != null) {
        val others = groups.filter { it.id != original.id }
        val count = tasks.orEmpty().count { it.groupId == original.id }
        var target by remember { mutableStateOf(others.first().id) }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("“${original.name}” verwijderen?") },
            text = {
                Column {
                    if (count == 0) Text("Er zitten geen taken in deze groep.")
                    else {
                        Text(if (count == 1) "De taak in deze groep gaat naar:" else "De $count taken in deze groep gaan naar:")
                        others.forEach { g ->
                            Row(
                                Modifier.fillMaxWidth().selectable(selected = target == g.id, onClick = { target = g.id }, role = Role.RadioButton).padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = target == g.id, onClick = null)
                                Text(g.name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteGroup(original.id, target)
                    confirmDelete = false
                    onDone()
                }) { Text("Verwijderen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") } },
        )
    }
}
