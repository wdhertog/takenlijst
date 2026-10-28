package nl.wdhertog.takenlijst.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.data.Task
import nl.wdhertog.takenlijst.data.TaskGroup
import nl.wdhertog.takenlijst.domain.Recurrence
import nl.wdhertog.takenlijst.domain.Repeat
import nl.wdhertog.takenlijst.domain.RepeatUnit
import nl.wdhertog.takenlijst.ui.theme.App
import java.time.DayOfWeek
import java.time.LocalDate

private enum class Preset(val label: String, val repeat: Repeat?) {
    DAILY("Dagelijks", Repeat.DAILY),
    WEEKLY("Wekelijks", Repeat.WEEKLY),
    MONTHLY("Maandelijks", Repeat.MONTHLY),
    QUARTERLY("Per kwartaal", Repeat.QUARTERLY),
    YEARLY("Jaarlijks", Repeat.YEARLY),
    ON_DEMAND("Op afroep", Repeat.ON_DEMAND),
    CUSTOM("Aangepast…", null),
}

private fun presetOf(r: Repeat) = when {
    r.unit == RepeatUnit.NONE -> Preset.ON_DEMAND
    r.every == 1 && r.unit == RepeatUnit.DAY -> Preset.DAILY
    r.every == 1 && r.unit == RepeatUnit.WEEK -> Preset.WEEKLY
    r.every == 1 && r.unit == RepeatUnit.MONTH -> Preset.MONTHLY
    r.every == 3 && r.unit == RepeatUnit.MONTH -> Preset.QUARTERLY
    r.every == 1 && r.unit == RepeatUnit.YEAR -> Preset.YEARLY
    else -> Preset.CUSTOM
}

