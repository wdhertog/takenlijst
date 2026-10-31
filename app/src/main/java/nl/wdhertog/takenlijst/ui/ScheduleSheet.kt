package nl.wdhertog.takenlijst.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.data.Task
import nl.wdhertog.takenlijst.ui.theme.App
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Een dag kiezen voor [task]: uitstellen, of een taak "op afroep" inplannen. Snelle keuzes plus een
 * kalender; een ingeplande taak op afroep kan ook weer van de planning af ([onPick] met null).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSheet(task: Task, today: LocalDate, onPick: (LocalDate?) -> Unit, onDismiss: () -> Unit) {
    var pickDate by remember { mutableStateOf(false) }
    val current = task.nextDue
    val postpone = current != null && !current.isAfter(today)
    val saturday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    val options = buildList {
        if (!postpone && current != today) add("Vandaag" to today)
        add("Morgen" to today.plusDays(1))
        add(today.plusDays(2).relativeLabel(today) to today.plusDays(2))
        if (saturday.isAfter(today.plusDays(2))) add("Zaterdag · ${saturday.relativeLabel(today)}" to saturday)
        add("Over een week · ${today.plusWeeks(1).relativeLabel(today)}" to today.plusWeeks(1))
    }.filter { it.second != current }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                when {
                    current == null -> "Inplannen"
                    postpone -> "Uitstellen"
                    else -> "Verplaatsen"
                },
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                task.title, style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
            options.forEach { (label, day) -> SheetRow(R.drawable.ic_calendar, label) { onPick(day) } }
            SheetRow(R.drawable.ic_calendar, "Kies een dag…") { pickDate = true }
            if (task.repeat.isOnDemand && current != null) SheetRow(R.drawable.ic_clock, "Niet meer inplannen") { onPick(null) }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (current?.takeIf { it.isAfter(today) } ?: today.plusDays(1)).toEpochDay() * DAY_MS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MS >= today.toEpochDay()
                override fun isSelectableYear(year: Int) = year >= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickDate = false
                    state.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(it / DAY_MS)) }
                }) { Text("Kies") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Annuleren") } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun SheetRow(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 52.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawableIcon(icon, Modifier.size(20.dp), tint = App.colors.muted)
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
    }
}

/** Taak op [day] zetten en een melding met "Ongedaan maken" tonen. */
fun CoroutineScope.scheduleWithUndo(vm: AppViewModel, snackbar: SnackbarHostState, task: Task, day: LocalDate?, today: LocalDate) = launch {
    val before = vm.schedule(task.id, day) ?: return@launch
    val label = when {
        day == null -> "${task.title} staat niet meer ingepland"
        before.nextDue == null -> "${task.title} ingepland op ${day.dayLabel(today)}"
        !before.nextDue.isAfter(today) -> "${task.title} uitgesteld tot ${day.dayLabel(today)}"
        else -> "${task.title} verplaatst naar ${day.dayLabel(today)}"
    }
    val result = snackbar.showSnackbar(label, "Ongedaan maken", duration = SnackbarDuration.Short)
    if (result == SnackbarResult.ActionPerformed) vm.restore(before)
}

const val DAY_MS = 86_400_000L
