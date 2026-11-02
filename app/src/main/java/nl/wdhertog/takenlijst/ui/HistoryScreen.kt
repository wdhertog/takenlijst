package nl.wdhertog.takenlijst.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.wdhertog.takenlijst.data.HistoryEntry
import nl.wdhertog.takenlijst.ui.theme.App
import nl.wdhertog.takenlijst.ui.theme.bg
import nl.wdhertog.takenlijst.ui.theme.fg

/** Eén taak op één dag, met hoe vaak hij die dag gedaan is. */
private data class DoneLine(val entry: HistoryEntry, val times: Int)

/** Alles wat afgevinkt is, per dag, nieuwste eerst. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: AppViewModel, onEdit: (Long) -> Unit, onBack: () -> Unit) {
    val history by vm.history.collectAsState()
    val today by vm.currentDate.collectAsState()
    val groups = LocalGroups.current
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    val days = remember(history, filter) {
        history.orEmpty()
            .filter { filter == null || it.groupId == filter }
            .groupBy { it.doneOn }
            .map { (day, entries) -> day to entries.groupBy { it.taskId }.values.map { DoneLine(it.first(), it.size) } }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Gedaan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                val chipColors = FilterChipDefaults.filterChipColors(
                    containerColor = App.colors.card,
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = MaterialTheme.colorScheme.surface,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Alles") }, colors = chipColors) }
                    items(groups, key = { it.id }) { g ->
                        FilterChip(
                            selected = filter == g.id,
                            onClick = { filter = if (filter == g.id) null else g.id },
                            label = { Text(g.label, color = if (filter == g.id) MaterialTheme.colorScheme.surface else g.fg) },
                            colors = chipColors,
                        )
                    }
                }
            }

            if (history != null && days.isEmpty()) {
                item {
                    Text(
                        "Hier komt alles wat je afvinkt, per dag.",
                        style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
                        modifier = Modifier.padding(top = 24.dp, start = 4.dp),
                    )
                }
            }

            days.forEach { (day, lines) ->
                item(key = "h$day") { SectionHeader(day.dateLabel(today), trailing = lines.sumOf { it.times }.toString()) }
                item(key = "d$day") {
                    Column(Modifier.clip(RoundedCornerShape(18.dp)).background(App.colors.card)) {
                        lines.forEachIndexed { i, line ->
                            if (i > 0) HorizontalDivider(color = App.colors.line)
                            DoneRow(line, onClick = { onEdit(line.entry.taskId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneRow(line: DoneLine, onClick: () -> Unit) {
    val e = line.entry
    val group = groupOf(e.groupId)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(group.bg),
            contentAlignment = Alignment.Center,
        ) { DrawableIcon(taskIconRes(e.icon, e.title, group), Modifier.size(20.dp), tint = group.fg) }
        Text(
            e.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        if (line.times > 1) Text("${line.times}×", style = MaterialTheme.typography.labelLarge, color = App.colors.muted, modifier = Modifier.padding(end = 8.dp))
        GroupTag(group)
    }
}