private val unitLabels = listOf(RepeatUnit.DAY to "dagen", RepeatUnit.WEEK to "weken", RepeatUnit.MONTH to "maanden", RepeatUnit.YEAR to "jaar")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    vm: AppViewModel,
    taskId: Long,
    onDone: () -> Unit,
    /** Opent de iconenkiezer met het huidige icoon, de naam (voor "automatisch") en de groep (voor de kleur). */
    onPickIcon: (current: String, title: String, group: TaskGroup) -> Unit,
    /** Uitkomst van de iconenkiezer, of null als er niets gekozen is. */
    pickedIcon: String?,
    onPickedIconHandled: () -> Unit,
) {
    val context = LocalContext.current
    val today by vm.currentDate.collectAsState()
    var original by remember { mutableStateOf<Task?>(null) }
    var loaded by rememberSaveable { mutableStateOf(taskId == 0L) }
    var lastDone by remember { mutableStateOf<LocalDate?>(null) }

    var title by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf(TaskGroup.HOME) }
    var preset by rememberSaveable { mutableStateOf(Preset.WEEKLY) }
    var customUnit by rememberSaveable { mutableStateOf(RepeatUnit.WEEK) }
    var customEvery by rememberSaveable { mutableIntStateOf(2) }
    var weekdays by rememberSaveable { mutableIntStateOf(0) }
    var dueEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var reminder by rememberSaveable { mutableStateOf<Int?>(null) }
    var fromCompletion by rememberSaveable { mutableStateOf(false) }
    var linkShopping by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    var timesPerDay by rememberSaveable { mutableIntStateOf(1) }
    var slotTimes by rememberSaveable { mutableStateOf("") }
    var pickSlot by remember { mutableStateOf<Int?>(null) }
    var icon by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(pickedIcon) {
        if (pickedIcon != null) {
            icon = pickedIcon
            onPickedIconHandled()
        }
    }

    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId != 0L && !loaded) {
            vm.task(taskId)?.let { t ->
                original = t
                title = t.title; group = t.group
                preset = presetOf(t.repeat)
                if (preset == Preset.CUSTOM) { customUnit = t.repeatUnit; customEvery = t.repeatEvery }
                weekdays = t.weekdays
                dueEpoch = (t.nextDue ?: today).toEpochDay()
                reminder = t.reminderMinutes; fromCompletion = t.fromCompletion
                linkShopping = t.linkShopping; note = t.note
                timesPerDay = t.timesPerDay; slotTimes = t.slotTimes
                icon = t.icon
            }
            loaded = true
        } else if (taskId != 0L && original == null) {
            original = vm.task(taskId)
        }
        if (taskId != 0L) lastDone = vm.lastDone(taskId)
    }

    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun currentRepeat(): Repeat {
        val base = preset.repeat ?: Repeat(customUnit, customEvery.coerceAtLeast(1))
        return if (base.unit == RepeatUnit.WEEK) base.copy(weekdays = weekdays) else base.copy(weekdays = 0)
    }

    fun save() {
        val repeat = currentRepeat()
        val picked = LocalDate.ofEpochDay(dueEpoch)
        val due = when {
            !repeat.isOnDemand -> Recurrence.firstDue(repeat, picked)
            original?.repeat?.isOnDemand == true -> original?.nextDue
            else -> null
        }
        val base = original ?: Task(title = "")
        val count = if (repeat.isOnDemand) 1 else timesPerDay
        val times = slotList(slotTimes, count)
        vm.save(
            base.copy(
                title = title.trim(), group = group,
                repeatUnit = repeat.unit, repeatEvery = repeat.every, weekdays = repeat.weekdays,
                nextDue = due,
                // Bij meerdere keren per dag is de herinnering een aan/uit-schakelaar en gelden de tijden per keer.
                reminderMinutes = if (count > 1 && reminder != null) times.first() else reminder,
                fromCompletion = fromCompletion, linkShopping = linkShopping, note = note.trim(),
                timesPerDay = count, slotTimes = if (count > 1) times.joinToString(",") else "",
                slotsDone = base.slotsDone.coerceAtMost(count - 1),
                icon = icon,
            )
        )
        onDone()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == 0L) "Nieuwe taak" else "Taak bewerken", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Default.Close, "Sluiten") } },
                actions = {
                    if (taskId != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Verwijderen") }
                    Button(onClick = ::save, enabled = title.isNotBlank(), modifier = Modifier.padding(end = 8.dp)) { Text("Opslaan") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Label("Wat moet er gebeuren?")
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                placeholder = { Text("Bijv. heg snoeien") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Label("Groep")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskGroup.entries.forEach { g ->
                    val selected = g == group
                    Surface(
                        onClick = { group = g },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) App.colors.groupBg.getValue(g) else App.colors.card,
                        border = BorderStroke(if (selected) 2.dp else 1.5.dp, if (selected) App.colors.groupFg.getValue(g) else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f).height(76.dp).semantics { this.selected = selected },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            DrawableIcon(g.icon(), tint = App.colors.groupFg.getValue(g))
                            Spacer(Modifier.height(6.dp))
                            Text(g.label, style = MaterialTheme.typography.labelLarge, color = App.colors.groupFg.getValue(g))
                        }
                    }
                }
            }

            Label("Icoon")
            val chosen = TaskIcon.byKey(icon)
            val shown = chosen ?: TaskIcon.guess(title)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(App.colors.card)
                    .clickable { onPickIcon(icon, title, group) }
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(App.colors.groupBg.getValue(group)),
                    contentAlignment = Alignment.Center,
                ) { DrawableIcon(shown?.drawable ?: group.icon(), Modifier.size(22.dp), tint = App.colors.groupFg.getValue(group)) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(chosen?.label ?: "Automatisch", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (chosen != null) chosen.category else "Gekozen op basis van de naam",
                        style = MaterialTheme.typography.bodySmall, color = App.colors.muted,
                    )
                }
                Text("Wijzigen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }

            Label("Hoe vaak?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Preset.entries.forEach { p -> Pill(p.label, p == preset) { preset = p } }
            }

            if (preset == Preset.CUSTOM) {
                Row(
                    Modifier.padding(top = 12.dp).clip(RoundedCornerShape(16.dp)).background(App.colors.card).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Elke", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = { customEvery = (customEvery - 1).coerceAtLeast(1) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text("$customEvery", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
                    FilledIconButton(onClick = { customEvery = (customEvery + 1).coerceAtMost(99) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    unitLabels.forEach { (u, label) -> Pill(label, u == customUnit) { customUnit = u } }
                }
            }

            if (!currentRepeat().isOnDemand) {
                Label("Hoe vaak op de dag?")
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(App.colors.card).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledIconButton(
                        onClick = { timesPerDay = (timesPerDay - 1).coerceAtLeast(1); slotTimes = slotList(slotTimes, timesPerDay).joinToString(",") },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) { Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    Text(
                        if (timesPerDay == 1) "1 keer" else "$timesPerDay keer",
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledIconButton(
                        onClick = { timesPerDay = (timesPerDay + 1).coerceAtMost(8); slotTimes = slotList(slotTimes, timesPerDay).joinToString(",") },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) { Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
                if (timesPerDay > 1) {
                    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        slotList(slotTimes, timesPerDay).forEachIndexed { i, m ->
                            Pill("${i + 1}e keer · ${minutesLabel(m)}", false) { pickSlot = i }
                        }
                    }
                }
            }

            if (currentRepeat().unit == RepeatUnit.WEEK) {
                Label("Op welke dagen? (optioneel)")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        val bit = Repeat.bit(d)
                        val on = weekdays and bit != 0
                        Box(
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primary else App.colors.card)
                                .clickable(role = Role.Checkbox) { weekdays = weekdays xor bit }
                                .semantics { selected = on; contentDescription = d.getDisplayName(java.time.format.TextStyle.FULL, Dutch) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                Repeat.SHORT_DAYS[d.value - 1],
                                style = MaterialTheme.typography.labelLarge,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(top = 22.dp).clip(RoundedCornerShape(18.dp)).background(App.colors.card)) {
                val repeat = currentRepeat()
                if (!repeat.isOnDemand) {
                    val shown = Recurrence.firstDue(repeat, LocalDate.ofEpochDay(dueEpoch))!!
                    SettingRow(
                        R.drawable.ic_calendar,
                        if (original == null) "Eerste keer" else "Volgende keer",
                        "Daarna ${repeat.describe().lowercase()}",
                        onClick = { pickDate = true },
                    ) { Text(shown.relativeLabel(today), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                    HorizontalDivider(color = App.colors.line)
                }
                SettingRow(
                    R.drawable.ic_bell, "Herinnering",
                    when {
                        reminder == null -> "Geen melding"
                        timesPerDay > 1 && !repeat.isOnDemand -> "Bij elke keer een melding"
                        else -> "Op de dag zelf om ${minutesLabel(reminder!!)}"
                    },
                    onClick = { if (reminder != null && (timesPerDay == 1 || repeat.isOnDemand)) pickTime = true },
                ) {
                    Switch(checked = reminder != null, onCheckedChange = { on ->
                        reminder = if (on) 9 * 60 else null
                        if (on && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    })
                }
                if (!repeat.isOnDemand) {
                    HorizontalDivider(color = App.colors.line)
                    SettingRow(R.drawable.ic_repeat, "Plannen vanaf afvinken", "Volgende keer telt vanaf wanneer je het deed", onClick = { fromCompletion = !fromCompletion }) {
                        Switch(checked = fromCompletion, onCheckedChange = { fromCompletion = it })
                    }
                }
                HorizontalDivider(color = App.colors.line)
                SettingRow(R.drawable.ic_cart, "Boodschappenlijst", "Opent je lijst en wordt afgevinkt na het winkelen", onClick = { linkShopping = !linkShopping }) {
                    Switch(checked = linkShopping, onCheckedChange = { linkShopping = it })
                }
            }

            Label("Notitie")
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                placeholder = { Text("Bijv. heggenschaar staat in de schuur") },
                minLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            lastDone?.let {
                Text(
                    "Laatst gedaan: ${it.relativeLabel(today).lowercase()}",
                    style = MaterialTheme.typography.bodySmall, color = App.colors.muted,
                    modifier = Modifier.padding(top = 16.dp, start = 4.dp),
                )
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueEpoch * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dueEpoch = it / DAY_MS }
                    pickDate = false
                }) { Text("Kies") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Annuleren") } },
        ) { DatePicker(state) }
    }

    if (pickTime) {
        val start = reminder ?: (9 * 60)
        val state = rememberTimePickerState(start / 60, start % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = { TextButton(onClick = { reminder = state.hour * 60 + state.minute; pickTime = false }) { Text("Kies") } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Annuleren") } },
            text = { TimePicker(state) },
        )
    }

    pickSlot?.let { index ->
        val times = slotList(slotTimes, timesPerDay)
        val start = times[index]
        val state = rememberTimePickerState(start / 60, start % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickSlot = null },
            title = { Text("${index + 1}e keer") },
            confirmButton = {
                TextButton(onClick = {
                    slotTimes = times.toMutableList().apply { this[index] = state.hour * 60 + state.minute }.sorted().joinToString(",")
                    pickSlot = null
                }) { Text("Kies") }
            },
            dismissButton = { TextButton(onClick = { pickSlot = null }) { Text("Annuleren") } },
            text = { TimePicker(state) },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Taak verwijderen?") },
            text = { Text("\"$title\" en de geschiedenis ervan worden verwijderd.") },
            confirmButton = {
                TextButton(onClick = {
                    original?.let(vm::delete)
                    confirmDelete = false
                    onDone()
                }) { Text("Verwijderen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") } },
        )
    }
}

private const val DAY_MS = 86_400_000L

/** Tijden per keer; ontbrekende tijden worden over de dag verdeeld tussen 08:00 en 21:00. */
private fun slotList(stored: String, count: Int): List<Int> {
    val existing = stored.split(',').mapNotNull { it.trim().toIntOrNull() }
    if (existing.size == count) return existing
    if (count <= 1) return listOf(existing.firstOrNull() ?: (9 * 60))
    val spread = List(count) { i -> 8 * 60 + i * (13 * 60) / (count - 1) }.map { it / 15 * 15 }
    return (existing.take(count) + spread.drop(existing.size.coerceAtMost(count))).take(count).sorted()
}

@Composable
private fun Label(text: String) {
    SectionHeader(text, modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = App.colors.card,
    unfocusedContainerColor = App.colors.card,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else App.colors.card,
        border = if (selected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.heightIn(min = 40.dp).semantics { this.selected = selected },
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SettingRow(icon: Int, title: String, subtitle: String, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            DrawableIcon(icon, Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = App.colors.muted)
        }
        trailing()
    }
}
