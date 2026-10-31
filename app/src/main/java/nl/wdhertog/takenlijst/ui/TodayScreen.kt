package nl.wdhertog.takenlijst.ui

import nl.wdhertog.takenlijst.ui.theme.fg
import nl.wdhertog.takenlijst.ui.theme.bg
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.data.Task
import nl.wdhertog.takenlijst.data.TaskGroup
import nl.wdhertog.takenlijst.ui.theme.App

@Composable
fun TodayScreen(
    vm: AppViewModel,
    snackbar: SnackbarHostState,
    contentPadding: PaddingValues,
    onEdit: (Long) -> Unit,
    onShopping: () -> Unit,
) {
    val state by vm.todayState.collectAsState()
    val day by vm.currentDate.collectAsState()
    val filter by vm.groupFilter.collectAsState()
    val openShopping by vm.openShopping.collectAsState()
    val groups = LocalGroups.current
    val scope = rememberCoroutineScope()
    var postpone by remember { mutableStateOf<Task?>(null) }

    fun complete(task: Task) = scope.launch {
        val id = vm.complete(task.id) ?: return@launch
        val label = if (task.timesPerDay > 1 && task.slotsDone + 1 < task.timesPerDay) "${task.title}: ${task.slotsDone + 1} van ${task.timesPerDay}" else "${task.title} afgevinkt"
        val result = snackbar.showSnackbar(label, "Ongedaan maken", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) vm.undo(id)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(day.longLabel(), style = MaterialTheme.typography.labelLarge, color = App.colors.muted)
            Text("Vandaag", style = MaterialTheme.typography.headlineLarge)
        }

        if (state.loaded && !state.hasTasks) {
            item { EmptyStart(onExamples = vm::addExamples, onNew = { onEdit(0) }) }
            return@LazyColumn
        }

        item { ProgressCard(state) }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                item {
                    FilterChip(selected = filter == null, onClick = { vm.groupFilter.value = null }, label = { Text("Alles") }, colors = chipColors())
                }
                items(groups, key = { it.id }) { g ->
                    FilterChip(
                        selected = filter == g.id,
                        onClick = { vm.groupFilter.value = if (filter == g.id) null else g.id },
                        label = { Text(g.label, color = if (filter == g.id) MaterialTheme.colorScheme.surface else g.fg) },
                        colors = chipColors(),
                    )
                }
            }
        }

        if (state.late.isNotEmpty()) {
            item { SectionHeader("Te laat", color = App.colors.lateFg) }
            items(state.late, key = { "late${it.id}" }) { t ->
                TaskRow(
                    t.title, groupOf(t.groupId), t.describeRepeat(), checked = false,
                    onCheck = { complete(t) }, onClick = { onEdit(t.id) }, onLongClick = { postpone = t },
                    late = lateLabel(t.nextDue!!, day), icon = t.iconRes(groupOf(t.groupId)),
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item { SectionHeader("Vandaag") }
        if (state.due.isEmpty() && state.done.isEmpty()) {
            item {
                Text(
                    "Niets gepland voor vandaag.",
                    style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        items(state.done, key = { "done${it.completionId}" }) { d ->
            TaskRow(
                d.title, groupOf(d.groupId), d.repeat.describe(), checked = true,
                onCheck = { vm.undo(d.completionId) }, onClick = { onEdit(d.taskId) },
                icon = taskIconRes(d.icon, d.title, groupOf(d.groupId)),
                modifier = Modifier.animateItem(),
            )
        }
        items(state.due, key = { "due${it.id}" }) { t ->
            TaskRow(
                t.title, groupOf(t.groupId), detail(t), checked = false,
                onCheck = { complete(t) }, onClick = { onEdit(t.id) }, onLongClick = { postpone = t }, icon = t.iconRes(groupOf(t.groupId)),
                modifier = Modifier.animateItem(),
                progress = if (t.timesPerDay > 1) t.slotsDone / t.timesPerDay.toFloat() else 0f,
                trailing = { if (t.linkShopping) ShoppingBadge(openShopping, onShopping) },
            )
        }

        if (state.upcoming.isNotEmpty()) {
            item { SectionHeader("Binnenkort") }
            items(state.upcoming, key = { "next${it.id}" }) { t ->
                TaskRow(
                    t.title, groupOf(t.groupId), "${t.describeRepeat()} · ${t.nextDue!!.relativeLabel(day)}", checked = false,
                    onCheck = { complete(t) }, onClick = { onEdit(t.id) }, onLongClick = { postpone = t }, icon = t.iconRes(groupOf(t.groupId)),
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    postpone?.let { t ->
        ScheduleSheet(t, day, onDismiss = { postpone = null }, onPick = { picked ->
            postpone = null
            scope.scheduleWithUndo(vm, snackbar, t, picked, day)
        })
    }
}

private fun detail(t: Task) = buildString {
    if (t.timesPerDay > 1) {
        append("${t.slotsDone} van ${t.timesPerDay} gedaan")
        t.nextSlotTime?.let { append(" · volgende ").append(minutesLabel(it)) }
    } else {
        append(t.repeat.describe())
        t.reminderMinutes?.let { append(" · ").append(minutesLabel(it)) }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = App.colors.card,
    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
    selectedLabelColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun ProgressCard(state: TodayState) {
    val colors = App.colors
    val progress by animateFloatAsState(state.progress, label = "voortgang")
    val open = state.late.size + state.due.size
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.hero)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(56.dp).semantics { contentDescription = "${state.done.size} van ${state.total} gedaan" }) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(colors.heroTrack, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(colors.onHero, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                if (state.total == 0) "Vrije dag" else "${state.done.size} van ${state.total} gedaan",
                style = MaterialTheme.typography.titleLarge, color = colors.onHero,
            )
            val sub = when {
                state.total == 0 -> "Er staat vandaag niets op de planning"
                open == 0 -> "Alles gedaan, lekker bezig!"
                state.late.isEmpty() -> "Nog $open ${if (open == 1) "taak" else "taken"}"
                else -> "Nog $open ${if (open == 1) "taak" else "taken"}, waarvan ${state.late.size} te laat"
            }
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = colors.onHero.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun ShoppingBadge(count: Int, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(if (count == 0) "Lijst" else "$count", fontWeight = FontWeight.Bold) },
        leadingIcon = { DrawableIcon(R.drawable.ic_cart, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
    )
}

@Composable
private fun EmptyStart(onExamples: () -> Unit, onNew: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(App.colors.card)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { DrawableIcon(R.drawable.ic_home, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Text("Welkom!", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Zet hier je terugkerende klusjes in: wekelijks, per maand, per kwartaal of gewoon op afroep. " +
                "Begin met een paar voorbeelden die je zelf kunt aanpassen, of maak meteen je eigen taak.",
            style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onExamples, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Voorbeeldtaken toevoegen") }
        TextButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("Eigen taak maken") }
    }
}
