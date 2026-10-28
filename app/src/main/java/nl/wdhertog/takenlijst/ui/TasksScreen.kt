package nl.wdhertog.takenlijst.ui

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import nl.wdhertog.takenlijst.ui.theme.fg
import nl.wdhertog.takenlijst.ui.theme.bg
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.wdhertog.takenlijst.data.Task
import nl.wdhertog.takenlijst.data.TaskGroup
import nl.wdhertog.takenlijst.domain.RepeatUnit
import nl.wdhertog.takenlijst.ui.theme.App

private enum class Bucket(val label: String) {
    DAILY("Dagelijks"), WEEKLY("Wekelijks"), MONTHLY("Maandelijks"), RARE("Per kwartaal of minder"), ON_DEMAND("Op afroep")
}

private fun Task.bucket() = when (repeatUnit) {
    RepeatUnit.DAY -> Bucket.DAILY
    RepeatUnit.WEEK -> Bucket.WEEKLY
    RepeatUnit.MONTH -> if (repeatEvery < 3) Bucket.MONTHLY else Bucket.RARE
    RepeatUnit.YEAR -> Bucket.RARE
    RepeatUnit.NONE -> Bucket.ON_DEMAND
}

@Composable
fun TasksScreen(vm: AppViewModel, contentPadding: PaddingValues, onEdit: (Long) -> Unit) {
    val tasks by vm.tasks.collectAsState()
    val day by vm.currentDate.collectAsState()
    val groups = LocalGroups.current
    var groupId by rememberSaveable { mutableStateOf(groups.firstOrNull()?.id ?: TaskGroup.HOME) }
    // Groep weggehaald terwijl hij gekozen was: terug naar de eerste.
    val group = groups.firstOrNull { it.id == groupId } ?: groups.firstOrNull() ?: TaskGroup.FALLBACK
    val all = tasks.orEmpty()
    val counts = all.groupingBy { it.groupId }.eachCount()
    val buckets = all.filter { it.groupId == group.id }
        .groupBy { it.bucket() }
        .toSortedMap()
        .mapValues { (_, list) -> list.sortedWith(compareBy(nullsLast()) { it.nextDue }) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
    ) {
        item { Text("Taken", style = MaterialTheme.typography.headlineLarge) }
        item {
            val tabScroll = rememberScrollState()
            Row(
                Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    // Bij meer dan drie groepen scrollen de tabbladen; anders verdelen ze de breedte.
                    .then(if (groups.size > 3) Modifier.horizontalScroll(tabScroll) else Modifier)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                groups.forEach { g ->
                    val selected = g.id == group.id
                    Row(
                        Modifier
                            .then(if (groups.size <= 3) Modifier.weight(1f) else Modifier.widthIn(min = 96.dp))
                            .height(44.dp)
                            .then(if (selected) Modifier.shadow(1.dp, RoundedCornerShape(12.dp)) else Modifier)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) App.colors.card else MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { groupId = g.id }
                            .semantics { role = Role.Tab; this.selected = selected },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fg = if (selected) g.fg else MaterialTheme.colorScheme.onSurface
                        DrawableIcon(g.icon(), Modifier.padding(start = 10.dp).size(18.dp), tint = fg)
                        Text(" ${g.label} ", style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
                        Text("${counts[g.id] ?: 0}", style = MaterialTheme.typography.labelLarge, color = App.colors.muted, modifier = Modifier.padding(end = 10.dp))
                    }
                }
            }
        }

        if (buckets.isEmpty()) {
            item {
                Text(
                    "Nog geen taken in ${group.label.lowercase()}. Tik op + om er een te maken.",
                    style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
                    modifier = Modifier.padding(top = 24.dp, start = 4.dp),
                )
            }
        }

        buckets.forEach { (bucket, list) ->
            item(key = "h${bucket.name}") { SectionHeader(bucket.label, trailing = list.size.toString()) }
            item(key = "c${bucket.name}") {
                Column(Modifier.clip(RoundedCornerShape(18.dp)).background(App.colors.card)) {
                    list.forEachIndexed { i, t ->
                        if (i > 0) HorizontalDivider(color = App.colors.line)
                        TaskListItem(t, group, day, onClick = { onEdit(t.id) }, onSchedule = { vm.schedule(t.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskListItem(t: Task, group: TaskGroup, day: java.time.LocalDate, onClick: () -> Unit, onSchedule: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(group.bg),
            contentAlignment = Alignment.Center,
        ) { DrawableIcon(t.iconRes(group), Modifier.size(20.dp), tint = group.fg) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = buildString {
                append(t.describeRepeat())
                if (t.timesPerDay > 1) t.times.takeIf { it.isNotEmpty() }?.let { append(" · ").append(it.joinToString(", ") { m -> minutesLabel(m) }) }
                else t.reminderMinutes?.let { append(" · ").append(minutesLabel(it)) }
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = App.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val due = t.nextDue
        when {
            due == null -> OutlinedButton(onClick = onSchedule, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Vandaag") }
            due.isBefore(day) -> Text(lateLabel(due, day), style = MaterialTheme.typography.labelLarge, color = App.colors.lateFg)
            else -> Text(due.relativeLabel(day), style = MaterialTheme.typography.labelLarge)
        }
    }
}
